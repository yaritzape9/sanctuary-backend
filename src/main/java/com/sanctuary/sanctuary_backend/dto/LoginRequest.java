package com.sanctuary.sanctuary_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No 12-character minimum here: a short password is simply a wrong one (401).
// Max 72 matches RegisterRequest, since no stored password can be longer.
public record LoginRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 72) String password
) {

    @Override
    public String toString() {
        return "LoginRequest[email=<hidden>, password=<hidden>]";
    }
}
