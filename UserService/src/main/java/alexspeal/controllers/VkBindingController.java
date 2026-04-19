package alexspeal.controllers;

import alexspeal.dto.requests.ConfirmVkBindingRequest;
import alexspeal.dto.requests.StartVkBindingRequest;
import alexspeal.entities.UserEntity;
import alexspeal.exceptions.AppError;
import alexspeal.service.VkBindingService;
import alexspeal.utils.JwtIdentificationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/secured/vk")
@RequiredArgsConstructor
public class VkBindingController {

    private final VkBindingService vkBindingService;
    private final JwtIdentificationUtils jwtIdentificationUtils;

    @PostMapping("/bind/start")
    public ResponseEntity<?> startBinding(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody StartVkBindingRequest request
    ) {
        try {
            UserEntity user = jwtIdentificationUtils.getUserFromHeader(authHeader);
            vkBindingService.startBinding(user, request);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
        }
    }

    @PostMapping("/bind/confirm")
    public ResponseEntity<?> confirmBinding(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ConfirmVkBindingRequest request
    ) {
        try {
            UserEntity user = jwtIdentificationUtils.getUserFromHeader(authHeader);
            vkBindingService.confirmBinding(user, request);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
        }
    }

    @PostMapping("/bind/disable")
    public ResponseEntity<?> disableBinding(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            UserEntity user = jwtIdentificationUtils.getUserFromHeader(authHeader);
            vkBindingService.disableBinding(user);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
        }
    }
}