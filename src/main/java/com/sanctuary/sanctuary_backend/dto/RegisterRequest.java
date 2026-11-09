package com.sanctuary.sanctuary_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// The only fields a client may send when signing up; unknown JSON fields are dropped.
// Max 72 because bcrypt only uses the first 72 bytes of a password.
public record RegisterRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotNull @Size(min = 12, max = 72) String password
) {

    // The generated toString would print the password if this were ever logged
    @Override
    public String toString() {
        return "RegisterRequest[email=<hidden>, password=<hidden>]";
    }
}
