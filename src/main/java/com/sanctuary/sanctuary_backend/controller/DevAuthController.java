package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.service.DevAuthService;
import com.sanctuary.sanctuary_backend.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/dev")
@Profile("dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final DevAuthService devAuthService;

    @PostMapping("/token")
    public ResponseEntity<?> getToken(@Valid @RequestBody TokenRequest request) {
        try {
            String token = devAuthService.mintTokenForEmail(request.email());
            return ResponseEntity.ok(new TokenResponse(token));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/promote")
    public ResponseEntity<?> promote(@Valid @RequestBody TokenRequest request) {
        try {
            User user = devAuthService.promoteToDev(request.email());
            return ResponseEntity.ok(new PromoteResponse(user.getEmail(), user.getRole().name()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Data
    static class PromoteResponse {
        private final String email;
        private final String role;
    }

    // A record, not @Data: Lombok doesn't reliably process inner static classes
    record TokenRequest(@NotBlank @Email String email) {
    }

    @Data
    static class TokenResponse {
        private final String token;
    }
}