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

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@Transactional
public interface MeetingRepository extends CrudRepository<EventEntity, Long> {
    @Query("""
                SELECT new alexspeal.dto.BusyIntervalDto(e.id, e.startTime, e.duration, e.isFixed, e.isPersonal)
                FROM EventEntity e
                LEFT JOIN e.eventParticipants ep ON ep.user.id = :userId
                WHERE e.startTime IS NOT NULL
                AND ep.status = 'ACCEPTED'
                AND e.startTime >= :from
                AND e.startTime < :to
            """)
    List<BusyIntervalDto> getBusyIntervals(@Param("userId") Long userId,
                                           @Param("from") OffsetDateTime from,
                                           @Param("to") OffsetDateTime to);

    @Query("""
                SELECT DISTINCT e
                FROM EventEntity e
                JOIN e.eventParticipants ep
                WHERE ep.user.id = :userId
                AND ep.status = 'ACCEPTED'
                AND e.startTime IS NOT NULL
                AND e.isPersonal = TRUE
                AND e.isFixed = FALSE
                AND e.startTime < :endExclusive
                AND e.startTime >= :startInclusive
            """)
    List<EventEntity> findUserMovablePersonalEvents(@Param("userId") Long userId,
                                                    @Param("startInclusive") OffsetDateTime startInclusive,
                                                    @Param("endExclusive") OffsetDateTime endExclusive);

    @Modifying
    @Query("UPDATE EventEntity e SET e.startTime = :startTime WHERE e.id = :eventId")
    void updateEventStartTime(@Param("eventId") Long eventId,
                              @Param("startTime") OffsetDateTime startTime);

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
            @Param("startTime") OffsetDateTime startTime,
            @Param("status") AcceptStatusEvent status);

    @Query("""
    SELECT e
    FROM EventEntity e
    WHERE e.status = alexspeal.enums.AcceptStatusEvent.ACCEPTED
      AND e.startTime >= :from
      AND e.startTime < :to
""")
    List<EventEntity> findMeetingsStartingBetween(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
