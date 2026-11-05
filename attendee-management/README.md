# Attendee Management

**Owner:** Member 3  
**Package:** `com.ems.attendees`  
**Primary table:** `attendees`

## Purpose

Register people for existing events and manage their attendee records while enforcing event registration capacity and ensuring each successful registration receives a ticket.

## Responsibilities

- Register attendees with full name, email, optional phone, event, and VIP status. Let PostgreSQL set the registration timestamp.
- List, view, edit, search by name/email/phone, filter by event, and delete attendees where permitted.
- Validate required names and email, validate email and any provided phone number, reject duplicate email registrations for the same event, and require an existing event.
- Enforce authorization in the service layer.
- Before registration, confirm the event has an assigned room and compare **all registered attendees** with that room's capacity. Do not use check-in counts for registration capacity.
- Coordinate with Room Management to retrieve the event's room capacity and with Event Management to retrieve available events.
- Coordinate with Ticket & Check-In so a successful registration creates exactly one ticket.

## Required classes

- Model: `Attendee`
- DAO: `AttendeeDAO`
- Service: `RegistrationService`
- UI panel: `AttendeeManagementPanel`

## Transaction requirement

Attendee insertion and ticket issuance must share one database connection and transaction:

1. Validate event, room, capacity, input, and permissions.
2. Insert the attendee and retrieve its generated ID.
3. Call the ticket issuance service using the same connection/transaction.
4. Commit only after both writes succeed; otherwise roll back both.

## Database and implementation rules

This module owns `attendees`; tickets are owned by Ticket & Check-In. Use `DatabaseConnection.getConnection()`, `PreparedStatement`s, try-with-resources, `ValidationUtil`, and `UserSession` as appropriate. Keep SQL out of Swing components.

## Done when

- Attendee records can be managed and searched.
- Invalid input, duplicate event/email registrations, and registrations without an event are rejected.
- Registration capacity uses total registrations and is enforced correctly.
- Every successful registration gets exactly one ticket, with both writes committed or rolled back together.
