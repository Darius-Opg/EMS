package com.ems.events.dao;

import com.ems.events.model.Event;
import com.ems.shared.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventDAO {

    // CREATE
    public long create(Event event) throws SQLException {
        String sql = """
                INSERT INTO events
                (event_name, description, venue, starts_at, ends_at, status, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, event.getName());
            stmt.setString(2, event.getDescription());
            stmt.setString(3, event.getVenue());
            stmt.setTimestamp(4, Timestamp.valueOf(event.getStartDateTime()));
            stmt.setTimestamp(5, Timestamp.valueOf(event.getEndDateTime()));
            stmt.setString(6, event.getStatus());
            stmt.setLong(7, event.getCreatedBy());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    event.setEventId(id);
                    return id;
                }
            }
        }

        throw new SQLException("Event creation failed: no ID was returned.");
    }

    // READ ONE
    public Optional<Event> findById(long id) throws SQLException {
        String sql = "SELECT * FROM events WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    // READ ALL
    public List<Event> findAll() throws SQLException {
        String sql = "SELECT * FROM events ORDER BY starts_at DESC";
        List<Event> events = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                events.add(mapRow(rs));
            }
        }

        return events;
    }

    // FILTER BY STATUS
    public List<Event> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM events WHERE status = ? ORDER BY starts_at";
        List<Event> events = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapRow(rs));
                }
            }
        }

        return events;
    }

    // UPDATE
    public boolean update(Event event) throws SQLException {
        String sql = """
                UPDATE events
                SET event_name = ?, description = ?, venue = ?,
                    starts_at = ?, ends_at = ?, status = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, event.getName());
            stmt.setString(2, event.getDescription());
            stmt.setString(3, event.getVenue());
            stmt.setTimestamp(4, Timestamp.valueOf(event.getStartDateTime()));
            stmt.setTimestamp(5, Timestamp.valueOf(event.getEndDateTime()));
            stmt.setString(6, event.getStatus());
            stmt.setLong(7, event.getEventId());

            return stmt.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM events WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // CONVERT DATABASE ROW INTO EVENT OBJECT
    private Event mapRow(ResultSet rs) throws SQLException {
        Event event = new Event();

        event.setEventId(rs.getLong("id"));
        event.setName(rs.getString("event_name"));
        event.setDescription(rs.getString("description"));
        event.setVenue(rs.getString("venue"));
        event.setStartDateTime(
                rs.getTimestamp("starts_at").toLocalDateTime()
        );
        event.setEndDateTime(
                rs.getTimestamp("ends_at").toLocalDateTime()
        );
        event.setStatus(rs.getString("status"));
        event.setCreatedBy(rs.getLong("created_by"));

        return event;
    }
}