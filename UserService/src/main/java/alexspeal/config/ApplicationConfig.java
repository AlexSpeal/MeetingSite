package alexspeal.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

@ConfigurationProperties(prefix = "app")
public record ApplicationConfig(@NotNull String baseUrlStat,
                                @NotNull LocalTime workStart,
                                @NotNull LocalTime workEnd) {
}
