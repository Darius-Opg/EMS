package com.ems.rooms.service;

import com.ems.rooms.dao.RoomDAO;
import com.ems.rooms.model.Room;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Room operations backed by PostgreSQL. */
public class RoomService {
    private final RoomDAO roomDAO;

    public RoomService() {
        this(new RoomDAO());
    }

    public RoomService(RoomDAO roomDAO) {
        this.roomDAO = Objects.requireNonNull(roomDAO, "roomDAO");
    }

    public void registerRoom(String name, int capacity, int eventId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name is required.");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Room name must be at most 100 characters.");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive.");
        }
        if (eventId <= 0) {
            throw new IllegalArgumentException("Choose a valid event ID.");
        }
        try {
            if (roomDAO.roomNameExists(name.trim())) {
                throw new IllegalArgumentException("A room with that name already exists.");
            }
            if (!roomDAO.eventExists(eventId)) {
                throw new IllegalArgumentException("No event exists with ID " + eventId + ".");
            }
            if (roomDAO.eventAlreadyHasRoom(eventId)) {
                throw new IllegalArgumentException("Event " + eventId + " already has a room assigned.");
            }
            roomDAO.insert(name.trim(), capacity, eventId);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new RoomServiceException(registerError(exception), exception);
        }
    }

    private String registerError(SQLException exception) {
        if ("23505".equals(exception.getSQLState())) {
            String details = exception.getMessage() == null ? "" : exception.getMessage();
            if (details.contains("room_name")) return "A room with that name already exists.";
            if (details.contains("event_id")) return "That event already has a room assigned.";
        }
        if ("23503".equals(exception.getSQLState())) {
            return "The selected event does not exist anymore.";
        }
        return "Unable to register room: " + exception.getMessage();
    }

    public List<Room> getAllRooms() {
        try {
            return roomDAO.findAll();
        } catch (SQLException exception) {
            throw new RoomServiceException("Unable to load rooms.", exception);
        }
    }

    public List<Room> searchRoomsByName(String name) {
        try {
            return roomDAO.searchByName(name);
        } catch (SQLException exception) {
            throw new RoomServiceException("Unable to search rooms.", exception);
        }
    }

    public int getAssignedCapacityForEvent(int eventId) {
        try {
            return roomDAO.getAssignedCapacityForEvent(eventId);
        } catch (SQLException exception) {
            throw new RoomServiceException("Unable to load room capacity for the event.", exception);
        }
    }

    /** Returns the room assigned to an event, or empty when the event has no room. */
    public Optional<Room> getRoomAssignedToEvent(int eventId) {
        try {
            return roomDAO.findAssignedToEvent(eventId);
        } catch (SQLException exception) {
            throw new RoomServiceException("Unable to load the room assigned to the event.", exception);
        }
    }

    /** Capacity of the event's assigned room, or zero when no room is assigned. */
    public int getAssignedRoomCapacityForEvent(int eventId) {
        return getRoomAssignedToEvent(eventId).map(Room::getCapacity).orElse(0);
    }

    public int getRegisteredAttendeeCountForEvent(int eventId) {
        try {
            return roomDAO.getRegisteredAttendeeCountForEvent(eventId);
        } catch (SQLException exception) {
            throw new RoomServiceException("Unable to load registered attendees for the event.", exception);
        }
    }

    /** Remaining registration capacity is based on all registrations, not check-ins. */
    public int getRemainingRegistrationCapacityForEvent(int eventId) {
        int capacity = getAssignedRoomCapacityForEvent(eventId);
        if (capacity == 0) return 0;
        return Math.max(0, capacity - getRegisteredAttendeeCountForEvent(eventId));
    }

    /** Capacity preflight only; actual ticket check-in is persisted by Ticket & Check-In. */
    public boolean handleAttendeeCheckIn(int id) {
        try {
            return roomDAO.hasRoomWithCapacity(id);
        } catch (SQLException exception) {
            throw new RoomServiceException("Unable to check room capacity.", exception);
        }
    }

    /** Occupancy preflight only; actual ticket check-in state remains owned by Ticket & Check-In. */
    public boolean handleAttendeeExit(int id) {
        try {
            return roomDAO.hasOccupants(id);
        } catch (SQLException exception) {
            throw new RoomServiceException("Unable to check room occupancy.", exception);
        }
    }

    public static final class RoomServiceException extends RuntimeException {
        public RoomServiceException(String message, Throwable cause) { super(message, cause); }
    }
}
