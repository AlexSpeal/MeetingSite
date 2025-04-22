package alexspeal.repositories;

import alexspeal.entities.DayEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DayRepository extends CrudRepository<DayEntity, Long> {
    List<DayEntity> findByEventParticipantId(Long participantId);

    void deleteByEventParticipantId(Long participantId);
}
