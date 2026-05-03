package alexspeal.dto;

import alexspeal.enums.AcceptStatusEvent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public record EventDto(Long id, String title, String description,
                       Long authorId, List<LocalDate> possibleDays,
                       List<EventParticipantsDto> participants,
                       Boolean isPersonal, Boolean isFixed,
                       LocalTime preferredWindowStart, LocalTime preferredWindowEnd,
                       OffsetDateTime startTime,
                       Integer duration,
                       AcceptStatusEvent status,
                       OffsetDateTime createdAt
) {
}
