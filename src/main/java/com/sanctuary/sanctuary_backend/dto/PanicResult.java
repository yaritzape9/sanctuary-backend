package com.sanctuary.sanctuary_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * What happened when a panic or all-clear SMS went out, returned as the response body.
 * Only counts: never contact IDs, names or phone numbers.
 */
public record PanicResult(int sent, int failed) {

    public static final PanicResult NO_CONTACTS = new PanicResult(0, 0);

    /** "no_contacts", "sent" (every text went out), "partial" (some failed) or "failed" (none went out). */
    @JsonProperty("status")
    public String status() {
        if (sent == 0 && failed == 0) {
            return "no_contacts";
        }
        if (failed == 0) {
            return "sent";
        }
        return sent > 0 ? "partial" : "failed";
    }
}
