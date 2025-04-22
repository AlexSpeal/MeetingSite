package alexspeal.dto;

import alexspeal.enums.AcceptStatusParticipant;

import java.time.LocalDate;
import java.util.List;

public record EventParticipantsDto(
        Long id,
        Long eventId,
        Long userId,
        UserDetailsDto user,
        AcceptStatusParticipant status,
        List<LocalDate> selectedDays
) {
}
