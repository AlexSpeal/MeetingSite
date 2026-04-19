package alexspeal.entities;
import alexspeal.models.VkBindingAttemptId;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vk_binding_attempt")
@Data
@NoArgsConstructor
@IdClass(VkBindingAttemptId.class)
public class VkBindingAttemptEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "vk_user_id", nullable = false)
    private Long vkUserId;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Setter
    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    public VkBindingAttemptEntity(
            Long userId,
            LocalDateTime expiresAt,
            Long vkUserId,
            String codeHash,
            Integer attempts
    ) {
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.vkUserId = vkUserId;
        this.codeHash = codeHash;
        this.attempts = attempts;
    }
}