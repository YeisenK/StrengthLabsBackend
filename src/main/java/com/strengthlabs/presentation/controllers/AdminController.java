package com.strengthlabs.presentation.controllers;

import com.strengthlabs.domain.entities.User;
import com.strengthlabs.domain.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.strengthlabs.presentation.middleware.LocalizedStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ── TRAINER + ADMIN ────────────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> listUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId().toString(),
                        "name", u.getName(),
                        "email", u.getEmail(),
                        "role", u.getRole().name(),
                        "active", u.isActive()))
                .toList();
        return ResponseEntity.ok(Map.of("items", users));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LocalizedStatusException(HttpStatus.NOT_FOUND, "error.user.not.found"));
        return ResponseEntity.ok(Map.of(
                "id", user.getId().toString(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "active", user.isActive()));
    }

    // ── ADMIN only ─────────────────────────────────────────────────────────────

    @DeleteMapping("/users/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LocalizedStatusException(HttpStatus.NOT_FOUND, "error.user.not.found"));
        user.deactivate();
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User deactivated"));
    }
}
