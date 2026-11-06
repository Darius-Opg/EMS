package com.ems.auth.dao;

import com.ems.auth.model.StaffAssignment;
import com.ems.shared.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Database access for the STAFF_ASSIGNMENT entity. */
public class StaffAssignmentDAO {

    private static final String SELECT_ASSIGNMENTS = """
            SELECT a.id, a.staff_id, s.full_name AS staff_name,
                   a.event_id, e.event_name,
                   a.room_id, r.room_name,
                   a.duty, o.full_name AS assigned_by
            FROM staff_assignment a
            JOIN users s ON s.id = a.staff_id
            LEFT JOIN events e ON e.id = a.event_id
            LEFT JOIN rooms r ON r.id = a.room_id
            JOIN users o ON o.id = a.assigned_by
            """;

    public StaffAssignment create(long staffId, Long eventId, Long roomId,
                                  String duty, long organizerId) throws SQLException {
        String query = """
                INSERT INTO staff_assignment (staff_id, event_id, room_id, duty, assigned_by)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, staffId);
            setNullableLong(statement, 2, eventId);
            setNullableLong(statement, 3, roomId);
            statement.setString(4, duty);
            statement.setLong(5, organizerId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("The assignment was not returned by the database.");
                }
                return findById(connection, results.getLong(1));
            }
        }
    }

    public List<StaffAssignment> findAll() throws SQLException {
        return findAssignments(SELECT_ASSIGNMENTS + " ORDER BY LOWER(s.full_name), a.id", null);
    }

    public List<StaffAssignment> findForStaff(long staffId) throws SQLException {
        return findAssignments(SELECT_ASSIGNMENTS + " WHERE a.staff_id = ? ORDER BY a.id", staffId);
    }

    public boolean delete(long assignmentId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM staff_assignment WHERE id = ?")) {
            statement.setLong(1, assignmentId);
            return statement.executeUpdate() == 1;
        }
    }

    public List<StaffOption> findActiveStaff() throws SQLException {
        String query = """
                SELECT id, full_name, username FROM users
                WHERE role = 'STAFF' AND is_active = TRUE
                ORDER BY LOWER(full_name), id
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet results = statement.executeQuery()) {
            List<StaffOption> staff = new ArrayList<>();
            while (results.next()) {
                staff.add(new StaffOption(results.getLong("id"),
                        results.getString("full_name"), results.getString("username")));
            }
            return staff;
        }
    }

    public List<EventOption> findEvents() throws SQLException {
        String query = "SELECT id, event_name FROM events ORDER BY starts_at, id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet results = statement.executeQuery()) {
            List<EventOption> events = new ArrayList<>();
            while (results.next()) {
                events.add(new EventOption(results.getLong("id"), results.getString("event_name")));
            }
            return events;
        }
    }

    public List<RoomOption> findRooms() throws SQLException {
        String query = """
                SELECT id, room_name, status FROM rooms
                WHERE status <> 'MAINTENANCE'
                ORDER BY room_name, id
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet results = statement.executeQuery()) {
            List<RoomOption> rooms = new ArrayList<>();
            while (results.next()) {
                rooms.add(new RoomOption(results.getLong("id"), results.getString("room_name"),
                        results.getString("status")));
            }
            return rooms;
        }
    }

    private List<StaffAssignment> findAssignments(String query, Long staffId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            if (staffId != null) {
                statement.setLong(1, staffId);
            }
            try (ResultSet results = statement.executeQuery()) {
                List<StaffAssignment> assignments = new ArrayList<>();
                while (results.next()) {
                    assignments.add(readAssignment(results));
                }
                return assignments;
            }
        }
    }

    private StaffAssignment findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT_ASSIGNMENTS + " WHERE a.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("The inserted assignment could not be found.");
                }
                return readAssignment(results);
            }
        }
    }

    private StaffAssignment readAssignment(ResultSet results) throws SQLException {
        long eventId = results.getLong("event_id");
        Long eventKey = results.wasNull() ? null : eventId;
        long roomId = results.getLong("room_id");
        Long roomKey = results.wasNull() ? null : roomId;
        return new StaffAssignment(results.getLong("id"), results.getLong("staff_id"),
                results.getString("staff_name"), eventKey, results.getString("event_name"),
                roomKey, results.getString("room_name"), results.getString("duty"),
                results.getString("assigned_by"));
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    public record StaffOption(long id, String fullName, String username) {
        @Override public String toString() { return fullName + " (" + username + ")"; }
    }

    public record EventOption(long id, String name) {
        @Override public String toString() { return name; }
    }

    public record RoomOption(long id, String name, String status) {
        @Override public String toString() { return name + " (" + status + ")"; }
    }
}
