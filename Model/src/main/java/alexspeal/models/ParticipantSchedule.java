package alexspeal.models;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ParticipantSchedule(
        List<LocalDate> selectedDays,
        Map<LocalDate, List<TimeInterval>> availability
) {
}