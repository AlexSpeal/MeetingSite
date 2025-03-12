package alexspeal.models;

import java.time.LocalDate;
import java.time.LocalTime;

public record Slot(LocalDate day, LocalTime startTime, int duration) {
}
