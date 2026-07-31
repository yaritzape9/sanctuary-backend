package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.service.DevAuthService;
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
    public ResponseEntity<?> getToken(@RequestBody TokenRequest request) {
        try {
            String token = devAuthService.mintTokenForEmail(request.getEmail());
            return ResponseEntity.ok(new TokenResponse(token));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Data
    static class TokenRequest {
        private String email;
    }

    @Data
    static class TokenResponse {
        private final String token;
    }
}