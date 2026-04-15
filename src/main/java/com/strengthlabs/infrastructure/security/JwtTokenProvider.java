package com.strengthlabs.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final long REFRESH_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000; // 7 days

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final long accessExpirationMs;

    public JwtTokenProvider(@Value("${jwt.private-key}") String privateKeyBase64,
                            @Value("${jwt.public-key}") String publicKeyBase64,
                            @Value("${jwt.expiration-ms:3600000}") long accessExpirationMs) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");

            byte[] privBytes = Base64.getDecoder().decode(stripPemHeaders(privateKeyBase64));
            this.privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

            byte[] pubBytes = Base64.getDecoder().decode(stripPemHeaders(publicKeyBase64));
            this.publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(pubBytes));

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA keys — check jwt.private-key and jwt.public-key", e);
        }
        this.accessExpirationMs = accessExpirationMs;
    }

    public String generateAccessToken(UUID userId, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_MS))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /** Legacy alias used by existing code — delegates to access token. */
    public String generateToken(UUID userId, String role) {
        return generateAccessToken(userId, role);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public String extractRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    /** JWKS representation of the public key for external verification. */
    public Map<String, Object> getJwks() {
        String n = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getPublicExponent().toByteArray());
        return Map.of("keys", new Object[]{
                Map.of("kty", "RSA", "alg", "RS256", "use", "sig", "n", n, "e", e)
        });
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static String stripPemHeaders(String pem) {
        return pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s", "");
    }
}
