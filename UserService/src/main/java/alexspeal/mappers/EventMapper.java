package alexspeal.mappers;

import alexspeal.dto.EventDto;
import alexspeal.dto.EventParticipantsDto;
import alexspeal.entities.DayEntity;
import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.enums.ErrorMessage;
import alexspeal.repositories.DayRepository;
import alexspeal.repositories.EventParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final EventParticipantMapper participantMapper;
    private final DayRepository dayRepository;
    private final EventParticipantRepository eventParticipantRepository;

    public EventDto toEventDto(EventEntity event) {
        EventParticipantEntity authorParticipant = eventParticipantRepository
                .findByEventIdAndUserId(event.getId(), event.getAuthor().getId())
                .orElseThrow(() -> new IllegalStateException(ErrorMessage.NOT_FOUND_AUTHOR.getMessage()));

        List<LocalDate> possibleDays = dayRepository
                .findByEventParticipantId(authorParticipant.getId())
                .stream()
                .map(DayEntity::getDate)
                .toList();

        List<EventParticipantsDto> participants = eventParticipantRepository
                .findByEventId(event.getId())
                .stream()
                .map(participantMapper::toEventParticipantsDto)
                .toList();

        return new EventDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getAuthor().getId(),
                possibleDays,
                participants,
                event.getIsPersonal(),
                event.getStartTime(),
                event.getDuration(),
                event.getStatus(),
                event.getCreatedAt()
        );
    }
}