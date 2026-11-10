package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.config.SecurityConfig;
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

/** Body validation for POST /api/auth/login and /api/auth/oauth-sync through the real MVC stack. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtUtil.class})
@TestPropertySource(properties = "jwt.secret=auth-login-test-secret-long-enough-for-hs256")
class AuthLoginWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private MockHttpServletRequestBuilder postJson(String path, String body) {
        return post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(body);
    }

    // ── login ──────────────────────────────────────────────────────────────

    @Test
    void login_validBody_reachesService() throws Exception {
        when(authService.login("user@example.com", "short")).thenReturn(Map.of("token", "jwt"));

        // No 12-character minimum on login: a short password is just a wrong one (401 from the service)
        mockMvc.perform(postJson("/api/auth/login", "{\"email\":\"user@example.com\",\"password\":\"short\"}"))
            .andExpect(status().isOk());

        verify(authService).login("user@example.com", "short");
    }

    @Test
    void login_missingFields_returns400NamingBoth() throws Exception {
        mockMvc.perform(postJson("/api/auth/login", "{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: email, password"));

        verify(authService, never()).login(any(), any());
    }

    @Test
    void login_invalidEmail_returns400WithoutEchoingIt() throws Exception {
        mockMvc.perform(postJson("/api/auth/login", "{\"email\":\"PRIVATE-not-an-email\",\"password\":\"pw\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: email"))
            .andExpect(content().string(not(containsString("PRIVATE-not-an-email"))));

        verify(authService, never()).login(any(), any());
    }

    @Test
    void login_passwordOver72Characters_returns400WithoutEchoingIt() throws Exception {
        String tooLong = "S".repeat(73);
        mockMvc.perform(postJson("/api/auth/login", "{\"email\":\"user@example.com\",\"password\":\"" + tooLong + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: password"))
            .andExpect(content().string(not(containsString(tooLong))));

        verify(authService, never()).login(any(), any());
    }

    // ── oauth-sync ─────────────────────────────────────────────────────────

    @Test
    void oauthSync_blankIdToken_returns400() throws Exception {
        mockMvc.perform(postJson("/api/auth/oauth-sync", "{\"idToken\":\"  \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: idToken"));

        verify(authService, never()).oauthSync(any());
    }

    @Test
    void oauthSync_oversizedIdToken_returns400WithoutEchoingIt() throws Exception {
        String huge = "t".repeat(4097);
        mockMvc.perform(postJson("/api/auth/oauth-sync", "{\"idToken\":\"" + huge + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: idToken"))
            .andExpect(content().string(not(containsString(huge))));

        verify(authService, never()).oauthSync(any());
    }

    @Test
    void oauthSync_tokenOf4096Characters_reachesService() throws Exception {
        String longest = "t".repeat(4096);
        when(authService.oauthSync(longest)).thenReturn(Map.of("token", "jwt"));

        mockMvc.perform(postJson("/api/auth/oauth-sync", "{\"idToken\":\"" + longest + "\"}"))
            .andExpect(status().isOk());
    }
}
