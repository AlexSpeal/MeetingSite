package alexspeal.repositories;

import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.enums.AcceptStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface EventParticipantRepository extends CrudRepository<EventParticipantEntity, Long> {
    List<EventParticipantEntity> findByEventId(long eventId);

    List<EventParticipantEntity> findByUserId(long participantId);

    Optional<EventParticipantEntity> findByUserIdAndEvent(long participantId, EventEntity event);

    List<EventParticipantEntity> findByStatus(String status);

    List<EventParticipantEntity> findByEventIdAndStatus(long meetingId, AcceptStatus status);
}