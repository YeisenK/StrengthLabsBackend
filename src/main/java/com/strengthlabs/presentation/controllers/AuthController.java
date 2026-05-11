package com.strengthlabs.presentation.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.strengthlabs.domain.entities.User;
import com.strengthlabs.domain.repositories.UserRepository;
import com.strengthlabs.infrastructure.security.JwtTokenProvider;
import com.strengthlabs.infrastructure.security.RevokedTokenStore;
import com.strengthlabs.presentation.middleware.LocalizedStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    /** Password policy: at least 8 chars, with at least one letter and one digit. */
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RevokedTokenStore revokedTokenStore;
    private final GoogleIdTokenVerifier googleVerifier;

    public AuthController(UserRepository userRepository,
                          JwtTokenProvider tokenProvider,
                          PasswordEncoder passwordEncoder,
                          RevokedTokenStore revokedTokenStore,
                          @Value("${google.client-id}") String googleClientId) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.revokedTokenStore = revokedTokenStore;
        this.googleVerifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    public record RegisterRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank
            @Size(min = 8, message = "{validation.password.min_length}")
            @Pattern(regexp = PASSWORD_PATTERN, message = "{validation.password.pattern}")
            String password) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refresh_token) {}

    public record GoogleRequest(@NotBlank String id_token) {}

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new LocalizedStatusException(HttpStatus.CONFLICT, "error.email.already.registered");
        }
        User user = new User(UUID.randomUUID(), req.name(), req.email(),
                passwordEncoder.encode(req.password()), User.Role.USER);
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenPair(user));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new LocalizedStatusException(HttpStatus.UNAUTHORIZED, "error.credentials.invalid"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new LocalizedStatusException(HttpStatus.UNAUTHORIZED, "error.credentials.invalid");
        }
        return ResponseEntity.ok(tokenPair(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@Valid @RequestBody RefreshRequest req) {
        String refreshToken = req.refresh_token();
        if (!tokenProvider.isValid(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new LocalizedStatusException(HttpStatus.UNAUTHORIZED, "error.refresh.token.invalid");
        }
        String jti = tokenProvider.extractJti(refreshToken);
        if (revokedTokenStore.isRevoked(jti)) {
            throw new LocalizedStatusException(HttpStatus.UNAUTHORIZED, "error.refresh.token.revoked");
        }
        UUID userId = tokenProvider.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LocalizedStatusException(HttpStatus.UNAUTHORIZED, "error.user.not.found"));
        // Rotate: the refresh token just used cannot be reused.
        revokedTokenStore.revoke(jti, tokenProvider.extractExpiresAtMillis(refreshToken));
        return ResponseEntity.ok(tokenPair(user));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LocalizedStatusException(HttpStatus.NOT_FOUND, "error.user.not.found"));
        return ResponseEntity.ok(Map.of(
                "id", user.getId().toString(),
                "name", user.getName(),
                "email", user.getEmail()
        ));
    }

    /**
     * Revoke the bearer access token (and optional refresh token) so they cannot
     * be reused. Always returns 204 — even on missing/invalid tokens — to avoid
     * leaking which tokens are valid.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                        @RequestBody(required = false) RefreshRequest body) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            tryRevoke(header.substring(7));
        }
        if (body != null && body.refresh_token() != null) {
            tryRevoke(body.refresh_token());
        }
        return ResponseEntity.noContent().build();
    }

    private void tryRevoke(String token) {
        if (!tokenProvider.isValid(token)) return;
        revokedTokenStore.revoke(
                tokenProvider.extractJti(token),
                tokenProvider.extractExpiresAtMillis(token));
    }

    /** JWKS endpoint — exposes the RSA public key so clients can verify tokens locally. */
    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok(tokenProvider.getJwks());
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, String>> loginWithGoogle(@Valid @RequestBody GoogleRequest req) {
        GoogleIdToken idToken;
        try {
            idToken = googleVerifier.verify(req.id_token());
        } catch (Exception e) {
            throw new LocalizedStatusException(HttpStatus.UNAUTHORIZED, "error.google.token.verify");
        }
        if (idToken == null) {
            throw new LocalizedStatusException(HttpStatus.UNAUTHORIZED, "error.google.token.invalid");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String rawName = (String) payload.get("name");
        String name = (rawName == null || rawName.isBlank()) ? email : rawName;

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User(UUID.randomUUID(), name, email,
                    passwordEncoder.encode(UUID.randomUUID().toString()), User.Role.USER);
            return userRepository.save(newUser);
        });

        return ResponseEntity.ok(tokenPair(user));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Map<String, String> tokenPair(User user) {
        return Map.of(
                "access_token", tokenProvider.generateAccessToken(user.getId(), user.getRole().name()),
                "refresh_token", tokenProvider.generateRefreshToken(user.getId())
        );
    }
}
