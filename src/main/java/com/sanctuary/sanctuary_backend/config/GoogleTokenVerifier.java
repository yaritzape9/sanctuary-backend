package com.sanctuary.sanctuary_backend.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Checks a Google ID token: Google's signature, issuer, expiry, audience (our client ID)
 * and that Google has verified the email. Identity for oauth-sync comes only from here,
 * never from fields the client sends.
 */
@Component
@Slf4j
public class GoogleTokenVerifier {

    public record GoogleIdentity(String googleId, String email) {}

    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleTokenVerifier(@Value("${google.client-id:}") String clientId) {
        this(clientId.isBlank() ? null : new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(List.of(clientId))
            .build());
        if (clientId.isBlank()) {
            log.warn("google.client-id is not set; every Google sign-in will be rejected");
        }
    }

    // Package-private so tests can supply a stubbed verifier without network calls
    GoogleTokenVerifier(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    public Optional<GoogleIdentity> verify(String idToken) {
        if (verifier == null || idToken == null || idToken.isBlank()) {
            return Optional.empty();
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                return Optional.empty();
            }
            GoogleIdToken.Payload payload = token.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                return Optional.empty();
            }
            return Optional.of(new GoogleIdentity(payload.getSubject(), payload.getEmail()));
        } catch (Exception e) {
            // Malformed token, bad signature or Google certs unreachable: reject, and don't log the token
            log.warn("Google ID token rejected: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
