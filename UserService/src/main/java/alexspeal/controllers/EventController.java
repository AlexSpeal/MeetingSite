package alexspeal.controllers;

import alexspeal.dto.EventDto;
import alexspeal.dto.requests.AcceptMeetingRequest;
import alexspeal.dto.requests.CreatingMeetingRequest;
import alexspeal.dto.responses.AvailabilityResponse;
import alexspeal.entities.UserEntity;
import alexspeal.enums.ErrorMessage;
import alexspeal.exceptions.AppError;
import alexspeal.service.EventService;
import alexspeal.service.UserService;
import alexspeal.utils.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController("/secured/meeting")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;

    @GetMapping("/{meeting_id}/availability")
    public ResponseEntity<?> getMeetingAvailability(@RequestHeader("Authorization") String authHeader, @PathVariable("meeting_id") Long meetingId) {
        String jwtToken = authHeader.replace("Bearer ", "");
        String username = jwtTokenUtils.getUsername(jwtToken);
        UserEntity user = userService.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_USERNAME.getMessage()));

        try {
            EventDto event = eventService.getEventById(meetingId);
            if (!event.author().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new AppError(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase()));
            }

            AvailabilityResponse response = eventService.getMeetingAvailability(meetingId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @PostMapping
    ResponseEntity<?> createMeeting(@RequestHeader("Authorization") String authHeader,
                                    @RequestBody CreatingMeetingRequest creatingMeetingRequest) {
        String jwtToken = authHeader.replace("Bearer ", "");
        String username = jwtTokenUtils.getUsername(jwtToken);

        UserEntity user = userService.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_USERNAME.getMessage()));

        try {

            Long idMeeting = eventService.createMeeting(user, creatingMeetingRequest);
            return ResponseEntity.ok(idMeeting);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @PostMapping("/{meeting_id}/selectDays")
    public ResponseEntity<?> acceptMeeting(@RequestHeader("Authorization") String authHeader,
                                           @RequestBody AcceptMeetingRequest acceptMeetingRequest,
                                           @PathVariable("meeting_id") Long meetingId) {
        String jwtToken = authHeader.replace("Bearer ", "");
        String username = jwtTokenUtils.getUsername(jwtToken);

        UserEntity user = userService.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_USERNAME.getMessage()));

        try {
            eventService.acceptMeeting(user, acceptMeetingRequest, meetingId);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }
}