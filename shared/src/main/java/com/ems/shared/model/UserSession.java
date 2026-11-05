package com.ems.shared.model;


public final class UserSession {

    private final long userId;
    private final String fullName;
    private final String username;
    private final String role;

    public UserSession(
            long userId,
            String fullName,
            String username,
            String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.role = role;
    }

    public long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean hasRole(String expectedRole) {
        return role != null
                && role.equalsIgnoreCase(expectedRole);
    }
}
