package alexspeal.mappers;

import alexspeal.dto.EventParticipantsDto;
import alexspeal.entities.DayEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.repositories.DayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventParticipantMapper {

    private final DayRepository dayRepository;

    public EventParticipantsDto toEventParticipantsDto(EventParticipantEntity participant) {
        List<LocalDate> selectedDays = dayRepository.findByEventParticipantId(participant.getId())
                .stream()
                .map(DayEntity::getDate)
                .toList();

        return new EventParticipantsDto(
                participant.getId(),
                participant.getEvent().getId(),
                participant.getUser().getId(),
                UserMapper.toUserDetailsDto(participant.getUser()),
                participant.getStatus(),
                selectedDays
        );
    }
}