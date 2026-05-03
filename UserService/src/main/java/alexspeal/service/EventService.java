package alexspeal.service;

import alexspeal.dto.BusyIntervalDto;
import alexspeal.dto.EventDto;
import alexspeal.dto.requests.AcceptMeetingRequest;
import alexspeal.dto.requests.CreatingMeetingRequest;
import alexspeal.entities.DayEntity;
import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.entities.UserEntity;
import alexspeal.enums.AcceptStatusEvent;
import alexspeal.enums.AcceptStatusParticipant;
import alexspeal.enums.ErrorMessage;
import alexspeal.enums.SortOption;
import alexspeal.mappers.MeetingMapper;
import alexspeal.models.Participant;
import alexspeal.repositories.DayRepository;
import alexspeal.repositories.MeetingParticipantRepository;
import alexspeal.repositories.MeetingRepository;
import alexspeal.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final DayRepository dayRepository;
    private final MeetingMapper meetingMapper;
    private final PersonalEventOptimizer personalEventOptimizer;


    public EventDto getEventById(Long id) {
        return meetingRepository.findById(id)
                .map(meetingMapper::toEventDto)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));
    }

    @Transactional(readOnly = true)
    public List<EventDto> getAllUserEvents(Long id, SortOption sortOption) {
        List<EventEntity> eventList = meetingRepository.getAllUserEvents(id);
        if (eventList == null || eventList.isEmpty()) {
            return List.of();
        }
        List<EventDto> eventDtoList = eventList.stream()
                .map(meetingMapper::toEventDto)
                .collect(Collectors.toCollection(ArrayList::new));

        Comparator<EventDto> comparator = switch (sortOption) {
            case DATE -> Comparator
                    .comparing(EventDto::startTime, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(EventDto::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case TITLE -> Comparator
                    .comparing(EventDto::title, Comparator.nullsLast(Comparator.naturalOrder()));
            case STATUS -> Comparator
                    .comparing(EventDto::status, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        eventDtoList.sort(comparator);
        return eventDtoList;
    }

    @Transactional
    public EventDto scheduleEvent(Long eventId, OffsetDateTime startTime) {
        EventEntity event = meetingRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));

        int duration = event.getDuration();

        EventParticipantEntity authorParticipant = meetingParticipantRepository
                .findByEventIdAndUserId(eventId, event.getAuthor().getId())
                .orElseThrow(() -> new IllegalStateException(ErrorMessage.NOT_FOUND_AUTHOR.getMessage()));

        String authorTz = event.getAuthor().getTimezone();
        ZoneId authorZone = (authorTz != null && !authorTz.isBlank())
                ? ZoneId.of(authorTz) : ZoneOffset.UTC;
        LocalDate scheduledDate = startTime.atZoneSameInstant(authorZone).toLocalDate();

        List<LocalDate> possibleDays = authorParticipant.getDays()
                .stream()
                .map(DayEntity::getDate)
                .toList();

        if (possibleDays.isEmpty() || !possibleDays.contains(scheduledDate)) {
            throw new IllegalArgumentException(ErrorMessage.DATE_IS_NOT_INCLUDED.getMessage(scheduledDate));
        }

        OffsetDateTime startUtc = startTime.withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime meetingEnd = startUtc.plusMinutes(duration);

        if (Boolean.TRUE.equals(event.getIsPersonal())) {
            Map<EventEntity, OffsetDateTime> relocations = personalEventOptimizer
                    .planRelocations(event.getAuthor(), eventId, startUtc, duration);

            for (Map.Entry<EventEntity, OffsetDateTime> entry : relocations.entrySet()) {
                EventEntity moved = entry.getKey();
                OffsetDateTime newStart = entry.getValue();
                meetingRepository.updateEventStartTime(moved.getId(), newStart);
                moved.setStartTime(newStart);
            }
        }

        OffsetDateTime fetchFrom = startUtc.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fetchTo = fetchFrom.plusDays(2);

        List<EventParticipantEntity> participants = meetingParticipantRepository
                .findByEventId(eventId)
                .stream()
                .filter(p -> !p.getUser().getId().equals(event.getAuthor().getId()))
                .toList();

        for (EventParticipantEntity participant : participants) {
            Long userId = participant.getUser().getId();

            String participantTz = participant.getUser().getTimezone();
            ZoneId participantZone = (participantTz != null && !participantTz.isBlank())
                    ? ZoneId.of(participantTz) : ZoneOffset.UTC;
            LocalDate participantLocalDate = startUtc.atZoneSameInstant(participantZone).toLocalDate();

            boolean selectedDay = participant.getDays().stream()
                    .anyMatch(day -> day.getDate().equals(participantLocalDate));

            if (!selectedDay) {
                participant.setStatus(AcceptStatusParticipant.INABILITY);
                continue;
            }

            List<BusyIntervalDto> busyIntervals = meetingRepository
                    .getBusyIntervals(userId, fetchFrom, fetchTo);

            boolean overlaps = busyIntervals.stream().anyMatch(interval -> {
                OffsetDateTime busyStart = interval.startTime().withOffsetSameInstant(ZoneOffset.UTC);
                OffsetDateTime busyEnd = busyStart.plusMinutes(interval.duration());
                return !meetingEnd.isBefore(busyStart) && !startUtc.isAfter(busyEnd);
            });

            if (overlaps) {
                participant.setStatus(AcceptStatusParticipant.INABILITY);
            }
        }

        meetingParticipantRepository.saveAll(participants);
        meetingRepository.updateEventStartTimeAndStatus(eventId, startUtc, AcceptStatusEvent.ACCEPTED);

        event.setStartTime(startUtc);
        event.setStatus(AcceptStatusEvent.ACCEPTED);

        return meetingMapper.toEventDto(event);
    }


    @Transactional
    public EventDto createEvent(UserEntity author, CreatingMeetingRequest meeting) {
        validateDates(meeting.possibleDays());

        boolean isPersonal = meeting.participants().isEmpty();
        boolean isFixed = !isPersonal || meeting.isFixed() == null || meeting.isFixed();

        LocalTime preferredStart = meeting.preferredWindowStart();
        LocalTime preferredEnd = meeting.preferredWindowEnd();
        validatePreferredWindow(preferredStart, preferredEnd, meeting.duration(), isPersonal);

        EventEntity eventEntity = new EventEntity(
                meeting.title(),
                meeting.description(),
                AcceptStatusEvent.PENDING,
                null,
                meeting.duration(),
                author,
                isPersonal,
                isFixed,
                preferredStart,
                preferredEnd
        );
        eventEntity = meetingRepository.save(eventEntity);

        EventParticipantEntity authorParticipant = meetingParticipantRepository.save(
                new EventParticipantEntity(
                        eventEntity,
                        author,
                        AcceptStatusParticipant.ACCEPTED,
                        true
                )
        );

        List<DayEntity> possibleDayEntities = createDayEntities(authorParticipant, meeting.possibleDays());
        dayRepository.saveAll(possibleDayEntities);

        if (!meeting.participants().isEmpty()) {
            addParticipantsToEvent(eventEntity, meeting.participants());
        }

        return meetingMapper.toEventDto(eventEntity);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        meetingRepository.deleteById(eventId);
    }

    @Transactional
    public void acceptEvent(UserEntity user, AcceptMeetingRequest acceptMeetingRequest, Long meetingId) {
        EventEntity event = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));

        EventParticipantEntity participant = meetingParticipantRepository
                .findByUserIdAndEvent(user.getId(), event)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USER_IS_NOT_A_PARTICIPANT.getMessage()));

        EventParticipantEntity authorParticipant = meetingParticipantRepository
                .findByEventIdAndUserId(meetingId, event.getAuthor().getId())
                .orElseThrow(() -> new IllegalStateException(ErrorMessage.NOT_FOUND_AUTHOR.getMessage()));


        if (acceptMeetingRequest.status() == AcceptStatusParticipant.ACCEPTED) {
            List<LocalDate> localDates = authorParticipant.getDays()
                    .stream()
                    .map(DayEntity::getDate)
                    .toList();

            validateDates(acceptMeetingRequest.selectedDays(), localDates);
            participant.setStatus(AcceptStatusParticipant.ACCEPTED);

            dayRepository.deleteByEventParticipantId(participant.getId());

            List<DayEntity> selectedDayEntities = createDayEntities(participant, acceptMeetingRequest.selectedDays());
            meetingParticipantRepository.save(participant);
            dayRepository.saveAll(selectedDayEntities);
        } else if (acceptMeetingRequest.status() == AcceptStatusParticipant.DECLINED) {
            participant.setStatus(AcceptStatusParticipant.DECLINED);
            dayRepository.deleteByEventParticipantId(participant.getId());
        } else {
            throw new IllegalArgumentException(ErrorMessage.INCORRECT_STATUS.getMessage());
        }

    }

    private void validatePreferredWindow(LocalTime start, LocalTime end, int durationMinutes, boolean isPersonal) {
        boolean startSet = start != null;
        boolean endSet = end != null;

        if (!startSet && !endSet) {
            return;
        }

        if (startSet ^ endSet) {
            throw new IllegalArgumentException(ErrorMessage.PREFERRED_WINDOW_INCOMPLETE.getMessage());
        }

        if (!isPersonal) {
            throw new IllegalArgumentException(ErrorMessage.PREFERRED_WINDOW_NON_PERSONAL.getMessage());
        }

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException(ErrorMessage.PREFERRED_WINDOW_INVALID_ORDER.getMessage());
        }

        long windowMinutes = Duration.between(start, end).toMinutes();
        if (windowMinutes < durationMinutes) {
            throw new IllegalArgumentException(
                    ErrorMessage.PREFERRED_WINDOW_TOO_SHORT.getMessage(windowMinutes, durationMinutes));
        }
    }

    private void validateDates(List<LocalDate> dates) {
        LocalDate today = LocalDate.now();
        for (LocalDate date : dates) {
            if (date.isBefore(today)) {
                throw new IllegalArgumentException(ErrorMessage.DATE_ALREADY_PASSED.getMessage(date));
            }
        }
    }

    private void validateDates(List<LocalDate> dates, List<LocalDate> possibleDays) {
        LocalDate today = LocalDate.now();
        for (LocalDate date : dates) {
            if (date.isBefore(today)) {
                throw new IllegalArgumentException(ErrorMessage.DATE_ALREADY_PASSED.getMessage(date));
            }
            if (!possibleDays.contains(date)) {
                throw new IllegalArgumentException(ErrorMessage.DATE_IS_NOT_INCLUDED.getMessage(date));
            }
        }
    }

    private UserEntity findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId.toString())));
    }

    private void addParticipantsToEvent(EventEntity eventEntity, List<Participant> participants) {
        List<EventParticipantEntity> eventParticipants = participants.stream()
                .filter(participant -> !participant.userId().equals(eventEntity.getAuthor().getId()))
                .map(participant -> {
                    UserEntity user = findUserById(participant.userId());
                    return new EventParticipantEntity(eventEntity, user,
                            AcceptStatusParticipant.PENDING,
                            participant.required());
                })
                .toList();

        meetingParticipantRepository.saveAll(eventParticipants);
        eventEntity.setEventParticipants(eventParticipants);
    }

    private List<DayEntity> createDayEntities(EventParticipantEntity participant, List<LocalDate> dates) {
        return dates.stream()
                .map(date -> {
                    DayEntity day = new DayEntity();
                    day.setDate(date);
                    day.setEventParticipant(participant);
                    return day;
                })
                .toList();
    }
}