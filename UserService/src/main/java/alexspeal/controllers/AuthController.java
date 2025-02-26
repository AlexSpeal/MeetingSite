package alexspeal.controllers;

import alexspeal.dto.JwtRequest;
import alexspeal.dto.JwtResponse;
import alexspeal.dto.RegistrationRequest;
import alexspeal.dto.RegistrationResponse;
import alexspeal.dto.UserDTO;
import alexspeal.exceptions.AppError;
import alexspeal.service.UserService;
import alexspeal.utils.JwtTokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;
    private final AuthenticationManager authenticationManager;

    @Operation(summary = "user authorization")
    @PostMapping("/auth")
    public ResponseEntity<?> createAuthToken(@RequestBody JwtRequest jwtRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(jwtRequest.getUsername(), jwtRequest.getPassword()));
        } catch (BadCredentialsException e) {
            //todo magic number
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(),
                    "incorrect username or password"), HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = userService.loadUserByUsername(jwtRequest.getUsername());
        String token = jwtTokenUtils.generateToken(userDetails);

        return ResponseEntity.ok(new JwtResponse(token));

    }

    @Operation(summary = "user registration")
    @PostMapping("/signup")
    public ResponseEntity<?> registration(@Validated @RequestBody RegistrationRequest registrationRequest) {

        if (userService.findByUsername(registrationRequest.username()).isPresent()) {
            return new ResponseEntity<>(new AppError(HttpStatus.BAD_REQUEST.value(), "The user with the specified name already exists"), HttpStatus.BAD_REQUEST);
        }

        UserDTO userDTO = new UserDTO(registrationRequest.username(), registrationRequest.password());
        userService.createNewUser(userDTO);

        return ResponseEntity.ok(new RegistrationResponse(userDTO.username()));
    }
}
