package alexspeal.controllers;

import alexspeal.dto.EventDto;
import alexspeal.dto.UserDetailsDto;
import alexspeal.dto.responses.GetAllUserEventsResponse;
import alexspeal.entities.UserEntity;
import alexspeal.enums.ErrorMessage;
import alexspeal.enums.SortOption;
import alexspeal.exceptions.AppError;
import alexspeal.service.EventService;
import alexspeal.service.UserService;
import alexspeal.utils.JwtTokenUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/secured/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final EventService eventService;
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;

    private UserEntity getUserFromHeader(String authHeader) {
        String jwtToken = authHeader.replace("Bearer ", "");
        String username = jwtTokenUtils.getUsername(jwtToken);
        return userService.findUserEntityByUsername(username)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_USERNAME.getMessage()));
    }

    @GetMapping("/{user_id}")
    public ResponseEntity<?> getUserById(@PathVariable Long user_id) {
        try {
            UserDetailsDto userDetails = userService.findById(user_id);
            return ResponseEntity.ok(userDetails);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserByUsername(@RequestParam String username) {
        try {
            UserDetailsDto userDetails = userService.findByUsername(username);
            return ResponseEntity.ok(userDetails);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage());
        }
    }

    @GetMapping("/meetings")
    public ResponseEntity<?> getAllUserMeetings(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "сортировка", required = true, example = "date")
            @RequestParam(name = "sort", defaultValue = "DATE") SortOption sortOption) {

        try {
            UserEntity user = getUserFromHeader(authHeader);
            List<EventDto> eventList = eventService.getAllUserEvents(user.getId(), sortOption);
            return ResponseEntity.ok(new GetAllUserEventsResponse(eventList));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

}