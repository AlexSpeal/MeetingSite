package alexspeal.dto.requests;

import alexspeal.models.Participant;

import java.time.LocalDate;
import java.util.List;

public record CreatingMeetingRequest(String title, String description,
                                     Integer duration, List<LocalDate> possibleDays,
                                     List<Participant> participants) {
}
