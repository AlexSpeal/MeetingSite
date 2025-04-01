package alexspeal.dto;

import java.time.LocalDateTime;

public record BusyIntervalDto(LocalDateTime startTime, int duration) {
}