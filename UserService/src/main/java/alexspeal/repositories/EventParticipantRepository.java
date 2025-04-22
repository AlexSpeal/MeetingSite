package alexspeal.repositories;

import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.enums.AcceptStatusParticipant;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface EventParticipantRepository extends CrudRepository<EventParticipantEntity, Long> {
    List<EventParticipantEntity> findByEventId(long eventId);

    Optional<EventParticipantEntity> findByUserIdAndEvent(long participantId, EventEntity event);

    Optional<EventParticipantEntity> findByEventIdAndUserId(Long eventId, Long userId);

    List<EventParticipantEntity> findByEventIdAndStatus(long meetingId, AcceptStatusParticipant status);

}