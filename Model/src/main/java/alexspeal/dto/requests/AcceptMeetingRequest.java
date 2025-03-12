package alexspeal.dto.requests;

import alexspeal.enums.AcceptStatus;

import java.time.LocalDate;
import java.util.List;

public record AcceptMeetingRequest(List<LocalDate> selectedDays, AcceptStatus acceptStatus) {
}
