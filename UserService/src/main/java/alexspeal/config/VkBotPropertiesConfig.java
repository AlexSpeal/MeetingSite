package alexspeal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VkBotProperties.class)
public class VkBotPropertiesConfig {
}
