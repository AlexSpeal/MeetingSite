package alexspeal.service;

import alexspeal.models.AvailabilitySegment;
import alexspeal.models.Interval;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class IntervalService {

    public List<Interval> toMeetingIntervals(
            LocalDate date,
            List<AvailabilitySegment> segments,
            int durationMinutes
    ) {
        List<Interval> result = new ArrayList<>();

        for (AvailabilitySegment segment : segments) {
            result.addAll(expandSegmentToMeetings(date, segment, durationMinutes));
        }

        return result;
    }

    private List<Interval> expandSegmentToMeetings(
            LocalDate date,
            AvailabilitySegment segment,
            int durationMinutes
    ) {
        List<Interval> meetings = new ArrayList<>();

        long segmentLength = Duration.between(segment.start(), segment.end()).toMinutes();
        if (segmentLength < durationMinutes) {
            return meetings;
        }

        LocalTime start = segment.start();
        LocalTime latestStart = segment.end().minusMinutes(durationMinutes);

        while (!start.isAfter(latestStart)) {
            meetings.add(new Interval(date, start, start.plusMinutes(durationMinutes)));
            start = start.plusMinutes(1);
        }

        return meetings;
    }
}