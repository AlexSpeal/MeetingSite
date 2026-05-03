package alexspeal.dto;

import java.time.OffsetDateTime;

public record BusyIntervalDto(Long eventId, OffsetDateTime startTime, int duration,
                              Boolean isFixed, Boolean isPersonal) {
}
