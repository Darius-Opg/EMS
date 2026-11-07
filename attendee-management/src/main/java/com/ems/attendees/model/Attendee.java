package com.ems.attendees.model;

import java.time.OffsetDateTime;

public class Attendee {

    private Long id;
    private Long eventId;
    private String fullName;
    private String email;
    private String phone;
    private OffsetDateTime registeredAt;

    public Attendee() {
    }

    public Attendee(Long eventId, String fullName, String email, String phone) {
        this.eventId = eventId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public OffsetDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(OffsetDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public boolean isVip() {
        return false;
    }
}
