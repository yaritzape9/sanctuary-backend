package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.config.SecurityConfig;
import com.sanctuary.sanctuary_backend.exception.EmailAlreadyRegisteredException;
import com.sanctuary.sanctuary_backend.exception.InvalidCredentialsException;
import com.sanctuary.sanctuary_backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/auth/register through the real MVC stack: security filter, Jackson config and
 * every @RestControllerAdvice the app registers, so the status codes hold when they compete.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtUtil.class})
@TestPropertySource(properties = "jwt.secret=auth-register-test-secret-long-enough-for-hs256")
class AuthRegisterWebMvcTest {

    private static final String VALID_PASSWORD = "x".repeat(12);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private MockHttpServletRequestBuilder register(String body) {
        return post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(body);
    }

    private static String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void validSignup_returnsTokenWithoutAuthHeader() throws Exception {
        when(authService.register("new@example.com", VALID_PASSWORD))
            .thenReturn(Map.of("userId", "user-1", "token", "jwt"));

        mockMvc.perform(register(body("new@example.com", VALID_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt"));
    }

    @Test
    void passwordOfExactly72Characters_isAccepted() throws Exception {
        String longest = "x".repeat(72);
        when(authService.register("new@example.com", longest))
            .thenReturn(Map.of("userId", "user-1", "token", "jwt"));

        mockMvc.perform(register(body("new@example.com", longest)))
            .andExpect(status().isOk());
    }

    @Test
    void passwordOf11Characters_returns400WithoutEchoingIt() throws Exception {
        mockMvc.perform(register(body("new@example.com", "SECRET-PASS")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: password"))
            .andExpect(content().string(not(containsString("SECRET-PASS"))));

        verify(authService, never()).register(any(), any());
    }

    @Test
    void passwordOver72Characters_returns400() throws Exception {
        mockMvc.perform(register(body("new@example.com", "x".repeat(73))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: password"));

        verify(authService, never()).register(any(), any());
    }

    @Test
    void invalidEmail_returns400WithoutEchoingIt() throws Exception {
        mockMvc.perform(register(body("not-an-email", VALID_PASSWORD)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: email"))
            .andExpect(content().string(not(containsString("not-an-email"))));

        verify(authService, never()).register(any(), any());
    }

    @Test
    void missingFields_returns400NamingBoth() throws Exception {
        mockMvc.perform(register("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: email, password"));

        verify(authService, never()).register(any(), any());
    }

    @Test
    void duplicateEmail_returns409() throws Exception {
        when(authService.register("taken@example.com", VALID_PASSWORD))
            .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(register(body("taken@example.com", VALID_PASSWORD)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("An account with this email already exists."));
    }

    @Test
    void extraFieldsAreIgnored_soARoleCannotBeRequested() throws Exception {
        when(authService.register("new@example.com", VALID_PASSWORD))
            .thenReturn(Map.of("userId", "user-1", "token", "jwt"));

        mockMvc.perform(register("""
                {"email": "new@example.com", "password": "%s", "role": "ADMIN", "id": "chosen-id"}
                """.formatted(VALID_PASSWORD)))
            .andExpect(status().isOk());

        verify(authService).register("new@example.com", VALID_PASSWORD);
    }

    @Test
    void wrongLogin_returns401NotServerError() throws Exception {
        when(authService.login("user@example.com", "wrong-password"))
            .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body("user@example.com", "wrong-password")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Invalid credentials"))
            .andExpect(content().string(not(containsString("wrong-password"))));
    }

    @Test
    void malformedJson_returns400WithoutEchoingInput() throws Exception {
        mockMvc.perform(register("{\"email\": \"PRIVATE@example.com\", \"password\": oops"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Malformed request body"))
            .andExpect(content().string(not(containsString("PRIVATE@example.com"))));

        verify(authService, never()).register(any(), any());
    }
}
