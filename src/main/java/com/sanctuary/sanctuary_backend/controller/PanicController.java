package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.service.PanicService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panic")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class PanicController {

    private final PanicService panicService;

    @PostMapping("/trigger")
    public ResponseEntity<String> trigger(@RequestBody TriggerRequest request) {
        panicService.triggerAlert(request.getUserId(), request.getLat(), request.getLng());
        return ResponseEntity.ok("Alert sent");
    }

    @PostMapping("/safe")
    public ResponseEntity<String> safe(@RequestBody SafeRequest request) {
        panicService.sendAllClear(request.getUserId());
        return ResponseEntity.ok("All clear sent");
    }

    @Data
    static class TriggerRequest {
        private String userId;
        private Double lat;
        private Double lng;
    }

    @Data
    static class SafeRequest {
        private String userId;
    }
}