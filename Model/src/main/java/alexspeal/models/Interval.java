package alexspeal.models;

import java.time.LocalDate;
import java.time.LocalTime;

public record Interval(LocalDate date, LocalTime start, LocalTime end) {
}