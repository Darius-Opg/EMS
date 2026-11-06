package com.ems.auth.model;

public final class Organizer extends User {

    public Organizer(long id, String fullName, String username, String email, boolean active) {
        super(id, fullName, username, email, active);
    }

    @Override
    public String getRole() {
        return Role.ORGANIZER.name();
    }
}
