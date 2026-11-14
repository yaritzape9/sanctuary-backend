package com.sanctuary.sanctuary_backend.controller;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.sanctuary.sanctuary_backend.dto.PanicResult;
import com.sanctuary.sanctuary_backend.dto.PanicTriggerRequest;
import com.sanctuary.sanctuary_backend.service.PanicService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PanicControllerTest {

    @Mock
    private PanicService panicService;

    @InjectMocks
    private PanicController panicController;

    @BeforeEach
    void stubService() {
        lenient().when(panicService.triggerAlert(anyString(), any(), any())).thenReturn(new PanicResult(1, 0));
        lenient().when(panicService.sendAllClear(anyString())).thenReturn(new PanicResult(1, 0));
    }

    @Test
    void trigger_usesAuthenticatedUserId_notRequestBody() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "real-authenticated-user", null, Collections.emptyList()
        );
        PanicTriggerRequest request = new PanicTriggerRequest(
            DoubleNode.valueOf(37.7749), DoubleNode.valueOf(-122.4194));

        ResponseEntity<PanicResult> response = panicController.trigger(request, authentication);

        verify(panicService).triggerAlert("real-authenticated-user", 37.7749, -122.4194);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(new PanicResult(1, 0));
    }

    @Test
    void trigger_cannotBeSpoofedViaRequestBody() {
        // Regression guard: PanicTriggerRequest has no userId field at all,
        // so there is no way for a client to supply one. This test documents
        // that guarantee by confirming the service is called with the
        // authenticated principal regardless of what's in the request.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "victim-user-id", null, Collections.emptyList()
        );
        PanicTriggerRequest request = new PanicTriggerRequest(DoubleNode.valueOf(1.0), DoubleNode.valueOf(2.0));

        panicController.trigger(request, authentication);

        verify(panicService).triggerAlert("victim-user-id", 1.0, 2.0);
        verify(panicService, never()).triggerAlert("attacker-user-id", 1.0, 2.0);
    }

    @Test
    void safe_usesAuthenticatedUserId() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "real-authenticated-user", null, Collections.emptyList()
        );

        ResponseEntity<PanicResult> response = panicController.safe(authentication);

        verify(panicService).sendAllClear("real-authenticated-user");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(new PanicResult(1, 0));
    }
}