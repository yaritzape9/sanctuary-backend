package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.dto.PanicResult;
import com.sanctuary.sanctuary_backend.dto.PanicTriggerRequest;
import com.sanctuary.sanctuary_backend.service.PanicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<PanicResult> trigger(
        @RequestBody(required = false) PanicTriggerRequest request,
        Authentication authentication
    ) {
        String userId = authentication.getName();
        PanicTriggerRequest body = request == null ? PanicTriggerRequest.NO_LOCATION : request;
        return toResponse(panicService.triggerAlert(userId, body.validLat(), body.validLng()));
    }

    @PostMapping("/safe")
    public ResponseEntity<PanicResult> safe(Authentication authentication) {
        String userId = authentication.getName();
        return toResponse(panicService.sendAllClear(userId));
    }

    // 200 when at least one text went out (a partial send is not retried, so nobody gets
    // it twice), 502 when none did (safe to retry), 422 when there is nobody to text.
    private static ResponseEntity<PanicResult> toResponse(PanicResult result) {
        HttpStatus status = switch (result.status()) {
            case "no_contacts" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "failed" -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(result);
    }
}
