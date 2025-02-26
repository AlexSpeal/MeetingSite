package alexspeal.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class MainController {
    @Operation(summary = "unsecure connect")
    @GetMapping("/unsecured")
    public String unsecuredData() {
        return "unsecured";
    }

    @Operation(summary = "secure connect")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/secured")
    public String securedData() {
        return "secured";
    }

    @Operation(summary = "get username")
    @GetMapping("/info")
    public String userData(Principal principal) {
        return principal.getName();
    }
}
