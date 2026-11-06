package com.ems.auth.model;

public final class Staff extends User {

    public Staff(long id, String fullName, String username, String email, boolean active) {
        super(id, fullName, username, email, active);
    }

    @Override
    public String getRole() {
        return Role.STAFF.name();
    }
}
