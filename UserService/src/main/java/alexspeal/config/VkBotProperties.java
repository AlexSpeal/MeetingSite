package alexspeal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vk.bot")
public record VkBotProperties(
        boolean enabled,
        Long groupId,
        String accessToken,
        Integer reminderMinutesBefore,
        Integer codeTtlMinutes,
        Integer maxAttempts
) {
}
