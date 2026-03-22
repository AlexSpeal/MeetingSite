package alexspeal.helpers;

import alexspeal.enums.EventType;
import alexspeal.models.AvailabilitySegment;
import alexspeal.models.ParticipantSchedule;
import alexspeal.models.TimeEvent;
import alexspeal.models.TimeInterval;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class AvailabilityCalculator {

    public List<AvailabilitySegment> calculateSegments(
            LocalDate day,
            List<ParticipantSchedule> schedules,
            List<TimeInterval> allowedWindows
    ) {
        List<AvailabilitySegment> result = new ArrayList<>();

        for (TimeInterval allowedWindow : allowedWindows) {
            result.addAll(calculateWindowSegments(day, schedules, allowedWindow));
        }

        return mergeAdjacentSegments(result);
    }

    private List<AvailabilitySegment> calculateWindowSegments(
            LocalDate day,
            List<ParticipantSchedule> schedules,
            TimeInterval allowedWindow
    ) {
        List<TimeEvent> events = schedules.stream()
                .flatMap(schedule -> schedule.availability().getOrDefault(day, List.of()).stream())
                .map(interval -> intersect(interval, allowedWindow))
                .filter(interval -> interval != null && interval.start().isBefore(interval.end()))
                .flatMap(interval -> Stream.of(
                        new TimeEvent(interval.start(), EventType.START),
                        new TimeEvent(interval.end(), EventType.END)
                ))
                .sorted(Comparator
                        .comparing(TimeEvent::time)
                        .thenComparing(event -> event.type() == EventType.END ? 0 : 1))
                .toList();

        return getAvailabilitySegments(allowedWindow, events);
    }

    private static List<AvailabilitySegment> getAvailabilitySegments(TimeInterval allowedWindow, List<TimeEvent> events) {
        List<AvailabilitySegment> segments = new ArrayList<>();
        int available = 0;
        LocalTime cursor = allowedWindow.start();

        for (TimeEvent event : events) {
            if (cursor.isBefore(event.time()) && available > 0) {
                segments.add(new AvailabilitySegment(cursor, event.time(), available));
            }

            available += event.type() == EventType.START ? 1 : -1;
            cursor = event.time();
        }

        if (cursor.isBefore(allowedWindow.end()) && available > 0) {
            segments.add(new AvailabilitySegment(cursor, allowedWindow.end(), available));
        }
        return segments;
    }

    private TimeInterval intersect(TimeInterval interval, TimeInterval window) {
        LocalTime start = max(interval.start(), window.start());
        LocalTime end = min(interval.end(), window.end());

        if (!start.isBefore(end)) {
            return null;
        }

        return new TimeInterval(start, end);
    }

    private List<AvailabilitySegment> mergeAdjacentSegments(List<AvailabilitySegment> segments) {
        if (segments.isEmpty()) {
            return List.of();
        }

        List<AvailabilitySegment> sorted = segments.stream()
                .sorted(Comparator.comparing(AvailabilitySegment::start))
                .toList();

        List<AvailabilitySegment> result = new ArrayList<>();
        AvailabilitySegment current = sorted.getFirst();

        for (int i = 1; i < sorted.size(); i++) {
            AvailabilitySegment next = sorted.get(i);

            if (current.end().equals(next.start())
                    && current.participantCount() == next.participantCount()) {
                current = new AvailabilitySegment(
                        current.start(),
                        next.end(),
                        current.participantCount()
                );
            } else {
                result.add(current);
                current = next;
            }
        }

        result.add(current);
        return result;
    }

    private LocalTime max(LocalTime first, LocalTime second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalTime min(LocalTime first, LocalTime second) {
        return first.isBefore(second) ? first : second;
    }
}