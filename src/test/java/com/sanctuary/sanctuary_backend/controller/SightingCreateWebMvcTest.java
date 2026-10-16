package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.config.SecurityConfig;
import com.sanctuary.sanctuary_backend.model.Role;
import com.sanctuary.sanctuary_backend.model.Sighting;
import com.sanctuary.sanctuary_backend.service.SightingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/sightings through the real MVC stack: security filter, Jackson config and
 * every @RestControllerAdvice the app registers. SightingControllerTest covers the
 * controller alone; this proves the same behavior holds when all advices compete.
 */
@WebMvcTest(SightingController.class)
@Import({SecurityConfig.class, JwtUtil.class})
@TestPropertySource(properties = "jwt.secret=sighting-create-test-secret-long-enough-for-hs256")
class SightingCreateWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private SightingService service;

    private MockHttpServletRequestBuilder createAs(String userId, String body) {
        return post("/api/sightings")
            .header("Authorization", "Bearer " + jwtUtil.generateToken(userId, Role.USER))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(body);
    }

    @Test
    void serverOwnedFieldsInBodyNeverReachTheService() throws Exception {
        when(service.create(any(Sighting.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(createAs("real-reporter", """
                {"location": "Main St", "description": "two vans", "lat": 40.7, "lng": -74.0,
                 "id": "attacker-id", "status": "confirmed", "confirmations": ["a", "b", "c"],
                 "removed": true, "reportedBy": "victim-user"}
                """))
            .andExpect(status().isOk());

        ArgumentCaptor<Sighting> captor = ArgumentCaptor.forClass(Sighting.class);
        verify(service).create(captor.capture());
        Sighting saved = captor.getValue();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getStatus()).isEqualTo("pending");
        assertThat(saved.getConfirmations()).isEmpty();
        assertThat(saved.getRemoved()).isFalse();
        assertThat(saved.getReportedBy()).isEqualTo("real-reporter");
    }

    @Test
    void tooLongDescription_returns400NotServerError() throws Exception {
        mockMvc.perform(createAs("real-reporter",
                "{\"location\":\"Main St\",\"description\":\"" + "x".repeat(1001) + "\",\"lat\":1.0,\"lng\":2.0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: description"));

        verify(service, never()).create(any());
    }

    @Test
    void malformedJson_returns400WithoutEchoingInput() throws Exception {
        mockMvc.perform(createAs("real-reporter", "{\"location\": \"PRIVATE-ADDRESS\", \"lat\": oops"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Malformed request body"))
            .andExpect(content().string(not(containsString("oops"))))
            .andExpect(content().string(not(containsString("PRIVATE-ADDRESS"))));

        verify(service, never()).create(any());
    }
}
