package com.sanctuary.sanctuary_backend.dto;

import com.sanctuary.sanctuary_backend.model.Sighting;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SightingResponse {

    private String id;
    private String location;
    private String description;
    private Double lat;
    private Double lng;
    private String status;
    private LocalDateTime createdAt;
    // confirmations intentionally excluded — exposes threshold if visible
    // reportedBy intentionally excluded — GET /api/sightings is public, so it would link reports to accounts

    public static SightingResponse from(Sighting s) {
        SightingResponse dto = new SightingResponse();
        dto.setId(s.getId());
        dto.setLocation(s.getLocation());
        dto.setDescription(s.getDescription());
        dto.setLat(s.getLat());
        dto.setLng(s.getLng());
        dto.setStatus(s.getStatus());
        dto.setCreatedAt(s.getCreatedAt());
        return dto;
    }
}