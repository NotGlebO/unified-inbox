
package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.entity.UserEntity;
import com.unifiedinbox.unified_inbox.service.AuthService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record RegisterRequest(
            @NotBlank
            String username,

            @NotBlank
            @Size(min = 8)
            String password
    ) {}

    public record UserResponse(
            Long id,
            String username
    ) {}

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        try {
            UserEntity user = authService.register(
                    request.username(),
                    request.password()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new UserResponse(
                            user.getId(),
                            user.getUsername()
                    ));

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }
}
