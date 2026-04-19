package alexspeal.repositories;

import alexspeal.entities.VkBindingAttemptEntity;
import alexspeal.models.VkBindingAttemptId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VkBindingAttemptRepository extends JpaRepository<VkBindingAttemptEntity, VkBindingAttemptId> {

    Optional<VkBindingAttemptEntity> findFirstByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(Long userId, LocalDateTime now);
    Optional<VkBindingAttemptEntity> findFirstByUserIdOrderByExpiresAtDesc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update VkBindingAttemptEntity a
           set a.attempts = a.attempts + 1
         where a.userId = :userId
           and a.expiresAt = :expiresAt
    """)
    int incrementAttempts(@Param("userId") Long userId,
                          @Param("expiresAt") LocalDateTime expiresAt);
}