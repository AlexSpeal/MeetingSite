package alexspeal.dto.responses;

import alexspeal.models.Slot;

import java.util.List;

public record AvailabilityResponse(Long meetingId, List<Slot> possibleSlots) {
}
