package com.ems.attendees.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Registration-specific capacity queries.
 *
 * The caller owns the connection and transaction. Locking the assigned room
 * serializes registrations for the same event until the transaction finishes.
 */
public class RegistrationCapacityDAO {

    public CapacitySnapshot lockAndGetCapacity(
            Connection connection,
            long eventId
    ) throws SQLException {

        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required.");
        }

        if (eventId <= 0) {
            throw new IllegalArgumentException("A valid event ID is required.");
        }

        String roomSql = """
                SELECT id, capacity
                FROM rooms
                WHERE event_id = ?
                  AND status = 'ASSIGNED'
                FOR UPDATE
                """;

        long capacity;

        try (PreparedStatement statement = connection.prepareStatement(roomSql)) {
            statement.setLong(1, eventId);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }

                capacity = results.getLong("capacity");
            }
        }

        String countSql = """
                SELECT COUNT(*)
                FROM attendees
                WHERE event_id = ?
                """;

        long registeredCount;

        try (PreparedStatement statement = connection.prepareStatement(countSql)) {
            statement.setLong(1, eventId);

            try (ResultSet results = statement.executeQuery()) {
                results.next();
                registeredCount = results.getLong(1);
            }
        }

        return new CapacitySnapshot(capacity, registeredCount);
    }

    public record CapacitySnapshot(long capacity, long registeredCount) {
        public long getRemainingCapacity() {
            return Math.max(0, capacity - registeredCount);
        }
    }
}
