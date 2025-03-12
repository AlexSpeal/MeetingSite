package alexspeal.repositories;

import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.enums.AcceptStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipantRepository extends CrudRepository<EventParticipantEntity, Long> {
    List<EventParticipantEntity> findByEventId(long eventId);

    //может не работать findByUserId (заменить на query запрос или передавать user)
    List<EventParticipantEntity> findByUserId(long participantId);

    Optional<EventParticipantEntity> findByUserIdAndEvent(long participantId, EventEntity event);

    List<EventParticipantEntity> findByStatus(String status);

    List<EventParticipantEntity> findByEventIdAndStatus(long meetingId, AcceptStatus status);
}