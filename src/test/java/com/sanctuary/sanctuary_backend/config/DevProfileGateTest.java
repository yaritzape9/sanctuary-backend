package com.sanctuary.sanctuary_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DevProfileGateTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devTokenEndpoint_notAvailable_whenDevProfileNotActive() throws Exception {
        mockMvc.perform(post("/api/dev/token")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\"}"))
            .andExpect(status().isNotFound());
    }
}