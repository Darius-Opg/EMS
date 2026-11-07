package com.ems.tickets.dao;

import com.ems.tickets.model.Ticket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for event tickets.
 * The caller supplies the connection so ticket operations can
 * participate in an existing database transaction.
 */
public class TicketDAO {

    private static final String SELECT_TICKET =
            "SELECT t.id, t.attendee_id, t.ticket_number, t.status, " +
            "t.issued_at, t.checked_in_at, t.checked_in_by, " +
            "a.full_name AS attendee_name, a.email AS attendee_email, " +
            "a.phone AS attendee_phone, a.is_vip, a.event_id, " +
            "e.event_name " +
            "FROM tickets t " +
            "JOIN attendees a ON t.attendee_id = a.id " +
            "JOIN events e ON a.event_id = e.id ";

    /**
     * Issues one ticket to an attendee.
     * The database UNIQUE constraint on attendee_id prevents duplicate tickets.
     */
    public Ticket issueTicket(Connection connection, long attendeeId,
                              String ticketNumber) throws SQLException {

        String sql = "INSERT INTO tickets (attendee_id, ticket_number) " +
                     "VALUES (?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, attendeeId);
            statement.setString(2, ticketNumber);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(connection, keys.getLong(1));
                }
            }
        }

        throw new SQLException("Ticket was inserted but its ID could not be retrieved.");
    }

    /**
     * Finds a ticket using its database ID.
     */
    public Ticket findById(Connection connection, long ticketId)
            throws SQLException {

        String sql = SELECT_TICKET + "WHERE t.id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapTicket(result);
                }
            }
        }

        return null;
    }

    /**
     * Finds a ticket using its unique ticket number.
     */
    public Ticket findByTicketNumber(Connection connection, String ticketNumber)
            throws SQLException {

        String sql = SELECT_TICKET + "WHERE t.ticket_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ticketNumber);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapTicket(result);
                }
            }
        }

        return null;
    }

    /**
     * Finds the ticket belonging to an attendee.
     */
    public Ticket findByAttendeeId(Connection connection, long attendeeId)
            throws SQLException {

        String sql = SELECT_TICKET + "WHERE t.attendee_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attendeeId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapTicket(result);
                }
            }
        }

        return null;
    }

    /**
     * Retrieves all tickets belonging to a particular event.
     */
    public List<Ticket> findByEventId(Connection connection, long eventId)
            throws SQLException {

        List<Ticket> tickets = new ArrayList<>();
        String sql = SELECT_TICKET +
                     "WHERE a.event_id = ? ORDER BY t.issued_at DESC";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tickets.add(mapTicket(result));
                }
            }
        }

        return tickets;
    }

    /**
     * Retrieves all tickets in the system.
     */
    public List<Ticket> findAll(Connection connection) throws SQLException {

        List<Ticket> tickets = new ArrayList<>();
        String sql = SELECT_TICKET + "ORDER BY t.issued_at DESC";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                tickets.add(mapTicket(result));
            }
        }

        return tickets;
    }

    /**
     * Cancels a ticket only when it is currently VALID.
     * Returns the number of rows updated.
     */
    public int cancelTicket(Connection connection, long ticketId)
            throws SQLException {

        String sql = "UPDATE tickets SET status = 'CANCELLED' " +
                     "WHERE id = ? AND status = 'VALID'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketId);
            return statement.executeUpdate();
        }
    }

    /**
     * Converts one database row into a Ticket object.
     */
    private Ticket mapTicket(ResultSet result) throws SQLException {

        long checkedInByValue = result.getLong("checked_in_by");
        Long checkedInBy = result.wasNull() ? null : checkedInByValue;

        long eventIdValue = result.getLong("event_id");
        Long eventId = result.wasNull() ? null : eventIdValue;

        return new Ticket(
                result.getLong("id"),
                result.getLong("attendee_id"),
                result.getString("ticket_number"),
                result.getString("status"),
                result.getObject("issued_at", OffsetDateTime.class),
                result.getObject("checked_in_at", OffsetDateTime.class),
                checkedInBy,
                result.getString("attendee_name"),
                result.getString("attendee_email"),
                result.getString("attendee_phone"),
                result.getBoolean("is_vip"),
                eventId,
                result.getString("event_name")
        );
    }

    /**
     * Checks in a ticket only if its current status is VALID.
     * The conditional update prevents duplicate check-ins.
     */
    public int checkInTicket(Connection connection, String ticketNumber,
                             long staffUserId) throws SQLException {

        String sql = "UPDATE tickets " +
                     "SET status = 'CHECKED_IN', " +
                     "checked_in_at = CURRENT_TIMESTAMP, " +
                     "checked_in_by = ? " +
                     "WHERE ticket_number = ? AND status = 'VALID'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, staffUserId);
            statement.setString(2, ticketNumber);
            return statement.executeUpdate();
        }
    }

    /**
     * Returns attendance totals for one event.
     * Order: registered, tickets issued, checked in,
     * not checked in, VIP attendees, checked-in VIP attendees.
     */
    public long[] getEventSummary(Connection connection, long eventId)
            throws SQLException {

        String sql =
                "SELECT COUNT(a.id), " +
                "COUNT(t.id), " +
                "COUNT(a.id) FILTER (WHERE t.status = 'CHECKED_IN'), " +
                "COUNT(a.id) FILTER (WHERE t.status IS DISTINCT FROM 'CHECKED_IN'), " +
                "COUNT(a.id) FILTER (WHERE a.is_vip = TRUE), " +
                "COUNT(a.id) FILTER (WHERE a.is_vip = TRUE AND t.status = 'CHECKED_IN') " +
                "FROM attendees a " +
                "LEFT JOIN tickets t ON a.id = t.attendee_id " +
                "WHERE a.event_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new long[]{
                            result.getLong(1),
                            result.getLong(2),
                            result.getLong(3),
                            result.getLong(4),
                            result.getLong(5),
                            result.getLong(6)
                    };
                }
            }
        }

        return new long[6];
    }

}
