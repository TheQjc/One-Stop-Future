package com.campus.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JwtUtilTests {

    @Test
    void tokenRoundTripPreservesStableSubjectAndRole() {
        JwtUtil jwtUtil = new JwtUtil("test-secret-key-test-secret-key-1234", 3600);
        String token = jwtUtil.generateToken("42", "USER");

        assertEquals("42", jwtUtil.extractSubject(token));
        assertEquals("USER", jwtUtil.extractRole(token));
        assertTrue(jwtUtil.isTokenValid(token));
    }
}
