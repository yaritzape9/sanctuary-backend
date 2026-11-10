package com.sanctuary.sanctuary_backend.util;

import java.util.regex.Pattern;

/**
 * US-only phone numbers (for now). Accepts the formats people type, such as
 * "(212) 555-0100", "212.555.0100" or "+1 212 555 0100", and turns them into
 * E.164 ("+12125550100"), the one format we store and that Twilio expects.
 */
public final class UsPhoneNumber {

    /**
     * +1, then a 10-digit US number whose area code and exchange both start with 2 to 9
     * (North American Numbering Plan rules).
     */
    public static final String E164_US_REGEX = "^\\+1[2-9]\\d{2}[2-9]\\d{6}$";

    private static final Pattern E164_US = Pattern.compile(E164_US_REGEX);

    // Digits plus the separators people type; a "+" only at the start
    private static final Pattern TYPED_FORMAT = Pattern.compile("^\\+?[0-9 ().-]+$");

    private UsPhoneNumber() {
    }

    /**
     * Returns the number as +1XXXXXXXXXX, or the input unchanged when it isn't a valid US
     * number, so that validation against {@link #E164_US_REGEX} rejects it.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.strip();
        if (!TYPED_FORMAT.matcher(trimmed).matches()) {
            return raw;
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (trimmed.startsWith("+") && !digits.startsWith("1")) {
            return raw; // another country code
        }
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }
        if (digits.length() != 10) {
            return raw;
        }

        String e164 = "+1" + digits;
        return E164_US.matcher(e164).matches() ? e164 : raw;
    }
}
