package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.exception.ApiException;
import com.sanctuary.sanctuary_backend.exception.EmailAlreadyRegisteredException;
import com.sanctuary.sanctuary_backend.exception.GlobalExceptionHandler;
import com.sanctuary.sanctuary_backend.exception.InvalidGoogleTokenException;
import com.sanctuary.sanctuary_backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Register both app-wide advices so the test proves the controller's own handlers win over them
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new ApiException(), new GlobalExceptionHandler())
            // Twilio puts an XML converter on the classpath; Boot orders it last, standalone MockMvc doesn't
            .defaultRequest(post("/").accept(MediaType.APPLICATION_JSON))
            .build();
    }

    private void postOauthSync(String body, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/auth/oauth-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().is(expectedStatus));
    }

    @Test
    void oauthSync_passesOnlyTheIdToken_ignoringClientIdentityFields() throws Exception {
        when(authService.oauthSync("google-id-token")).thenReturn(Map.of("userId", "u1", "token", "t"));

        postOauthSync("{\"idToken\":\"google-id-token\",\"email\":\"victim@test.com\",\"googleId\":\"victim-google\"}", 200);

        verify(authService).oauthSync("google-id-token");
    }

    @Test
    void oauthSync_returns401_whenGoogleTokenIsInvalid() throws Exception {
        when(authService.oauthSync(any())).thenThrow(new InvalidGoogleTokenException());

        mockMvc.perform(post("/api/auth/oauth-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"forged\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Google sign-in could not be verified"));
    }

    @Test
    void oauthSync_returns409_whenEmailHasPasswordAccount() throws Exception {
        when(authService.oauthSync(any())).thenThrow(new EmailAlreadyRegisteredException());

        postOauthSync("{\"idToken\":\"valid\"}", 409);
    }

    @Test
    void oauthSync_returns401_whenIdTokenIsMissing() throws Exception {
        when(authService.oauthSync(null)).thenThrow(new InvalidGoogleTokenException());

        postOauthSync("{\"email\":\"victim@test.com\",\"googleId\":\"victim-google\"}", 401);
    }
}
