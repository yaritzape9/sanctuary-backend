package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.exception.ApiException;
import com.sanctuary.sanctuary_backend.exception.GlobalExceptionHandler;
import com.sanctuary.sanctuary_backend.model.Sighting;
import com.sanctuary.sanctuary_backend.service.SightingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SightingControllerTest {

    @Mock
    private SightingService service;

    private MockMvc mockMvc;

    private final Authentication reporter = new UsernamePasswordAuthenticationToken(
        "real-reporter", null, Collections.emptyList()
    );

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new SightingController(service))
            // Both app-wide advices, as in production (see SightingCreateWebMvcTest)
            .setControllerAdvice(new ApiException(), new GlobalExceptionHandler())
            // Twilio puts an XML converter on the classpath; Boot orders it last, standalone MockMvc doesn't
            .defaultRequest(post("/").accept(MediaType.APPLICATION_JSON))
            .build();
    }

    private String body(String location, String description, Object lat, Object lng) {
        return """
            {"location": %s, "description": %s, "lat": %s, "lng": %s}
            """.formatted(json(location), json(description), lat, lng);
    }

    private String json(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    @Test
    void create_ignoresServerOwnedFieldsInRequestBody() throws Exception {
        when(service.create(any(Sighting.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mass-assignment attempt: every server-owned field set by the client
        String attack = """
            {
              "location": "Main St", "description": "two vans", "lat": 40.7, "lng": -74.0,
              "id": "attacker-chosen-id",
              "status": "confirmed",
              "confirmations": ["a", "b", "c"],
              "removed": true,
              "reportedBy": "victim-user",
              "createdAt": "2000-01-01T00:00:00"
            }
            """;

        mockMvc.perform(post("/api/sightings")
                .principal(reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(attack))
            .andExpect(status().isOk());

        ArgumentCaptor<Sighting> captor = ArgumentCaptor.forClass(Sighting.class);
        verify(service).create(captor.capture());
        Sighting saved = captor.getValue();

        assertThat(saved.getId()).isNull();
        assertThat(saved.getStatus()).isEqualTo("pending");
        assertThat(saved.getConfirmations()).isEmpty();
        assertThat(saved.getRemoved()).isFalse();
        assertThat(saved.getReportedBy()).isEqualTo("real-reporter");
        assertThat(saved.getCreatedAt()).isNull();
        assertThat(saved.getLocation()).isEqualTo("Main St");
        assertThat(saved.getDescription()).isEqualTo("two vans");
        assertThat(saved.getLat()).isEqualTo(40.7);
        assertThat(saved.getLng()).isEqualTo(-74.0);
    }

    @Test
    void create_descriptionOver1000Chars_returns400NotServerError() throws Exception {
        mockMvc.perform(post("/api/sightings")
                .principal(reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Main St", "x".repeat(1001), 40.7, -74.0)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: description"));

        verify(service, never()).create(any());
    }

    @Test
    void create_descriptionAtExactly1000Chars_isAccepted() throws Exception {
        when(service.create(any(Sighting.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/sightings")
                .principal(reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Main St", "x".repeat(1000), 40.7, -74.0)))
            .andExpect(status().isOk());
    }

    @Test
    void create_missingAndBlankFields_returns400ListingThem() throws Exception {
        mockMvc.perform(post("/api/sightings")
                .principal(reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("  ", null, null, null)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: description, lat, lng, location"));

        verify(service, never()).create(any());
    }

    @Test
    void create_coordinatesOutOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/sightings")
                .principal(reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Main St", "two vans", 90.1, -180.1)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: lat, lng"));

        verify(service, never()).create(any());
    }

    @Test
    void create_errorMessageDoesNotEchoSubmittedValues() throws Exception {
        String secret = "x".repeat(990) + "SECRETTEXT";

        mockMvc.perform(post("/api/sightings")
                .principal(reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(secret + "-too-long-for-location".repeat(20), "ok", 40.7, -74.0)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: location"));
    }

    @Test
    void create_malformedJson_returns400NotServerError() throws Exception {
        mockMvc.perform(post("/api/sightings")
                .principal(reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"location\": \"Main St\", \"lat\": \"not-a-number\""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Malformed request body"));

        verify(service, never()).create(any());
    }
}
