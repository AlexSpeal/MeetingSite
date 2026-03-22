package alexspeal.utils;

import alexspeal.entities.UserEntity;
import alexspeal.enums.ErrorMessage;
import alexspeal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class JwtIdentificationUtils {
    private final JwtTokenUtils jwtTokenUtils;
    private final UserService userService;

    public UserEntity getUserFromHeader(String authHeader) {
        String jwtToken = authHeader.replace("Bearer ", "");
        String username = jwtTokenUtils.getUsername(jwtToken);
        return userService.findUserEntityByUsername(username)
                .orElseThrow(()
                        -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_USERNAME + username));
    }
}
