package alexspeal.dto;

import alexspeal.enums.AcceptStatusEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EventDto(Long id, String title, String description,
                       Long authorId, List<LocalDate> possibleDays,
                       List<EventParticipantsDto> participants,
                       Boolean isPersonal, LocalDateTime startTime,
                       Integer duration,
                       AcceptStatusEvent status,
                       LocalDateTime createdAt
) {
}


