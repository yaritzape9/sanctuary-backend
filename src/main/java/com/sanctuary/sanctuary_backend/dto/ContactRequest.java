package com.sanctuary.sanctuary_backend.dto;

import com.sanctuary.sanctuary_backend.model.Relationship;
import com.sanctuary.sanctuary_backend.util.UsPhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Body for POST /api/contacts and PUT /api/contacts/{contactId}. PUT replaces the whole
// contact, so both use the same rules. US numbers only for now.
public record ContactRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @Pattern(regexp = UsPhoneNumber.E164_US_REGEX) String phone,
    @NotNull Relationship relationship
) {

    // Runs before validation, so the pattern checks the normalized +1XXXXXXXXXX form
    public ContactRequest {
        phone = UsPhoneNumber.normalize(phone);
    }

    // The generated toString would print the contact's number if this were ever logged
    @Override
    public String toString() {
        return "ContactRequest[name=<hidden>, phone=<hidden>, relationship=" + relationship + "]";
    }
}
