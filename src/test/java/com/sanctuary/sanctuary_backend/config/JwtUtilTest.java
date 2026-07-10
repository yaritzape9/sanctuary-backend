package com.sanctuary.sanctuary_backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // HS256 requires a secret of at least 256 bits (32+ chars)
        ReflectionTestUtils.setField(jwtUtil, "secret", "this-is-a-test-secret-key-long-enough-for-hs256");
    }

    @Test
    void generateToken_returnsNonEmptyToken() {
        String token = jwtUtil.generateToken("user-123");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUserId_returnsOriginalUserId() {
        String token = jwtUtil.generateToken("user-123");
        assertEquals("user-123", jwtUtil.extractUserId(token));
    }

    @Test
    void isValid_returnsTrueForValidToken() {
        String token = jwtUtil.generateToken("user-123");
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        String token = jwtUtil.generateToken("user-123");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertFalse(jwtUtil.isValid(tampered));
    }

    @Test
    void isValid_returnsFalseForGarbageToken() {
        assertFalse(jwtUtil.isValid("not-a-real-token"));
    }
}