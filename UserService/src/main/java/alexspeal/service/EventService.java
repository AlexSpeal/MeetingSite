package alexspeal.service;

import alexspeal.dto.BusyIntervalDto;
import alexspeal.dto.EventDto;
import alexspeal.dto.requests.AcceptMeetingRequest;
import alexspeal.dto.requests.CreatingMeetingRequest;
import alexspeal.dto.responses.AvailabilityResponse;
import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.entities.UserEntity;
import alexspeal.enums.AcceptStatus;
import alexspeal.enums.ErrorMessage;
import alexspeal.models.Interval;
import alexspeal.models.Slot;
import alexspeal.repositories.EventParticipantRepository;
import alexspeal.repositories.EventRepository;
import alexspeal.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipantRepository eventParticipantRepository;

    public EventDto getEventById(Long id) {
        return eventRepository.findById(id)
                .map(eventEntity -> new EventDto(
                        eventEntity.getId(),
                        eventEntity.getTitle(),
                        eventEntity.getDescription(),
                        eventEntity.getStatus(),
                        eventEntity.getStartTime(),
                        eventEntity.getDuration(),
                        eventEntity.getPossibleDays(),
                        eventEntity.getAuthor().getId()
                ))
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));
    }

    public void scheduleEvent(Long eventId, LocalDateTime startTime) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));

        LocalDate scheduledDate = startTime.toLocalDate();

        if (event.getPossibleDays() == null || !event.getPossibleDays().contains(scheduledDate)) {
            throw new IllegalArgumentException(ErrorMessage.DATE_IS_NOT_INCLUDED.getMessage(startTime.toLocalDate()));
        }

        eventRepository.updateEventStartTime(eventId, startTime);
    }

    public Long createMeeting(UserEntity author, CreatingMeetingRequest meeting) {

        validateDates(meeting.possibleDays(), null);

        EventEntity eventEntity = new EventEntity(
                meeting.title(),
                meeting.description(),
                AcceptStatus.PENDING,
                null,
                meeting.duration(),
                meeting.possibleDays(),
                author
        );
        eventEntity = eventRepository.save(eventEntity);
        addParticipantsToEvent(eventEntity, meeting.participants());
        return eventEntity.getId();
    }

    public void acceptMeeting(UserEntity user, AcceptMeetingRequest acceptMeetingRequest, Long meetingId) {
        EventEntity event = eventRepository.findById(meetingId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));

        EventParticipantEntity participant = eventParticipantRepository.findByUserIdAndEvent(user.getId(), event)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USER_IS_NOT_A_PARTICIPANT.getMessage()));

        if (acceptMeetingRequest.acceptStatus() == AcceptStatus.ACCEPTED) {
            validateDates(acceptMeetingRequest.selectedDays(), event);
            participant.setStatus(AcceptStatus.ACCEPTED);
            participant.setSelectedDays(acceptMeetingRequest.selectedDays());
        } else if (acceptMeetingRequest.acceptStatus() == AcceptStatus.DECLINED) {
            participant.setStatus(AcceptStatus.DECLINED);
            participant.setSelectedDays(null);
        } else {
            throw new IllegalArgumentException(ErrorMessage.INCORRECT_STATUS.getMessage());
        }
        eventParticipantRepository.save(participant);
    }

    private void validateDates(List<LocalDate> dates, EventEntity event) {
        LocalDate today = LocalDate.now();
        List<LocalDate> possibleDays = event != null ? event.getPossibleDays() : null;

        for (LocalDate date : dates) {
            if (date.isBefore(today)) {
                throw new IllegalArgumentException(ErrorMessage.DATE_ALREADY_PASSED.getMessage(date));
            }
            if (event != null && !possibleDays.contains(date)) {
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
                .map(user -> new EventParticipantEntity(eventEntity, user, AcceptStatus.PENDING))
                .toList();
        eventParticipantRepository.saveAll(participants);
    }

    public AvailabilityResponse getMeetingAvailability(Long meetingId) {
        EventEntity meeting = eventRepository.findById(meetingId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));
        List<LocalDate> possibleDays = meeting.getPossibleDays();

        List<EventParticipantEntity> acceptedParticipants =
                eventParticipantRepository.findByEventIdAndStatus(meetingId, AcceptStatus.ACCEPTED);


        Set<LocalDate> availableDays = new HashSet<>(possibleDays);
        for (EventParticipantEntity participant : acceptedParticipants) {
            availableDays.retainAll(participant.getSelectedDays());
        }

        LocalTime workStart = LocalTime.of(9, 0);
        LocalTime workEnd = LocalTime.of(18, 0);
        int slotDuration = meeting.getDuration();

        List<Slot> possibleSlots = new ArrayList<>();
        for (LocalDate day : availableDays) {

            List<Interval> busyIntervals = new ArrayList<>();
            for (EventParticipantEntity participant : acceptedParticipants) {

                List<BusyIntervalDto> busyDtos = eventRepository.getBusyIntervals(participant.getUser().getId(), day);

                for (BusyIntervalDto dto : busyDtos) {
                    LocalTime start = dto.startTime();
                    LocalTime end = start.plusMinutes(dto.duration());
                    busyIntervals.add(new Interval(start, end));
                }
            }
            busyIntervals.sort(Comparator.comparing(Interval::start));


            List<Interval> freeIntervals = excludeBusyTimes(workStart, workEnd, busyIntervals);

            for (Interval freeInterval : freeIntervals) {
                List<Slot> slots = generateSlots(day, freeInterval, slotDuration);
                possibleSlots.addAll(slots);
            }
        }

        return new AvailabilityResponse(meetingId, possibleSlots);
    }


    private List<Interval> excludeBusyTimes(LocalTime workStart, LocalTime workEnd, List<Interval> busyIntervals) {
        List<Interval> free = new ArrayList<>();
        free.add(new Interval(workStart, workEnd));

        for (Interval busy : busyIntervals) {
            List<Interval> newFree = new ArrayList<>();
            for (Interval f : free) {
                if (busy.end().isBefore(f.start()) || busy.start().isAfter(f.end())) {
                    newFree.add(f);
                } else {
                    if (f.start().isBefore(busy.start())) {
                        newFree.add(new Interval(f.start(), busy.start()));
                    }
                    if (busy.end().isBefore(f.end())) {
                        newFree.add(new Interval(busy.end(), f.end()));
                    }
                }
            }
            free = newFree;
        }
        return free;
    }

    private List<Slot> generateSlots(LocalDate day, Interval freeInterval, int slotDuration) {
        List<Slot> slots = new ArrayList<>();
        LocalTime start = freeInterval.start();
        LocalTime end = freeInterval.end();
        while (!start.plusMinutes(slotDuration).isAfter(end)) {
            slots.add(new Slot(day, start, slotDuration));
            start = start.plusMinutes(slotDuration);
        }
        return slots;
    }
}