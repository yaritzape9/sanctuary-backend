package com.sanctuary.sanctuary_backend.dto;

import com.sanctuary.sanctuary_backend.model.Sighting;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// The only fields a client may send when reporting a sighting.
// id, status, confirmations, removed, reportedBy and createdAt are owned by
// the server; unknown JSON fields are dropped, so they can't be set here.
public record CreateSightingRequest(
    @NotBlank @Size(max = 255) String location,
    @NotBlank @Size(max = 1000) String description,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng
) {

    public Sighting toSighting(String reportedBy) {
        Sighting s = new Sighting();
        s.setLocation(location);
        s.setDescription(description);
        s.setLat(lat);
        s.setLng(lng);
        s.setReportedBy(reportedBy);
        return s;
    }
}
