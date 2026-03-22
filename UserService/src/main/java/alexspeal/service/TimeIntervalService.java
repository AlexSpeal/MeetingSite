package alexspeal.service;

import alexspeal.models.TimeInterval;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TimeIntervalService {

    public List<TimeInterval> subtract(TimeInterval full, List<TimeInterval> busy) {
        if (busy.isEmpty()) return List.of(full);

        List<TimeInterval> sorted = busy.stream()
                .sorted(Comparator.comparing(TimeInterval::start))
                .toList();

        List<TimeInterval> result = new ArrayList<>();
        LocalTime cursor = full.start();

        for (TimeInterval b : sorted) {
            if (b.start().isAfter(cursor)) {
                result.add(new TimeInterval(cursor, b.start()));
            }
            cursor = max(cursor, b.end());
        }

        if (cursor.isBefore(full.end())) {
            result.add(new TimeInterval(cursor, full.end()));
        }

        return result;
    }

    private LocalTime max(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }
}
