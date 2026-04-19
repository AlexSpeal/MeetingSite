package alexspeal.service;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class VkNotificationService {

    private final VkApiClient vkApiClient;
    private final GroupActor groupActor;

    public void sendMessage(String screenName, String message) {
        Long vkUserId = resolveVkUserIdByScreenName(screenName);
        sendMessage(vkUserId, message);
    }

    public void sendMessage(Long vkUserId, String message) {
        try {
            vkApiClient.messages()
                    .sendDeprecated(groupActor)
                    .userId(vkUserId)
                    .randomId(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE))
                    .message(message)
                    .execute();

            log.info("VK message sent to vkUserId={}", vkUserId);
        } catch (Exception e) {
            log.error("Failed to send VK message to vkUserId={}", vkUserId, e);
            throw new RuntimeException("Не удалось отправить сообщение в VK", e);
        }
    }

    public Long resolveVkUserIdByScreenName(String screenName) {
        try {
            var result = vkApiClient.utils()
                    .resolveScreenName(groupActor)
                    .screenName(screenName)
                    .execute();

            if (result == null || result.getObjectId() == null) {
                throw new IllegalArgumentException("VK пользователь с таким логином не найден");
            }

            return result.getObjectId().longValue();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось определить VK user id по логину", e);
        }
    }
}