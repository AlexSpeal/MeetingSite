package alexspeal.service;

import alexspeal.dto.responses.AvailabilityIntervalsResponse;
import alexspeal.entities.DayEntity;
import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.enums.AcceptStatusParticipant;
import alexspeal.enums.ErrorMessage;
import alexspeal.enums.EventType;
import alexspeal.models.Interval;
import alexspeal.models.ParticipantSchedule;
import alexspeal.models.TimeEvent;
import alexspeal.models.TimeInterval;
import alexspeal.repositories.EventParticipantRepository;
import alexspeal.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    public AvailabilityIntervalsResponse getMeetingAvailability(Long meetingId) {
        EventEntity meeting = eventRepository.findById(meetingId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));
        int duration = meeting.getDuration();

        boolean havePending = meeting.getEventParticipants().stream()
                .anyMatch(p -> p.getStatus().equals(AcceptStatusParticipant.PENDING));

        Long authorId = meeting.getAuthor().getId();
        EventParticipantEntity authorPart = eventParticipantRepository
                .findByEventIdAndUserId(meetingId, authorId)
                .orElseThrow(() -> new IllegalStateException(ErrorMessage.NOT_FOUND_AUTHOR.getMessage()));
        List<LocalDate> dates = authorPart.getDays().stream().map(DayEntity::getDate).toList();

        List<EventParticipantEntity> acceptedParticipants = eventParticipantRepository
                .findByEventIdAndStatus(meetingId, AcceptStatusParticipant.ACCEPTED);

        ParticipantSchedule authorSchedule = createParticipantSchedule(authorPart, duration);
        List<ParticipantSchedule> schedules = Stream.concat(
                Stream.of(authorSchedule),
                acceptedParticipants.stream()
                        .filter(p -> !p.getUser().getId().equals(authorId))
                        .map(p -> createParticipantSchedule(p, duration))
        ).toList();

        List<Interval> intervals = new ArrayList<>();
        int maxParticipants = 0;

        for (LocalDate date : dates) {
            Map<LocalDateTime, Integer> availability = computeAvailability(date, schedules, duration);
            int dateMaxParticipants = availability.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            maxParticipants = Math.max(maxParticipants, dateMaxParticipants);

            List<TimeInterval> authorAvail = authorSchedule.availability().getOrDefault(date, List.of());
            List<LocalDateTime> slots = availability.entrySet().stream()
                    .filter(e -> e.getValue() >= dateMaxParticipants)
                    .map(Map.Entry::getKey)
                    .filter(dt -> {
                        LocalDateTime endDt = dt.plusMinutes(duration);
                        return !endDt.toLocalTime().isAfter(WORK_END) &&
                                authorAvail.stream().anyMatch(iv ->
                                        !dt.toLocalTime().isBefore(iv.start()) &&
                                                !endDt.toLocalTime().isAfter(iv.end()));
                    })
                    .sorted()
                    .toList();

            intervals.addAll(groupToIntervals(date, slots));
        }

        return new AvailabilityIntervalsResponse(meetingId, intervals, maxParticipants, havePending);
    }

    private List<Interval> groupToIntervals(LocalDate date, List<LocalDateTime> slots) {
        if (slots.isEmpty()) return List.of();

        List<Interval> intervals = new ArrayList<>();
        LocalDateTime start = slots.getFirst();
        LocalDateTime prev = start;

        for (LocalDateTime current : slots.subList(1, slots.size())) {
            if (current.minusMinutes(1).isAfter(prev)) {
                intervals.add(new Interval(date, start.toLocalTime(), prev.toLocalTime()));
                start = current;
            }
            prev = current;
        }

        intervals.add(new Interval(date, start.toLocalTime(), prev.toLocalTime()));

        return intervals;
    }

    private ParticipantSchedule createParticipantSchedule(EventParticipantEntity participant, int duration) {
        List<LocalDate> selectedDays = participant.getDays()
                .stream()
                .map(DayEntity::getDate)
                .toList();

        Map<LocalDate, List<TimeInterval>> availability = new HashMap<>();
        Map<LocalDate, List<TimeInterval>> busy = new HashMap<>();

        eventRepository.getBusyIntervals(participant.getUser().getId(), selectedDays)
                .forEach(dto -> {
                    LocalDateTime start = dto.startTime();
                    LocalDateTime end = start.plusMinutes(dto.duration());
                    LocalDate day = start.toLocalDate();

                    if (selectedDays.contains(day)) {
                        busy.computeIfAbsent(day, k -> new ArrayList<>())
                                .add(new TimeInterval(start.toLocalTime(), end.toLocalTime()));
                    }
                });

        for (LocalDate day : selectedDays) {
            List<TimeInterval> busyIntervals = busy.getOrDefault(day, List.of());
            List<TimeInterval> available = subtractIntervals(new TimeInterval(WORK_START, WORK_END), busyIntervals);
            List<TimeInterval> filteredAvailable = available.stream()
                    .filter(iv -> Duration.between(iv.start(), iv.end()).toMinutes() >= duration)
                    .toList();
            availability.put(day, filteredAvailable);
        }

        return new ParticipantSchedule(selectedDays, availability);
    }

    private List<TimeInterval> subtractIntervals(TimeInterval full, List<TimeInterval> busy) {
        List<TimeInterval> result = new ArrayList<>();
        LocalTime cursor = full.start();
        List<TimeInterval> sortedBusy = busy.stream()
                .sorted(Comparator.comparing(TimeInterval::start))
                .toList();

        for (TimeInterval b : sortedBusy) {
            if (b.start().isAfter(cursor)) {
                result.add(new TimeInterval(cursor, b.start()));
            }
            cursor = b.end().isAfter(cursor) ? b.end() : cursor;
        }

        if (cursor.isBefore(full.end())) {
            result.add(new TimeInterval(cursor, full.end()));
        }

        return result;
    }

    private Map<LocalDateTime, Integer> computeAvailability(LocalDate day, List<ParticipantSchedule> schedules, int duration) {
        List<TimeEvent> events = schedules.stream()
                .filter(s -> s.selectedDays().contains(day))
                .flatMap(s -> s.availability().getOrDefault(day, List.of()).stream()
                        .flatMap(iv -> Stream.of(
                                new TimeEvent(iv.start(), EventType.START),
                                new TimeEvent(iv.end(), EventType.END))))
                .sorted(Comparator.comparing(TimeEvent::time))
                .toList();

        Map<LocalDateTime, Integer> result = new HashMap<>();
        int available = 0;
        LocalTime cursor = WORK_START;
        for (TimeEvent event : events) {
            if (event.time().isAfter(WORK_END)) break;

            if (!cursor.equals(event.time())) {
                final int currentAvailable = available;
                generateTimeSlots(day, cursor, event.time(), duration)
                        .forEach(slot -> result.put(slot, currentAvailable));
            }

            available += event.type() == EventType.START ? 1 : -1;
            cursor = event.time();
        }

        return result;
    }

    private List<LocalDateTime> generateTimeSlots(LocalDate day, LocalTime start, LocalTime end, int duration) {
        List<LocalDateTime> slots = new ArrayList<>();
        LocalDateTime slot = LocalDateTime.of(day, start);
        LocalDateTime windowEnd = LocalDateTime.of(day, end);

        while (!slot.plusMinutes(duration).isAfter(windowEnd)) {
            slots.add(slot);
            slot = slot.plusMinutes(1);
        }

        return slots;
    }
}