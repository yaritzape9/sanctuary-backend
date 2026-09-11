package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.service.PanicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PanicControllerTest {

    @Mock
    private PanicService panicService;

    @InjectMocks
    private PanicController panicController;

    @Test
    void trigger_usesAuthenticatedUserId_notRequestBody() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "real-authenticated-user", null, Collections.emptyList()
        );
        PanicController.TriggerRequest request = new PanicController.TriggerRequest();
        request.setLat(37.7749);
        request.setLng(-122.4194);

        ResponseEntity<String> response = panicController.trigger(request, authentication);

        verify(panicService).triggerAlert("real-authenticated-user", 37.7749, -122.4194);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Alert sent");
    }

    @Test
    void trigger_cannotBeSpoofedViaRequestBody() {
        // Regression guard: TriggerRequest no longer has a userId field at all,
        // so there is no way for a client to supply one. This test documents
        // that guarantee by confirming the service is called with the
        // authenticated principal regardless of what's in the request.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "victim-user-id", null, Collections.emptyList()
        );
        PanicController.TriggerRequest request = new PanicController.TriggerRequest();
        request.setLat(1.0);
        request.setLng(2.0);

        panicController.trigger(request, authentication);

        verify(panicService).triggerAlert("victim-user-id", 1.0, 2.0);
        verify(panicService, never()).triggerAlert("attacker-user-id", 1.0, 2.0);
    }

    @Test
    void safe_usesAuthenticatedUserId() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "real-authenticated-user", null, Collections.emptyList()
        );

        ResponseEntity<String> response = panicController.safe(authentication);

        verify(panicService).sendAllClear("real-authenticated-user");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("All clear sent");
    }
}