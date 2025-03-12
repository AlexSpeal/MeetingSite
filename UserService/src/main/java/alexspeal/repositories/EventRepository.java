package alexspeal.repositories;

import alexspeal.dto.BusyIntervalDto;
import alexspeal.entities.EventEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends CrudRepository<EventEntity, Long> {
    @Modifying
    @Query("SELECT new alexspeal.dto.BusyIntervalDto(CAST(e.startTime AS LocalTime), e.duration) " +
            "FROM EventEntity e " +
            "LEFT JOIN e.eventParticipants ep " +
            "WHERE ((e.author.id = :userId AND e.startTime IS NOT NULL) " +
            "OR (ep.user.id = :userId AND ep.status = 'ACCEPTED')) " +
            "AND DATE(e.startTime) = :day")
    List<BusyIntervalDto> getBusyIntervals(@Param("userId") Long userId, @Param("day") LocalDate day);
}
