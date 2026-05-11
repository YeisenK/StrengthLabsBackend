package com.strengthlabs.infrastructure.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory blacklist of revoked JWT IDs (jti). Entries expire automatically
 * once the token's natural expiration passes — no background sweep needed
 * because {@link #isRevoked(String)} discards stale entries on read.
 *
 * <p>Adequate for the academic scope. Production should replace this with
 * Redis (SET revoked:rt:{jti} 1 EX {ttl_remaining}).
 */
@Component
public class RevokedTokenStore {

    private final ConcurrentHashMap<String, Long> revoked = new ConcurrentHashMap<>();

    /** Revoke a token until {@code expiresAtMillis}. */
    public void revoke(String jti, long expiresAtMillis) {
        if (jti == null || jti.isBlank()) return;
        if (expiresAtMillis <= System.currentTimeMillis()) return;
        revoked.put(jti, expiresAtMillis);
    }

    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        Long expiresAt = revoked.get(jti);
        if (expiresAt == null) return false;
        if (expiresAt <= System.currentTimeMillis()) {
            revoked.remove(jti, expiresAt);
            return false;
        }
        return true;
    }

    /** Used by tests to start from a clean slate. */
    public void clear() {
        revoked.clear();
    }

    public int size() {
        return revoked.size();
    }
}
