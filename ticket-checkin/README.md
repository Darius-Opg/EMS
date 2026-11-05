# Ticket and Check-In

**Owner:** Member 4  
**Package:** `com.ems.tickets`  
**Primary table:** `tickets`

## Purpose

Issue one unique ticket per attendee, validate tickets at event entry, record check-ins, and provide event attendance reports.

## Responsibilities

- Issue tickets with a unique ticket number, attendee ID, status, and issue timestamp. Do not issue a second ticket for the same attendee.
- Provide ticket issuance through a service method that can participate in Attendee Management's existing connection and transaction.
- List, search by ticket number or attendee, filter by event, view, and cancel tickets where permitted.
- Validate ticket numbers and display ticket, attendee, event, and VIP details. Reject missing, cancelled, or already checked-in tickets.
- Check in valid tickets. Record timestamp and staff user ID; use a conditional update from `VALID` and verify exactly one row changed to prevent duplicate check-ins.
- Provide per-event reports for registered attendees, tickets issued, checked-in and not-checked-in attendees, VIP attendees, and checked-in VIP attendees. Add CSV export where practical.
- Make check-in data available to Room Management for occupancy calculations.

## Required classes

- Model: `Ticket`
- DAO: `TicketDAO`
- Services: `TicketService`, `CheckInService`
- UI panels: `TicketManagementPanel`, `CheckInPanel`, `AttendanceReportPanel`

## Database and implementation rules

This module owns `tickets` and reads related attendee/event data for validation and reports. It references `attendees` and the staff member in `users`. Use `DatabaseConnection.getConnection()`, JDBC `PreparedStatement`s, try-with-resources, and `UserSession`; keep SQL out of Swing components.

## Done when

- Each attendee receives at most one unique ticket.
- Invalid and cancelled tickets cannot be checked in.
- A valid ticket can be checked in once, with check-in time and staff recorded.
- Event attendance reports show accurate totals.
- Ticket issuance can join the attendee-registration transaction, and check-ins can drive room occupancy.
