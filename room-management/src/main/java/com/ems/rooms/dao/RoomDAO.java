package com.ems.rooms.dao;

import com.ems.rooms.model.Room;
import com.ems.shared.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL access for persisted rooms and their calculated occupancy. */
public class RoomDAO {

    private static final String ROOM_COLUMNS = """
            SELECT room_id, room_name, capacity, event_id, occupied, room_status
            FROM room_occupancy
            """;

    public List<Room> findAll() throws SQLException {
        String sql = ROOM_COLUMNS + " ORDER BY room_name, room_id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (results.next()) rooms.add(readRoom(results));
            return List.copyOf(rooms);
        }
    }

    public List<Room> searchByName(String name) throws SQLException {
        String sql = ROOM_COLUMNS + " WHERE room_name ILIKE ? ORDER BY room_name, room_id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + (name == null ? "" : name.trim()) + "%");
            try (ResultSet results = statement.executeQuery()) {
                List<Room> rooms = new ArrayList<>();
                while (results.next()) rooms.add(readRoom(results));
                return List.copyOf(rooms);
            }
        }
    }

    public Optional<Room> findAssignedToEvent(int eventId) throws SQLException {
        String sql = ROOM_COLUMNS + " WHERE event_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(readRoom(results)) : Optional.empty();
            }
        }
    }

    public Room insert(String name, int capacity, int eventId) throws SQLException {
        String sql = """
                INSERT INTO rooms (room_name, capacity, event_id, status)
                VALUES (?, ?, ?, 'ASSIGNED')
                RETURNING id
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setInt(2, capacity);
            statement.setInt(3, eventId);
            long roomId;
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) throw new SQLException("The new room was not returned by the database.");
                roomId = results.getLong("id");
            }
            return findById(connection, roomId);
        }
    }

    public boolean eventExists(int eventId) throws SQLException {
        return exists("SELECT EXISTS (SELECT 1 FROM events WHERE id = ?)", eventId);
    }

    public boolean eventAlreadyHasRoom(int eventId) throws SQLException {
        return exists("SELECT EXISTS (SELECT 1 FROM rooms WHERE event_id = ?)", eventId);
    }

    public boolean roomNameExists(String name) throws SQLException {
        String sql = "SELECT EXISTS (SELECT 1 FROM rooms WHERE room_name = ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getBoolean(1);
            }
        }
    }

    private boolean exists(String sql, int value) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, value);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getBoolean(1);
            }
        }
    }

    public int getAssignedCapacityForEvent(int eventId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(capacity), 0) FROM rooms WHERE event_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1);
            }
        }
    }

    public int getRegisteredAttendeeCountForEvent(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendees WHERE event_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1);
            }
        }
    }

    public boolean hasRoomWithCapacity(int roomId) throws SQLException {
        Room room = findById(roomId);
        return room != null && room.getCurrentOccupancy() < room.getCapacity();
    }

    public boolean hasOccupants(int roomId) throws SQLException {
        Room room = findById(roomId);
        return room != null && room.getCurrentOccupancy() > 0;
    }

    private Room findById(int roomId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findById(connection, roomId);
        }
    }

    private Room findById(Connection connection, long roomId) throws SQLException {
        String sql = ROOM_COLUMNS + " WHERE room_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? readRoom(results) : null;
            }
        }
    }

    private Room readRoom(ResultSet results) throws SQLException {
        long assignedEvent = results.getLong("event_id");
        Integer eventId = results.wasNull() ? null : (int) assignedEvent;
        return new Room(results.getInt("room_id"), results.getString("room_name"),
                results.getInt("capacity"), results.getInt("occupied"),
                eventId, results.getString("room_status"));
    }
}
