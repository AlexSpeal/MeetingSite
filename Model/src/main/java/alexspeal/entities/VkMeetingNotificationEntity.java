package alexspeal.entities;

import alexspeal.models.VkMeetingNotificationId;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vk_meeting_notification")
@IdClass(VkMeetingNotificationId.class)
public class VkMeetingNotificationEntity {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    protected VkMeetingNotificationEntity() {
    }

    public VkMeetingNotificationEntity(Long eventId, Long userId) {
        this.eventId = eventId;
        this.userId = userId;
        this.sentAt = LocalDateTime.now();
    }

    public Long getEventId() {
        return eventId;
    }

    public Long getUserId() {
        return userId;
    }
}