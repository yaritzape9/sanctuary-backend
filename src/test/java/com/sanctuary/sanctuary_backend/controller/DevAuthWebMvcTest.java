package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.config.SecurityConfig;
import com.sanctuary.sanctuary_backend.service.DevAuthService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The dev-only endpoints validate their body too (they only exist with the dev profile). */
@WebMvcTest(DevAuthController.class)
@ActiveProfiles("dev")
@Import({SecurityConfig.class, JwtUtil.class})
@TestPropertySource(properties = "jwt.secret=dev-auth-test-secret-long-enough-for-hs256")
class DevAuthWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DevAuthService devAuthService;

    @ParameterizedTest
    @ValueSource(strings = {"/api/dev/token", "/api/dev/promote"})
    void blankOrInvalidEmail_returns400(String path) throws Exception {
        for (String body : new String[] {"{}", "{\"email\":\"  \"}", "{\"email\":\"not-an-email\"}"}) {
            mockMvc.perform(post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid fields: email"));
        }

        verifyNoInteractions(devAuthService);
    }
}
