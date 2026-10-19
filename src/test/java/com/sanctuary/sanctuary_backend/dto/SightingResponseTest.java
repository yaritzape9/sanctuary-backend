package com.sanctuary.sanctuary_backend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanctuary.sanctuary_backend.model.Sighting;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Every REST response and WebSocket broadcast goes through SightingResponse.from(),
// so this guards all of them at once
class SightingResponseTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private Sighting sighting() {
        Sighting s = new Sighting();
        s.setId("sighting-1");
        s.setLocation("Main St");
        s.setDescription("two vans");
        s.setLat(40.7);
        s.setLng(-74.0);
        s.setStatus("pending");
        s.setCreatedAt(LocalDateTime.of(2026, 10, 19, 12, 0));
        s.setReportedBy("reporter-user-id");
        s.setConfirmations(new ArrayList<>(List.of("confirmer-user-id")));
        return s;
    }

    @Test
    void from_jsonHasNoReporterOrConfirmerIdentity() throws Exception {
        String json = mapper.writeValueAsString(SightingResponse.from(sighting()));

        assertThat(json)
            .doesNotContain("reportedBy")
            .doesNotContain("reporter-user-id")
            .doesNotContain("confirmations")
            .doesNotContain("confirmer-user-id")
            .doesNotContain("removed");
    }

    @Test
    void from_jsonExposesOnlyPublicFields() throws Exception {
        String json = mapper.writeValueAsString(SightingResponse.from(sighting()));

        // A new field on SightingResponse fails here until someone decides it's safe to publish
        assertThat(mapper.readTree(json).fieldNames())
            .toIterable()
            .containsExactlyInAnyOrder("id", "location", "description", "lat", "lng", "status", "createdAt");
    }
}
