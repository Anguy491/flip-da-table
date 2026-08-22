package com.flip.backend.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecureTokenServiceTest {
    private final SecureTokenService tokens = new SecureTokenService();

    @Test
    void createsUrlSafeHighEntropyTokensAndStableHashes() {
        String first = tokens.generate();
        String second = tokens.generate();

        assertEquals(43, first.length());
        assertNotEquals(first, second);
        assertTrue(first.matches("[A-Za-z0-9_-]+"));
        assertEquals(64, tokens.hash(first).length());
        assertEquals(tokens.hash(first), tokens.hash(first));
        assertNotEquals(first, tokens.hash(first));
    }
}
