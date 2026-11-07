package com.ems.tickets.model;

import java.time.OffsetDateTime;

/**
 * Represents a ticket issued to an event attendee.
 */
public class Ticket {

    private final long id;
    private final long attendeeId;
    private final String ticketNumber;
    private final String status;
    private final OffsetDateTime issuedAt;
    private final OffsetDateTime checkedInAt;
    private final Long checkedInBy;

    private final String attendeeName;
    private final String attendeeEmail;
    private final String attendeePhone;
    private final boolean vip;
    private final Long eventId;
    private final String eventName;

    public Ticket(long id, long attendeeId, String ticketNumber,
                  String status, OffsetDateTime issuedAt,
                  OffsetDateTime checkedInAt, Long checkedInBy,
                  String attendeeName, String attendeeEmail,
                  String attendeePhone, boolean vip,
                  Long eventId, String eventName) {

        this.id = id;
        this.attendeeId = attendeeId;
        this.ticketNumber = ticketNumber;
        this.status = status;
        this.issuedAt = issuedAt;
        this.checkedInAt = checkedInAt;
        this.checkedInBy = checkedInBy;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.attendeePhone = attendeePhone;
        this.vip = vip;
        this.eventId = eventId;
        this.eventName = eventName;
    }

    public long getId() { return id; }
    public long getAttendeeId() { return attendeeId; }
    public String getTicketNumber() { return ticketNumber; }
    public String getStatus() { return status; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public OffsetDateTime getCheckedInAt() { return checkedInAt; }
    public Long getCheckedInBy() { return checkedInBy; }
    public String getAttendeeName() { return attendeeName; }
    public String getAttendeeEmail() { return attendeeEmail; }
    public String getAttendeePhone() { return attendeePhone; }
    public boolean isVip() { return vip; }
    public Long getEventId() { return eventId; }
    public String getEventName() { return eventName; }

    public boolean isValid() {
        return "VALID".equalsIgnoreCase(status);
    }

    public boolean isCheckedIn() {
        return "CHECKED_IN".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "id=" + id +
                ", attendeeId=" + attendeeId +
                ", ticketNumber='" + ticketNumber + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
