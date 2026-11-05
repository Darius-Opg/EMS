# Room Management

**Owner:** Member 5  
**Package:** `com.ems.rooms`  
**Primary table:** `rooms`

## Purpose

Manage rooms, assign one room to an event, provide registration capacity to Attendee Management, and monitor physical occupancy from successful check-ins.

## Responsibilities

- Register, view, edit, search, filter, and delete rooms where permitted. Capture room name, positive capacity, and status; let the database create IDs and timestamps.
- Support `AVAILABLE`, `ASSIGNED`, and `MAINTENANCE` statuses. Maintenance rooms cannot be assigned; assigned rooms cannot be deleted or assigned elsewhere.
- Assign an available room to an event and unassign it where permitted. Enforce one room per event and one event per room.
- Provide service methods for Attendee Management to retrieve a room assigned to an event, capacity, registered-attendee count, and remaining registration capacity.
- Display assigned event, capacity, occupied seats, remaining physical seats, occupancy percentage, and status. Calculate occupancy from tickets with `CHECKED_IN` status, preferably through the `room_occupancy` view.
- Warn at 80% occupancy and at full capacity. Do not store a separate occupancy value in `rooms`.
- Coordinate with Ticket & Check-In for check-in data and Event Management for event data.

## Required classes

- Model: `Room`
- DAO: `RoomDAO`
- Service: `RoomService`
- UI panel: `RoomManagementPanel`

## Database and implementation rules

This module owns `rooms`; it reads related event, attendee, and ticket data. Use `DatabaseConnection.getConnection()`, JDBC `PreparedStatement`s, try-with-resources, and `UserSession` for authorization. Keep SQL out of Swing components.

## Capacity distinction

Use total registered attendees to decide whether another registration is allowed. Use checked-in attendees to show current physical occupancy. For example, with capacity 100, 90 registered, and 60 checked in, only 10 more may register while physical occupancy is 60.

## Done when

- Rooms can be managed and assigned under the one-room/one-event constraints.
- Invalid capacity and maintenance assignments are rejected.
- Registration capacity is exposed to Attendee Management and counts all registrations.
- Occupancy and capacity warnings use actual check-ins and display accurately.
