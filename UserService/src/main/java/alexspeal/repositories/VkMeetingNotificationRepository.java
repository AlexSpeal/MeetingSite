package alexspeal.repositories;

import alexspeal.entities.VkMeetingNotificationEntity;
import alexspeal.models.VkMeetingNotificationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VkMeetingNotificationRepository
        extends JpaRepository<VkMeetingNotificationEntity, VkMeetingNotificationId> {
}
