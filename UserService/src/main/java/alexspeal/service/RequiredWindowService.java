package alexspeal.service;

import alexspeal.models.ParticipantSchedule;
import alexspeal.models.TimeInterval;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RequiredWindowService {

    public List<TimeInterval> findAllowedWindows(
            LocalDate day,
            List<ParticipantSchedule> requiredSchedules,
            int durationMinutes
    ) {
        if (requiredSchedules.isEmpty()) {
            return List.of();
        }

        List<TimeInterval> intersection = new ArrayList<>(
                requiredSchedules.getFirst().availability().getOrDefault(day, List.of())
        );

        for (int i = 1; i < requiredSchedules.size(); i++) {
            List<TimeInterval> next = requiredSchedules.get(i)
                    .availability()
                    .getOrDefault(day, List.of());

            intersection = intersect(intersection, next);

            if (intersection.isEmpty()) {
                return List.of();
            }
        }

        return intersection.stream()
                .filter(interval -> Duration.between(interval.start(), interval.end()).toMinutes() >= durationMinutes)
                .toList();
    }

    private List<TimeInterval> intersect(List<TimeInterval> first, List<TimeInterval> second) {
        List<TimeInterval> left = first.stream()
                .sorted(Comparator.comparing(TimeInterval::start))
                .toList();

        List<TimeInterval> right = second.stream()
                .sorted(Comparator.comparing(TimeInterval::start))
                .toList();

        List<TimeInterval> result = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < left.size() && j < right.size()) {
            TimeInterval a = left.get(i);
            TimeInterval b = right.get(j);

            LocalTime start = max(a.start(), b.start());
            LocalTime end = min(a.end(), b.end());

            if (start.isBefore(end)) {
                result.add(new TimeInterval(start, end));
            }

            if (a.end().isBefore(b.end())) {
                i++;
            } else {
                j++;
            }
        }

        return result;
    }

    private LocalTime max(LocalTime first, LocalTime second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalTime min(LocalTime first, LocalTime second) {
        return first.isBefore(second) ? first : second;
    }
}