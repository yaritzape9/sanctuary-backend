package com.sanctuary.sanctuary_backend.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleTokenVerifierTest {

    private final GoogleIdTokenVerifier googleVerifier = mock(GoogleIdTokenVerifier.class);
    private final GoogleTokenVerifier verifier = new GoogleTokenVerifier(googleVerifier);

    private GoogleIdToken tokenWith(Boolean emailVerified) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-123");
        payload.setEmail("user@test.com");
        payload.setEmailVerified(emailVerified);
        return new GoogleIdToken(new JsonWebSignature.Header(), payload, new byte[0], new byte[0]);
    }

    @Test
    void returnsIdentity_forValidTokenWithVerifiedEmail() throws Exception {
        when(googleVerifier.verify("valid")).thenReturn(tokenWith(true));

        assertThat(verifier.verify("valid"))
            .contains(new GoogleTokenVerifier.GoogleIdentity("google-123", "user@test.com"));
    }

    @Test
    void rejects_whenGoogleSaysEmailIsNotVerified() throws Exception {
        when(googleVerifier.verify("unverified")).thenReturn(tokenWith(false));
        assertThat(verifier.verify("unverified")).isEmpty();
    }

    @Test
    void rejects_whenEmailVerifiedClaimIsMissing() throws Exception {
        when(googleVerifier.verify("no-claim")).thenReturn(tokenWith(null));
        assertThat(verifier.verify("no-claim")).isEmpty();
    }

    @Test
    void rejects_whenSignatureAudienceOrExpiryFails() throws Exception {
        // GoogleIdTokenVerifier returns null when any of those checks fail
        when(googleVerifier.verify("bad")).thenReturn(null);
        assertThat(verifier.verify("bad")).isEmpty();
    }

    @Test
    void rejects_whenVerificationThrows() throws Exception {
        when(googleVerifier.verify("boom")).thenThrow(new GeneralSecurityException("certs unavailable"));
        assertThat(verifier.verify("boom")).isEmpty();
    }

    @Test
    void rejects_nullOrBlankToken_withoutCallingGoogle() throws Exception {
        assertThat(verifier.verify(null)).isEmpty();
        assertThat(verifier.verify("  ")).isEmpty();
        verify(googleVerifier, never()).verify(any(String.class));
    }

    @Test
    void rejectsEverything_whenClientIdIsNotConfigured() {
        assertThat(new GoogleTokenVerifier("").verify("anything")).isEmpty();
    }

    @Test
    void realVerifier_rejectsMalformedToken_withoutNetwork() {
        assertThat(new GoogleTokenVerifier("test-client-id").verify("not-a-jwt")).isEmpty();
    }
}
