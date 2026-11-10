package com.sanctuary.sanctuary_backend.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UsPhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "+12125550100",
        "+1 212 555 0100",
        "+1 (212) 555-0100",
        "(212) 555-0100",
        "212-555-0100",
        "212.555.0100",
        "2125550100",
        "12125550100",
        "1-212-555-0100",
        "  212 555 0100  "
    })
    void commonUsFormats_becomeE164(String typed) {
        assertThat(UsPhoneNumber.normalize(typed)).isEqualTo("+12125550100");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "+44 20 7946 0958",  // UK
        "+52 55 1234 5678",  // Mexico
        "123-456-7890",      // area code can't start with 1
        "012-555-0100",      // area code can't start with 0
        "212-155-0100",      // exchange can't start with 1
        "212-055-0100",      // exchange can't start with 0
        "212-555-010",       // 9 digits
        "212-555-01000",     // 11 digits not starting with 1
        "22125550100",       // 11 digits not starting with 1
        "212-555-CALL",
        "212 555 0100 ext 5",
        "1+2125550100",
        "",
        "   "
    })
    void nonUsOrInvalidNumbers_areNotValidAfterNormalizing(String typed) {
        String result = UsPhoneNumber.normalize(typed);

        assertThat(result).doesNotMatch(UsPhoneNumber.E164_US_REGEX);
    }

    @Test
    void null_staysNull() {
        assertThat(UsPhoneNumber.normalize(null)).isNull();
    }
}
