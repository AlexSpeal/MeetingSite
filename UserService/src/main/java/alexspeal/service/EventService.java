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
import alexspeal.mappers.EventMapper;
import alexspeal.repositories.DayRepository;
import alexspeal.repositories.EventParticipantRepository;
import alexspeal.repositories.EventRepository;
import alexspeal.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final DayRepository dayRepository;
    private final EventMapper eventMapper;


    public EventDto getEventById(Long id) {
        return eventRepository.findById(id)
                .map(eventMapper::toEventDto)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));
    }

    @Transactional(readOnly = true)
    public List<EventDto> getAllUserEvents(Long id, SortOption sortOption) {
        List<EventEntity> eventList = eventRepository.getAllUserEvents(id);
        if (eventList == null || eventList.isEmpty()) {
            return List.of();
        }
        List<EventDto> eventDtoList = eventList.stream()
                .map(eventMapper::toEventDto)
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
    public EventDto scheduleEvent(Long eventId, LocalDateTime startTime) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));

        int duration = event.getDuration();
        LocalDate scheduledDate = startTime.toLocalDate();

        EventParticipantEntity authorParticipant = eventParticipantRepository
                .findByEventIdAndUserId(eventId, event.getAuthor().getId())
                .orElseThrow(() -> new IllegalStateException(ErrorMessage.NOT_FOUND_AUTHOR.getMessage()));

        List<LocalDate> possibleDays = authorParticipant.getDays()
                .stream()
                .map(DayEntity::getDate)
                .toList();

        if (possibleDays.isEmpty() || !possibleDays.contains(scheduledDate)) {
            throw new IllegalArgumentException(ErrorMessage.DATE_IS_NOT_INCLUDED.getMessage(scheduledDate));
        }

        List<EventParticipantEntity> participants = eventParticipantRepository
                .findByEventId(eventId)
                .stream()
                .filter(p -> !p.getUser().getId().equals(event.getAuthor().getId()))
                .toList();

        for (EventParticipantEntity participant : participants) {
            Long userId = participant.getUser().getId();

            boolean selectedDay = participant.getDays().stream()
                    .anyMatch(day -> day.getDate().equals(scheduledDate));

            if (!selectedDay) {
                participant.setStatus(AcceptStatusParticipant.INABILITY);
                continue;
            }

            List<BusyIntervalDto> busyIntervals = eventRepository
                    .getBusyIntervals(userId, List.of(scheduledDate));

            boolean overlaps = busyIntervals.stream().anyMatch(interval -> {
                LocalDateTime busyStart = interval.startTime();
                LocalDateTime busyEnd = busyStart.plusMinutes(interval.duration());
                LocalDateTime meetingEnd = startTime.plusMinutes(duration);
                return !meetingEnd.isBefore(busyStart) && !startTime.isAfter(busyEnd);
            });

            if (overlaps) {
                participant.setStatus(AcceptStatusParticipant.INABILITY);
            }
        }

        eventParticipantRepository.saveAll(participants);
        eventRepository.updateEventStartTimeAndStatus(eventId, startTime, AcceptStatusEvent.ACCEPTED);

        event.setStartTime(startTime);
        event.setStatus(AcceptStatusEvent.ACCEPTED);

        return eventMapper.toEventDto(event);
    }


    @Transactional
    public EventDto createMeeting(UserEntity author, CreatingMeetingRequest meeting) {
        validateDates(meeting.possibleDays());

        EventEntity eventEntity = new EventEntity(
                meeting.title(),
                meeting.description(),
                AcceptStatusEvent.PENDING,
                null,
                meeting.duration(),
                author,
                meeting.participants().isEmpty()
        );
        eventEntity = eventRepository.save(eventEntity);

        EventParticipantEntity authorParticipant = eventParticipantRepository.save(
                new EventParticipantEntity(
                        eventEntity,
                        author,
                        AcceptStatusParticipant.ACCEPTED
                )
        );

        List<DayEntity> possibleDayEntities = createDayEntities(authorParticipant, meeting.possibleDays());
        dayRepository.saveAll(possibleDayEntities);

        if (!meeting.participants().isEmpty()) {
            addParticipantsToEvent(eventEntity, meeting.participants());
        }

        return eventMapper.toEventDto(eventEntity);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        eventRepository.deleteById(eventId);
    }

    @Transactional
    public void acceptEvent(UserEntity user, AcceptMeetingRequest acceptMeetingRequest, Long meetingId) {
        EventEntity event = eventRepository.findById(meetingId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));

        EventParticipantEntity participant = eventParticipantRepository
                .findByUserIdAndEvent(user.getId(), event)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USER_IS_NOT_A_PARTICIPANT.getMessage()));

        EventParticipantEntity authorParticipant = eventParticipantRepository
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
            eventParticipantRepository.save(participant);
            dayRepository.saveAll(selectedDayEntities);
        } else if (acceptMeetingRequest.status() == AcceptStatusParticipant.DECLINED) {
            participant.setStatus(AcceptStatusParticipant.DECLINED);
            dayRepository.deleteByEventParticipantId(participant.getId());
            eventParticipantRepository.deleteById(participant.getId());
        } else {
            throw new IllegalArgumentException(ErrorMessage.INCORRECT_STATUS.getMessage());
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

    private void addParticipantsToEvent(EventEntity eventEntity, List<Long> participantIds) {
        List<EventParticipantEntity> participants = participantIds.stream()
                .map(this::findUserById)
                .filter(user -> !user.getId().equals(eventEntity.getAuthor().getId()))
                .map(user -> new EventParticipantEntity(eventEntity, user, AcceptStatusParticipant.PENDING))
                .toList();
        eventParticipantRepository.saveAll(participants);
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