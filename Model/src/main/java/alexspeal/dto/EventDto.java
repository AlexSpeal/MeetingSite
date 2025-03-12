package alexspeal.dto;

import alexspeal.enums.AcceptStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EventDto(Long id, String title, String description,
                       AcceptStatus status, LocalDateTime startTime,
                       Integer duration, List<LocalDate> possibleDays, Long author) {
}
