package alexspeal.controllers;

import alexspeal.dto.EventDto;
import alexspeal.dto.requests.AcceptMeetingRequest;
import alexspeal.dto.requests.CreatingMeetingRequest;
import alexspeal.dto.requests.ScheduleRequest;
import alexspeal.dto.responses.AvailabilityIntervalsResponse;
import alexspeal.dto.responses.AvailabilityResponse;
import alexspeal.entities.UserEntity;
import alexspeal.enums.AcceptStatusParticipant;
import alexspeal.enums.ErrorMessage;
import alexspeal.enums.WebSocketAction;
import alexspeal.exceptions.AppError;
import alexspeal.models.WebSocketMessage;
import alexspeal.service.EventService;
import alexspeal.service.SchedulingService;
import alexspeal.service.UserService;
import alexspeal.utils.JwtTokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/secured/meetings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class EventController {
    private final EventService eventService;
    private final UserService userService;
    private final SchedulingService schedulingService;
    private final JwtTokenUtils jwtTokenUtils;
    private final SimpMessagingTemplate messagingTemplate;

    private UserEntity getUserFromHeader(String authHeader) {
        String jwtToken = authHeader.replace("Bearer ", "");
        String username = jwtTokenUtils.getUsername(jwtToken);
        return userService.findUserEntityByUsername(username)
                .orElseThrow(()
                        -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_USERNAME + username));
    }

    @DeleteMapping("/{meeting_id}")
    public ResponseEntity<?> deleteEvent(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable("meeting_id") Long id,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader
    ) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            EventDto event = eventService.getEventById(id);
            if (!event.authorId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new AppError(HttpStatus.FORBIDDEN.value(), ErrorMessage.FORBIDDEN.getMessage()));
            }
            eventService.deleteEvent(id);
            notifyParticipants(event, WebSocketAction.DELETE, null);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        }
    }

    @Operation(
            summary = "Получение возможных дат встреч",
            description = "Возвращает доступные временные интервалы и статистику доступности участников"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение доступных дат",
                    content = @Content(schema = @Schema(implementation = AvailabilityResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидные параметры запроса",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ к встрече запрещен",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Встреча не найдена",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            )
    })
    @GetMapping("/{meeting_id}/availability")
    public ResponseEntity<?> getMeetingAvailability(
            @Parameter(description = "JWT токен авторизации", required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "ID встречи", required = true, example = "12345")
            @PathVariable("meeting_id") Long meetingId
    ) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            EventDto event = eventService.getEventById(meetingId);
            if (!event.authorId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new AppError(HttpStatus.FORBIDDEN.value(), ErrorMessage.FORBIDDEN.getMessage()));
            }
            AvailabilityIntervalsResponse response = schedulingService.getMeetingAvailability(meetingId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @Operation(
            summary = "Создание новой встречи",
            description = "Создает новую встречу и возвращает ее данные"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Встреча успешно создана",
                    content = @Content(schema = @Schema(implementation = EventDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации данных",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь или участник не найден",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> createMeeting(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @RequestBody CreatingMeetingRequest request
    ) {
        try {
            UserEntity author = getUserFromHeader(authHeader);
            EventDto meeting = eventService.createMeeting(author, request);
            notifyParticipants(meeting, WebSocketAction.CREATE, meeting);
            return ResponseEntity.ok(meeting);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @Operation(
            summary = "Выбор дат для встречи",
            description = "Добавление выбранных дат участником встречи"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Даты успешно добавлены"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидные данные",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Встреча или пользователь не найден",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            )
    })
    @PostMapping("/{meeting_id}/selectDays")
    public ResponseEntity<?> acceptMeeting(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "ID встречи", required = true, example = "12345") @PathVariable("meeting_id") Long meetingId,
            @RequestBody AcceptMeetingRequest request
    ) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            eventService.acceptEvent(user, request, meetingId);
            EventDto updated = eventService.getEventById(meetingId);
            if (request.status().equals(AcceptStatusParticipant.DECLINED)) {
                messagingTemplate.convertAndSend(
                        "/user/" + user.getId() + "/queue/updates",
                        new WebSocketMessage(WebSocketAction.DELETE, updated.id(), null));
            }

            notifyParticipants(updated, WebSocketAction.UPDATE, updated);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @Operation(
            summary = "Фиксация времени встречи",
            description = "Устанавливает окончательное время проведения встречи"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Время успешно зафиксировано"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидные данные",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Нет прав для изменения встречи",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Встреча не найдена",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            )
    })
    @PutMapping("/{meeting_id}/schedule")
    public ResponseEntity<?> scheduleMeeting(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "ID встречи", required = true) @PathVariable("meeting_id") Long meetingId,
            @RequestBody ScheduleRequest request
    ) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            EventDto event = eventService.getEventById(meetingId);
            if (!event.authorId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new AppError(HttpStatus.FORBIDDEN.value(), ErrorMessage.FORBIDDEN.getMessage()));
            }
            EventDto updated = eventService.scheduleEvent(meetingId, request.startTime());
            notifyParticipants(updated, WebSocketAction.SCHEDULE, updated);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        }
    }

    private void notifyParticipants(EventDto event, WebSocketAction action, Object payload) {
        List<Long> participantIds = event.participants().stream()
                .map(p -> p.user().id())
                .toList();
        participantIds.forEach(pid -> messagingTemplate.convertAndSend(
                "/user/" + pid + "/queue/updates",
                new WebSocketMessage(action, event.id(), payload)
        ));
    }
}
