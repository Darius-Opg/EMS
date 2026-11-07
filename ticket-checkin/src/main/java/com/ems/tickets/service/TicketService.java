package com.ems.tickets.service;

import com.ems.shared.config.DatabaseConnection;
import com.ems.shared.model.UserSession;
import com.ems.tickets.dao.TicketDAO;
import com.ems.tickets.model.Ticket;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Handles ticket operations and business rules.
 */
public class TicketService {

    private final TicketDAO ticketDAO;

    public TicketService() {
        this.ticketDAO = new TicketDAO();
    }

    /**
     * Issues a ticket using an existing database connection.
     * This method allows attendee registration and ticket issuance
     * to participate in the same database transaction.
     */
    public Ticket issueTicket(Connection connection, long attendeeId,
                              UserSession session) throws SQLException {

        checkTicketIssuingPermission(session);

        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required.");
        }

        if (attendeeId <= 0) {
            throw new IllegalArgumentException("Invalid attendee ID.");
        }

        String ticketNumber = generateTicketNumber();

        return ticketDAO.issueTicket(connection, attendeeId, ticketNumber);
    }

    /**
     * Issues a ticket with a supplied ticket number.
     * Useful when another part of the application generates the number.
     */
    public Ticket issueTicket(Connection connection, long attendeeId,
                              String ticketNumber, UserSession session)
            throws SQLException {

        checkTicketIssuingPermission(session);

        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required.");
        }

        if (attendeeId <= 0) {
            throw new IllegalArgumentException("Invalid attendee ID.");
        }

        if (ticketNumber == null ||
                !ticketNumber.matches("[A-Za-z0-9-]{4,40}")) {
            throw new IllegalArgumentException("Ticket number must contain 4 to 40 letters, numbers, or hyphens.");
        }

        return ticketDAO.issueTicket(connection, attendeeId, ticketNumber);
    }

    /**
     * Generates a ticket number with the EMS prefix.
     */
    private String generateTicketNumber() {
        String uniquePart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();

        return "EMS-" + uniquePart;
    }

    /**
     * Searches for a ticket using its ticket number.
     */
    public Ticket findByTicketNumber(String ticketNumber)
            throws SQLException {

        if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket number is required.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            return ticketDAO.findByTicketNumber(connection, ticketNumber.trim());
        }
    }

    /**
     * Finds the ticket belonging to an attendee.
     */
    public Ticket findByAttendeeId(long attendeeId) throws SQLException {

        if (attendeeId <= 0) {
            throw new IllegalArgumentException("Invalid attendee ID.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            return ticketDAO.findByAttendeeId(connection, attendeeId);
        }
    }

    /**
     * Retrieves all tickets for a particular event.
     */
    public List<Ticket> findByEventId(long eventId) throws SQLException {

        if (eventId <= 0) {
            throw new IllegalArgumentException("Invalid event ID.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            return ticketDAO.findByEventId(connection, eventId);
        }
    }

    /**
     * Retrieves all tickets.
     */
    public List<Ticket> findAll() throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection()) {
            return ticketDAO.findAll(connection);
        }
    }

    /**
     * Cancels a ticket.
     * Only ADMIN and ORGANIZER users may cancel tickets.
     *
     * @return true if the ticket was successfully cancelled.
     */
    public boolean cancelTicket(long ticketId, UserSession session)
            throws SQLException {

        checkTicketCancellationPermission(session);

        if (ticketId <= 0) {
            throw new IllegalArgumentException("Invalid ticket ID.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            return ticketDAO.cancelTicket(connection, ticketId) == 1;
        }
    }

    private void checkTicketIssuingPermission(UserSession session) {

        if (session == null ||
                !(session.hasRole("ADMIN")
                || session.hasRole("ORGANIZER")
                || session.hasRole("STAFF"))) {

            throw new SecurityException(
                    "You do not have permission to issue tickets.");
        }
    }

    private void checkTicketCancellationPermission(UserSession session) {

        if (session == null ||
                !(session.hasRole("ADMIN")
                || session.hasRole("ORGANIZER"))) {

            throw new SecurityException(
                    "You do not have permission to cancel tickets.");
        }
    }

    /**
     * Retrieves attendance totals for a particular event.
     */
    public long[] getAttendanceSummary(long eventId) throws SQLException {

        if (eventId <= 0) {
            throw new IllegalArgumentException("Invalid event ID.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            return ticketDAO.getEventSummary(connection, eventId);
        }
    }
}
