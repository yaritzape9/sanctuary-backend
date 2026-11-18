package com.sanctuary.sanctuary_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Only the /ws WebSocket is called straight from the browser; REST goes through the
 * Next.js server proxies. So REST sends no CORS headers, and /ws allows only the
 * origins in sanctuary.allowed-origins (ALLOWED_ORIGINS, default http://localhost:3000).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsTest {

    private static final String FRONTEND = "http://localhost:3000";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void restApiSendsNoCorsHeadersEvenToTheFrontendOrigin() throws Exception {
        mockMvc.perform(get("/api/sightings").header("Origin", FRONTEND))
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void webSocketHandshakeAllowsTheConfiguredOrigin() throws Exception {
        mockMvc.perform(get("/ws/info").header("Origin", FRONTEND))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND));
    }

    @Test
    void webSocketHandshakeRejectsAnyOtherOrigin() throws Exception {
        mockMvc.perform(get("/ws/info").header("Origin", "https://evil.example"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
