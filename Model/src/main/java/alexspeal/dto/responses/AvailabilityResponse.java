package alexspeal.dto.responses;

import java.time.LocalDateTime;
import java.util.List;

public record AvailabilityResponse(Long meetingId, List<LocalDateTime> possibleSlots, Integer maxCount) {
}
