package alexspeal.dto.responses;

import alexspeal.models.Interval;

import java.util.List;

public record AvailabilityIntervalsResponse(
        Long meetingId,
        List<Interval> possibleIntervals,
        int maxCount,
        boolean havePending
) {

}

