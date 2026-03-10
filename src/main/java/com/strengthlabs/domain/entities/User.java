package com.strengthlabs.domain.entities;

import java.util.UUID;

public class User {
    private UUID id;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean active;

    public enum Role {
        USER, TRAINER, ADMIN
    }

    public User(UUID id, String email, String passwordHash, Role role) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
}
