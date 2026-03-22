package alexspeal.models;

import java.time.LocalTime;

public record AvailabilitySegment(
        LocalTime start,
        LocalTime end,
        int participantCount
) {
}
