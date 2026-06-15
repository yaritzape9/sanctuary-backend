package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.model.Sighting;
import com.sanctuary.sanctuary_backend.service.SightingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sightings")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class SightingController {

    private final SightingService service;

    @GetMapping
    public List<Sighting> getAll() {
        return service.getAll();
    }

    @PostMapping
    public ResponseEntity<Sighting> create(@RequestBody Sighting sighting) {
        return ResponseEntity.ok(service.create(sighting));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Sighting> confirm(
        @PathVariable String id,
        @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(service.confirm(id, body.get("userId")));
    }
}