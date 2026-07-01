package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.dto.SightingResponse;
import com.sanctuary.sanctuary_backend.model.Sighting;
import com.sanctuary.sanctuary_backend.service.SightingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sightings")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class SightingController {

    private final SightingService service;

    @GetMapping
    public List<SightingResponse> getAll() {
        return service.getAll().stream()
            .map(SightingResponse::from)
            .toList();
    }

    @PostMapping
    public ResponseEntity<SightingResponse> create(
        @RequestBody Sighting sighting,
        Authentication authentication
    ) {
        sighting.setReportedBy(authentication.getName());
        return ResponseEntity.ok(SightingResponse.from(service.create(sighting)));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<SightingResponse> confirm(
        @PathVariable String id,
        Authentication authentication
    ) {
        // userId comes from the validated JWT, never from the request body
        String userId = authentication.getName();
        return ResponseEntity.ok(SightingResponse.from(service.confirm(id, userId)));
    }
}