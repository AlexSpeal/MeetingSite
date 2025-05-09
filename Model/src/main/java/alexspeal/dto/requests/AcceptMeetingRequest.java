package alexspeal.dto.requests;

import alexspeal.enums.AcceptStatusParticipant;

import java.time.LocalDate;
import java.util.List;

public record AcceptMeetingRequest(List<LocalDate> selectedDays, AcceptStatusParticipant status) {
}
