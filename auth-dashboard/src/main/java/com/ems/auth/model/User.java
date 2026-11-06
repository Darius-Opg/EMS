package com.ems.auth.model;

import java.util.Objects;

/** Shared account data; concrete subclasses represent supported account roles. */
public abstract class User {

    public enum Role { ADMIN, ORGANIZER, STAFF }

    private final long id;
    private final String fullName;
    private final String username;
    private final String email;
    private final boolean active;

    protected User(long id, String fullName, String username, String email, boolean active) {
        this.id = id;
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.username = Objects.requireNonNull(username, "username");
        this.email = Objects.requireNonNull(email, "email");
        this.active = active;
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }
    public abstract String getRole();
}
