package alexspeal.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegistrationResponse(@JsonProperty("token") String token
) {
}
