package com.ems.tickets.service;

import com.ems.shared.config.DatabaseConnection;
import com.ems.shared.model.UserSession;
import com.ems.tickets.dao.TicketDAO;
import com.ems.tickets.model.Ticket;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Handles ticket validation and event check-in.
 */
public class CheckInService {

    private final TicketDAO ticketDAO;

    public CheckInService() {
        this.ticketDAO = new TicketDAO();
    }

    /**
     * Validates a ticket before allowing event entry.
     */
    public Ticket validateTicket(String ticketNumber) throws SQLException {

        if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a ticket number.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {

            Ticket ticket = ticketDAO.findByTicketNumber(
                    connection, ticketNumber.trim());

            if (ticket == null) {
                throw new IllegalArgumentException("Ticket not found.");
            }

            if (ticket.isCancelled()) {
                throw new IllegalStateException("This ticket has been cancelled.");
            }

            if (ticket.isCheckedIn()) {
                throw new IllegalStateException("This ticket has already been checked in.");
            }

            if (!ticket.isValid()) {
                throw new IllegalStateException("This ticket is not valid.");
            }

            return ticket;
        }
    }

    /**
     * Checks in an attendee and records the staff member and timestamp.
     *
     * Only ADMIN, ORGANIZER and STAFF users can perform check-in.
     */
    public Ticket checkIn(String ticketNumber, UserSession session)
            throws SQLException {

        checkPermission(session);

        if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a ticket number.");
        }

        String cleanTicketNumber = ticketNumber.trim();

        try (Connection connection = DatabaseConnection.getConnection()) {

            Ticket ticket = ticketDAO.findByTicketNumber(
                    connection, cleanTicketNumber);

            if (ticket == null) {
                throw new IllegalArgumentException("Ticket not found.");
            }

            if (ticket.isCancelled()) {
                throw new IllegalStateException("This ticket has been cancelled.");
            }

            if (ticket.isCheckedIn()) {
                throw new IllegalStateException("This ticket has already been checked in.");
            }

            if (!ticket.isValid()) {
                throw new IllegalStateException("This ticket is not valid.");
            }

            // Only one request can change a VALID ticket to CHECKED_IN.
            int rowsUpdated = ticketDAO.checkInTicket(
                    connection, cleanTicketNumber, session.getUserId());

            if (rowsUpdated != 1) {
                throw new IllegalStateException(
                        "Check-in failed. The ticket may have been checked in by another staff member.");
            }

            return ticketDAO.findByTicketNumber(
                    connection, cleanTicketNumber);
        }
    }

    private void checkPermission(UserSession session) {

        if (session == null ||
                !(session.hasRole("ADMIN")
                || session.hasRole("ORGANIZER")
                || session.hasRole("STAFF"))) {

            throw new SecurityException(
                    "You do not have permission to check in attendees.");
        }
    }
}
