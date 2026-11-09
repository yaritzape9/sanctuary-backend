package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.dto.RegisterRequest;
import com.sanctuary.sanctuary_backend.exception.EmailAlreadyRegisteredException;
import com.sanctuary.sanctuary_backend.exception.InvalidCredentialsException;
import com.sanctuary.sanctuary_backend.exception.InvalidGoogleTokenException;
import com.sanctuary.sanctuary_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request.email(), request.password()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.login(
            body.get("email"),
            body.get("password")
        ));
    }
    // Only the Google ID token is read; email/googleId in the body are ignored
    @PostMapping("/oauth-sync")
    public ResponseEntity<Map<String, String>> oauthSync(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.oauthSync(body.get("idToken")));
    }

    // Handled here so they can't be swallowed by the generic RuntimeException advice
    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidGoogleToken(InvalidGoogleTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}