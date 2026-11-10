package com.sanctuary.sanctuary_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Only Google's signed ID token is accepted; email/googleId in the body are dropped.
// Real tokens are about 1-2 KB, so 4096 only stops oversized junk.
public record OAuthSyncRequest(
    @NotBlank @Size(max = 4096) String idToken
) {

    @Override
    public String toString() {
        return "OAuthSyncRequest[idToken=<hidden>]";
    }
}
