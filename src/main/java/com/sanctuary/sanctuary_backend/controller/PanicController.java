package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.dto.PanicTriggerRequest;
import com.sanctuary.sanctuary_backend.service.PanicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panic")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class PanicController {

    private final PanicService panicService;

    // The body is optional and a bad location never rejects the request: the alert
    // always goes out, saying the location couldn't be shared when it's missing or invalid.
    @PostMapping("/trigger")
    public ResponseEntity<String> trigger(
        @RequestBody(required = false) PanicTriggerRequest request,
        Authentication authentication
    ) {
        String userId = authentication.getName();
        PanicTriggerRequest body = request == null ? PanicTriggerRequest.NO_LOCATION : request;
        panicService.triggerAlert(userId, body.validLat(), body.validLng());
        return ResponseEntity.ok("Alert sent");
    }

    @PostMapping("/safe")
    public ResponseEntity<String> safe(Authentication authentication) {
        String userId = authentication.getName();
        panicService.sendAllClear(userId);
        return ResponseEntity.ok("All clear sent");
    }
}
