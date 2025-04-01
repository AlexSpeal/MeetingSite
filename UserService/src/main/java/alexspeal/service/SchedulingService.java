package alexspeal.service;

import alexspeal.dto.BusyIntervalDto;
import alexspeal.dto.responses.AvailabilityResponse;
import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.enums.AcceptStatus;
import alexspeal.enums.EventType;
import alexspeal.models.ParticipantSchedule;
import alexspeal.models.TimeEvent;
import alexspeal.models.TimeInterval;
import alexspeal.repositories.EventParticipantRepository;
import alexspeal.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SchedulingService {
    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;

    public AvailabilityResponse getMeetingAvailability(Long meetingId) {
        EventEntity meeting = eventRepository.findById(meetingId)
                .orElseThrow(() -> new NoSuchElementException("Meeting not found"));

        List<EventParticipantEntity> participants = eventParticipantRepository
                .findByEventIdAndStatus(meetingId, AcceptStatus.ACCEPTED);

        List<ParticipantSchedule> schedules = participants.stream()
                .map(this::createParticipantSchedule)
                .toList();

        Map<LocalDateTime, Integer> slotAvailability = calculateAvailability(
                meeting.getPossibleDays(),
                schedules,
                meeting.getDuration()
        );

        return buildResponse(meetingId, slotAvailability);
    }


    private ParticipantSchedule createParticipantSchedule(EventParticipantEntity participant) {
        Map<LocalDate, List<TimeInterval>> availability = new HashMap<>();

        eventRepository.getBusyIntervals(participant.getUser().getId(), participant.getSelectedDays())
                .forEach(dto -> processBusyInterval(dto, availability));

        participant.getSelectedDays().forEach(day ->
                availability.putIfAbsent(day, List.of(new TimeInterval(WORK_START, WORK_END)))
        );

        return new ParticipantSchedule(participant.getSelectedDays(), availability);
    }

    private void processBusyInterval(BusyIntervalDto dto, Map<LocalDate, List<TimeInterval>> availability) {
        LocalDateTime start = dto.startTime();
        LocalDateTime end = start.plusMinutes(dto.duration());

        start.toLocalDate().datesUntil(end.toLocalDate().plusDays(1)).forEach(day -> {
            LocalTime dayStart = day.equals(start.toLocalDate()) ? start.toLocalTime() : WORK_START;
            LocalTime dayEnd = day.equals(end.toLocalDate()) ? end.toLocalTime() : WORK_END;

            availability.compute(day, (k, v) -> {
                List<TimeInterval> intervals = v != null ? v : new ArrayList<>();
                intervals.add(new TimeInterval(
                        clampTime(dayStart),
                        clampTime(dayEnd)
                ));
                return intervals;
            });
        });
    }

    private LocalTime clampTime(LocalTime time) {
        if (time.isBefore(WORK_START)) return WORK_START;
        if (time.isAfter(WORK_END)) return WORK_END;
        return time;
    }

    private Map<LocalDateTime, Integer> calculateAvailability(
            List<LocalDate> days,
            List<ParticipantSchedule> schedules,
            int slotDuration) {

        Map<LocalDateTime, Integer> result = new HashMap<>();

        days.forEach(day -> {
            List<TimeEvent> events = collectDayEvents(day, schedules);
            processDayEvents(day, events, schedules.size(), slotDuration, result);
        });

        return result;
    }

    private List<TimeEvent> collectDayEvents(LocalDate day, List<ParticipantSchedule> schedules) {
        return schedules.stream()
                .filter(s -> s.selectedDays().contains(day))
                .flatMap(s -> s.availability().getOrDefault(day, List.of()).stream()
                        .flatMap(i -> Stream.of(
                                new TimeEvent(i.start(), EventType.START),
                                new TimeEvent(i.end(), EventType.END)
                        )))
                .sorted(Comparator.comparing(TimeEvent::time))
                .toList();
    }

    private void processDayEvents(LocalDate day,
                                  List<TimeEvent> events,
                                  int totalParticipants,
                                  int slotDuration,
                                  Map<LocalDateTime, Integer> result) {
        int currentBusy = 0;
        LocalTime currentStart = WORK_START;

        for (TimeEvent event : events) {
            if (event.time().isAfter(WORK_END)) break;

            if (!currentStart.equals(event.time())) {
                addTimeSlots(day, currentStart, event.time(), totalParticipants - currentBusy, slotDuration, result);
            }

            currentBusy += event.type() == EventType.START ? 1 : -1;
            currentStart = event.time();
        }

        addTimeSlots(day, currentStart, WORK_END, totalParticipants - currentBusy, slotDuration, result);
    }

    private void addTimeSlots(LocalDate day,
                              LocalTime start,
                              LocalTime end,
                              int available,
                              int duration,
                              Map<LocalDateTime, Integer> result) {
        if (available <= 0 || start.isAfter(end)) return;

        generateTimeSlots(day, start, end, duration)
                .forEach(slot -> result.merge(slot, available, Math::max));
    }

    private List<LocalDateTime> generateTimeSlots(LocalDate day, LocalTime start, LocalTime end, int duration) {
        return Stream.iterate(start, t -> !t.plusMinutes(duration).isAfter(end), t -> t.plusMinutes(duration))
                .map(t -> LocalDateTime.of(day, t))
                .toList();
    }

    private AvailabilityResponse buildResponse(Long meetingId, Map<LocalDateTime, Integer> availability) {
        if (availability.isEmpty()) {
            return new AvailabilityResponse(meetingId, List.of(), 0);
        }

        int max = availability.values().stream().max(Integer::compare).orElse(0);
        List<LocalDateTime> bestSlots = availability.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        return new AvailabilityResponse(meetingId, bestSlots, max);
    }
}
