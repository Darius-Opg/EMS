# Event Management

**Owner:** Member 2  
**Package:** `com.ems.events`  
**Primary tables:** `events`, `vendors`

## Purpose

Create, schedule, monitor, and maintain events, along with vendors associated with those events.

## Responsibilities

- Create events with name, description, venue, start/end date and time, and status. Record the authenticated creator's user ID.
- List, view, edit, search, filter, and update events. Support `UPCOMING`, `ONGOING`, `COMPLETED`, and `CANCELLED` statuses.
- Validate required fields and ensure the end time is after the start time. Reject incomplete or invalid events.
- Prevent deletion when attendees or an assigned room still reference the event; show a clear message.
- Register, view, edit, search, and remove vendors associated with an existing event. Capture vendor name, service type, contact person, phone, and email.
- Expose event information through service methods for Attendee Management and Room Management.

## Required classes

- Models: `Event`, `Vendor`
- DAOs: `EventDAO`, `VendorDAO`
- Service: `EventService`
- UI panels: `EventManagementPanel`, `VendorManagementPanel`

## Database and implementation rules

This module owns `events` and `vendors`. Events reference their creator in `users`; vendors reference an event. Use `DatabaseConnection.getConnection()`, JDBC `PreparedStatement`s, and try-with-resources. Use `UserSession` for the creator ID and authorization. Keep SQL out of Swing components and business rules in services.

## Done when

- Authorized users can create, view, update, search, filter, and manage event status.
- Invalid fields and date ranges are rejected.
- Vendors can be managed against existing events.
- Referenced events cannot be accidentally deleted.
- Other modules can retrieve the event details they need through the service layer.
