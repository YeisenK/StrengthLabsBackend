package com.strengthlabs.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RevokedTokenStore")
class RevokedTokenStoreTest {

    private RevokedTokenStore store;

    @BeforeEach
    void setUp() {
        store = new RevokedTokenStore();
    }

    @Test
    @DisplayName("isRevoked returns false for unknown jti")
    void unknownJtiNotRevoked() {
        assertFalse(store.isRevoked("never-seen"));
    }

    @Test
    @DisplayName("revoke + isRevoked returns true while not expired")
    void revokedJtiIsBlacklisted() {
        long farFuture = System.currentTimeMillis() + 60_000;
        store.revoke("jti-1", farFuture);
        assertTrue(store.isRevoked("jti-1"));
    }

    @Test
    @DisplayName("revoke ignores tokens already expired")
    void doesNotStoreExpiredTokens() {
        store.revoke("expired", System.currentTimeMillis() - 1);
        assertEquals(0, store.size());
    }

    @Test
    @DisplayName("isRevoked discards entries on read once expired")
    void selfHealingOnRead() {
        long soon = System.currentTimeMillis() + 5;
        store.revoke("ephemeral", soon);
        try {
            Thread.sleep(20);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        assertFalse(store.isRevoked("ephemeral"));
        assertEquals(0, store.size());
    }

    @Test
    @DisplayName("revoke is null-safe")
    void nullJtiIgnored() {
        store.revoke(null, System.currentTimeMillis() + 60_000);
        store.revoke("", System.currentTimeMillis() + 60_000);
        assertEquals(0, store.size());
    }

    @Test
    @DisplayName("clear empties the store")
    void clearEmpties() {
        store.revoke("a", System.currentTimeMillis() + 60_000);
        store.revoke("b", System.currentTimeMillis() + 60_000);
        store.clear();
        assertEquals(0, store.size());
    }
}
