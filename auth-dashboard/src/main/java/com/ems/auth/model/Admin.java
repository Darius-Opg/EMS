package com.ems.auth.model;

public final class Admin extends User {

    public Admin(long id, String fullName, String username, String email, boolean active) {
        super(id, fullName, username, email, active);
    }

    @Override
    public String getRole() {
        return Role.ADMIN.name();
    }
}
