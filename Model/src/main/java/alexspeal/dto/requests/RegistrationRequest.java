package alexspeal.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record RegistrationRequest(
        @org.jetbrains.annotations.NotNull
        @JsonProperty("username") String username,

        @NotNull
        @JsonProperty("password") String password,

        @JsonProperty("timezone") String timezone,

        @JsonProperty("dailyLoadMinutes") Integer dailyLoadMinutes
) {
}
