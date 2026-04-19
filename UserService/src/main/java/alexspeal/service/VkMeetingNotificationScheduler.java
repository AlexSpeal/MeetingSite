package alexspeal.service;

import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.entities.UserEntity;
import alexspeal.entities.VkMeetingNotificationEntity;
import alexspeal.enums.AcceptStatusParticipant;
import alexspeal.models.VkMeetingNotificationId;
import alexspeal.repositories.MeetingRepository;
import alexspeal.repositories.VkMeetingNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class VkMeetingNotificationScheduler {

    private final MeetingRepository meetingRepository;
    private final VkMeetingNotificationRepository notificationRepository;
    private final VkNotificationService vkNotificationService;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void sendNotifications() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime from = now.plusMinutes(15);
        LocalDateTime to = now.plusMinutes(16);

        var events = meetingRepository.findMeetingsStartingBetween(from, to);

        for (EventEntity event : events) {
            notifyEvent(event);
        }
    }

    private void notifyEvent(EventEntity event) {
        Set<UserEntity> users = new HashSet<>();

        users.add(event.getAuthor());

        for (EventParticipantEntity participant : event.getEventParticipants()) {
            if (participant.getStatus() == AcceptStatusParticipant.ACCEPTED) {
                users.add(participant.getUser());
            }
        }

        for (UserEntity user : users) {
            if (user.getVkUserId() == null) {
                continue;
            }

            if (notificationRepository.existsById(
                    new VkMeetingNotificationId(event.getId(), user.getId())
            )) {
                continue;
            }

            sendNotification(event, user);
        }
    }

    private void sendNotification(EventEntity event, UserEntity user) {
        String message = buildMessage(event);

        try {
            vkNotificationService.sendMessage(user.getVkUserId(), message);

            notificationRepository.save(
                    new VkMeetingNotificationEntity(event.getId(), user.getId())
            );

            log.info("VK уведомление отправлено eventId={}, userId={}", event.getId(), user.getId());
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления eventId={}, userId={}", event.getId(), user.getId(), e);
        }
    }

    private String buildMessage(EventEntity event) {
        return """
                Напоминание о встрече
                                
                Встреча "%s"
                Начнется через 15 минут
                                
                Длительность: %d минут
                """.formatted(
                event.getTitle(),
                event.getDuration()
        );
    }
}