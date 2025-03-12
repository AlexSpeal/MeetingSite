package alexspeal.dto;

import java.time.LocalTime;

public record BusyIntervalDto(LocalTime startTime, int duration) {
}