package com.strengthlabs.infrastructure.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static JwtTokenProvider provider;
    private static String privateKeyBase64;
    private static String publicKeyBase64;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair pair = kpg.generateKeyPair();

        privateKeyBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        publicKeyBase64  = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        provider = new JwtTokenProvider(privateKeyBase64, publicKeyBase64, 3_600_000L);
    }

    // ── generateAccessToken ───────────────────────────────────────────────────

    @Test
    void accessToken_isValid() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "USER");
        assertTrue(provider.isValid(token));
    }

    @Test
    void accessToken_claimsAreCorrect() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "TRAINER");

        assertEquals(userId, provider.extractUserId(token));
        assertEquals("TRAINER", provider.extractRole(token));
    }

    @Test
    void accessToken_typeClaimIsAccess() {
        String token = provider.generateAccessToken(UUID.randomUUID(), "USER");
        Claims claims = provider.parseToken(token);
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void accessToken_isNotRefreshToken() {
        String token = provider.generateAccessToken(UUID.randomUUID(), "USER");
        assertFalse(provider.isRefreshToken(token));
    }

    // ── generateRefreshToken ──────────────────────────────────────────────────

    @Test
    void refreshToken_isValid() {
        String token = provider.generateRefreshToken(UUID.randomUUID());
        assertTrue(provider.isValid(token));
    }

    @Test
    void refreshToken_typeClaimIsRefresh() {
        String token = provider.generateRefreshToken(UUID.randomUUID());
        assertTrue(provider.isRefreshToken(token));
    }

    @Test
    void refreshToken_hasNoRoleClaim() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateRefreshToken(userId);
        Claims claims = provider.parseToken(token);
        assertNull(claims.get("role", String.class));
    }

    @Test
    void refreshToken_subjectIsUserId() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateRefreshToken(userId);
        assertEquals(userId, provider.extractUserId(token));
    }

    // ── isValid ───────────────────────────────────────────────────────────────

    @Test
    void isValid_returnsFalseForGarbage() {
        assertFalse(provider.isValid("not.a.jwt"));
    }

    @Test
    void isValid_returnsFalseForTamperedPayload() {
        String token = provider.generateAccessToken(UUID.randomUUID(), "USER");
        // Replace the payload section with garbage
        String[] parts = token.split("\\.");
        String tampered = parts[0] + ".dGFtcGVyZWQ." + parts[2];
        assertFalse(provider.isValid(tampered));
    }

    @Test
    void isValid_returnsFalseForTokenSignedWithDifferentKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair otherPair = kpg.generateKeyPair();

        String otherPriv = Base64.getEncoder().encodeToString(otherPair.getPrivate().getEncoded());
        String otherPub  = Base64.getEncoder().encodeToString(otherPair.getPublic().getEncoded());
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherPriv, otherPub, 3_600_000L);

        String foreignToken = otherProvider.generateAccessToken(UUID.randomUUID(), "USER");
        assertFalse(provider.isValid(foreignToken));
    }

    @Test
    void isValid_returnsFalseForExpiredToken() throws Exception {
        // Provider with 1 ms expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                privateKeyBase64, publicKeyBase64, 1L);
        String token = shortLivedProvider.generateAccessToken(UUID.randomUUID(), "USER");
        Thread.sleep(10);
        assertFalse(shortLivedProvider.isValid(token));
    }

    // ── legacy alias ──────────────────────────────────────────────────────────

    @Test
    void generateToken_delegatesToAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId, "ADMIN");
        assertTrue(provider.isValid(token));
        assertEquals("ADMIN", provider.extractRole(token));
        assertFalse(provider.isRefreshToken(token));
    }

    // ── getJwks ───────────────────────────────────────────────────────────────

    @Test
    void getJwks_containsRsaKey() {
        Map<String, Object> jwks = provider.getJwks();
        assertTrue(jwks.containsKey("keys"));

        Object[] keys = (Object[]) jwks.get("keys");
        assertEquals(1, keys.length);

        @SuppressWarnings("unchecked")
        Map<String, Object> key = (Map<String, Object>) keys[0];
        assertEquals("RSA", key.get("kty"));
        assertEquals("RS256", key.get("alg"));
        assertEquals("sig", key.get("use"));
        assertNotNull(key.get("n"));
        assertNotNull(key.get("e"));
    }
}
