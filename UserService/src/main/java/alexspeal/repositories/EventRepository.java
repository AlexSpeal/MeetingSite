package alexspeal.repositories;

import alexspeal.dto.BusyIntervalDto;
import alexspeal.entities.EventEntity;
import alexspeal.enums.AcceptStatusEvent;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional
public interface EventRepository extends CrudRepository<EventEntity, Long> {
    @Query("""
                SELECT new alexspeal.dto.BusyIntervalDto(e.startTime, e.duration)
                FROM EventEntity e
                LEFT JOIN e.eventParticipants ep ON ep.user.id = :userId
                WHERE e.startTime IS NOT NULL
                AND  ep.status = 'ACCEPTED'
                AND DATE(e.startTime) IN :days
            """)
    List<BusyIntervalDto> getBusyIntervals(@Param("userId") Long userId, @Param("days") List<LocalDate> days);

    @Query("""
            SELECT e
            FROM EventEntity e
            JOIN e.eventParticipants ep ON ep.user.id = :userId
            """)
    List<EventEntity> getAllUserEvents(@Param("userId") Long userId);


    @Modifying
    @Query("UPDATE EventEntity e SET e.startTime = :startTime, e.status = :status WHERE e.id = :meetingId")
    void updateEventStartTimeAndStatus(
            @Param("meetingId") Long meetingId,
            @Param("startTime") LocalDateTime startTime,
            @Param("status") AcceptStatusEvent status);
}
