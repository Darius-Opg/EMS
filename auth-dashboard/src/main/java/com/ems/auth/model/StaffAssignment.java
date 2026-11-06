package com.ems.auth.model;

/** A staff duty associated with an event, a room, or both. */
public record StaffAssignment(
        long id,
        long staffId,
        String staffName,
        Long eventId,
        String eventName,
        Long roomId,
        String roomName,
        String duty,
        String assignedBy) {
}
