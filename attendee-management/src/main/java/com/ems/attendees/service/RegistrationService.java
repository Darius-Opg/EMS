package com.ems.attendees.service;

import com.ems.attendees.dao.AttendeeDAO;
import com.ems.attendees.dao.RegistrationCapacityDAO;
import com.ems.attendees.model.Attendee;
import com.ems.attendees.model.VipAttendee;
import com.ems.shared.config.DatabaseConnection;
import com.ems.shared.model.UserSession;
import com.ems.shared.utils.NotificationUtil;
import com.ems.shared.utils.ValidationUtil;
import com.ems.tickets.model.Ticket;
import com.ems.tickets.service.TicketService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

public class RegistrationService {

    private final AttendeeDAO attendeeDAO;
    private final RegistrationCapacityDAO capacityDAO;
    private final TicketService ticketService;
    private final ConnectionFactory connectionFactory;

    public RegistrationService() {
        this(
                new AttendeeDAO(),
                new RegistrationCapacityDAO(),
                new TicketService(),
                DatabaseConnection::getConnection
        );
    }

    public RegistrationService(
            AttendeeDAO attendeeDAO,
            RegistrationCapacityDAO capacityDAO,
            TicketService ticketService,
            ConnectionFactory connectionFactory
    ) {
        this.attendeeDAO = Objects.requireNonNull(attendeeDAO, "attendeeDAO");
        this.capacityDAO = Objects.requireNonNull(capacityDAO, "capacityDAO");
        this.ticketService = Objects.requireNonNull(ticketService, "ticketService");
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory,
                "connectionFactory"
        );
    }

    public List<EventOption> getRegistrableEvents(UserSession session) {
        checkRegistrationPermission(session);

        String sql = """
                SELECT id, event_name, venue, starts_at, ends_at, status
                FROM events
                WHERE status IN ('UPCOMING', 'ONGOING')
                ORDER BY starts_at ASC, event_name ASC
                """;

        List<EventOption> events = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            while (results.next()) {
                events.add(new EventOption(
                        results.getLong("id"),
                        results.getString("event_name"),
                        results.getString("venue"),
                        results.getObject("starts_at", OffsetDateTime.class),
                        results.getObject("ends_at", OffsetDateTime.class),
                        results.getString("status")
                ));
            }

            return events;
        } catch (SQLException exception) {
            throw new RegistrationException(
                    "Unable to load events accepting registrations.",
                    exception
            );
        }
    }

    /**
     * Registers an attendee and issues their ticket in the same transaction.
     */
    public RegistrationResult register(
            Attendee attendee,
            UserSession session
    ) {
        checkRegistrationPermission(session);
        validateAttendee(attendee);

        RegistrationResult committedResult = null;
        boolean committed = false;

        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateEvent(connection, attendee.getEventId());

                RegistrationCapacityDAO.CapacitySnapshot capacity =
                        capacityDAO.lockAndGetCapacity(
                                connection,
                                attendee.getEventId()
                        );

                if (capacity == null) {
                    throw new RegistrationException(
                            "This event does not have an assigned room. "
                                    + "Assign a room before accepting registrations."
                    );
                }

                if (capacity.getRemainingCapacity() <= 0) {
                    throw new RegistrationException(
                            "Registration is full. The assigned room has reached "
                                    + "its capacity of " + capacity.capacity() + "."
                    );
                }

                long attendeeId = attendeeDAO.insert(connection, attendee);

                Ticket ticket = ticketService.issueTicket(
                        connection,
                        attendeeId,
                        session
                );

                connection.commit();
                committed = true;
                committedResult = new RegistrationResult(attendee, ticket);

                publishNotification(
                        "Attendee " + attendee.getFullName()
                                + " registered successfully. Ticket: "
                                + ticket.getTicketNumber()
                );

            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }

        } catch (RegistrationException | SecurityException exception) {
            throw exception;
        } catch (SQLException exception) {
            // The transaction is already durable. A close failure must not
            // make the caller believe registration failed.
            if (committed) {
                return committedResult;
            }

            if (isDuplicateAttendeeEmail(exception)) {
                throw new RegistrationException(
                        "This attendee is already registered for the event. "
                                + "Check the attendee's email address.",
                        exception
                );
            }

            throw new RegistrationException(
                    "Registration could not be completed: "
                            + exception.getMessage(),
                    exception
            );
        }

        return committedResult;
    }

    public List<Attendee> getAttendeesForEvent(
            long eventId,
            UserSession session
    ) {
        checkRegistrationPermission(session);
        requirePositiveEventId(eventId);

        try (Connection connection = connectionFactory.getConnection()) {
            return attendeeDAO.findByEvent(connection, eventId);
        } catch (SQLException exception) {
            throw new RegistrationException(
                    "Unable to load attendees for this event.",
                    exception
            );
        }
    }

    public List<Attendee> searchAttendees(
            long eventId,
            String searchTerm,
            UserSession session
    ) {
        checkRegistrationPermission(session);
        requirePositiveEventId(eventId);

        if (!ValidationUtil.isNotBlank(searchTerm)) {
            return getAttendeesForEvent(eventId, session);
        }

        try (Connection connection = connectionFactory.getConnection()) {
            return attendeeDAO.search(connection, eventId, searchTerm.trim());
        } catch (SQLException exception) {
            throw new RegistrationException(
                    "Unable to search attendees.",
                    exception
            );
        }
    }

    public Attendee getAttendeeById(
            long attendeeId,
            UserSession session
    ) {
        checkRegistrationPermission(session);
        requirePositiveAttendeeId(attendeeId);

        try (Connection connection = connectionFactory.getConnection()) {
            Attendee attendee = attendeeDAO.findById(connection, attendeeId);

            if (attendee == null) {
                throw new RegistrationException("Attendee was not found.");
            }

            return attendee;
        } catch (RegistrationException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new RegistrationException(
                    "Unable to retrieve attendee details.",
                    exception
            );
        }
    }

    /**
     * Updates attendee details without changing the event or issuing
     * another ticket.
     */
    public Attendee updateAttendee(
            Attendee attendee,
            UserSession session
    ) {
        checkRegistrationPermission(session);

        if (attendee == null || attendee.getId() == null) {
            throw new IllegalArgumentException(
                    "Select an existing attendee before updating."
            );
        }

        requirePositiveAttendeeId(attendee.getId());
        validateAttendee(attendee);

        Attendee committedAttendee = null;
        boolean committed = false;

        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Long existingEventId = lockAttendee(
                        connection,
                        attendee.getId()
                );

                if (existingEventId == null) {
                    throw new RegistrationException(
                            "The attendee no longer exists. Refresh the attendee list."
                    );
                }

                if (!existingEventId.equals(attendee.getEventId())) {
                    throw new RegistrationException(
                            "An attendee cannot be transferred to another event "
                                    + "through the edit operation."
                    );
                }

                boolean updated = attendeeDAO.update(connection, attendee);

                if (!updated) {
                    throw new RegistrationException(
                            "The attendee could not be updated."
                    );
                }

                connection.commit();
                committed = true;
                committedAttendee = attendee;

                publishNotification(
                        "Attendee details updated: " + attendee.getFullName()
                );

            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }

        } catch (RegistrationException | SecurityException exception) {
            throw exception;
        } catch (SQLException exception) {
            if (committed) {
                return committedAttendee;
            }

            if (isDuplicateAttendeeEmail(exception)) {
                throw new RegistrationException(
                        "Another attendee is already registered for this event "
                                + "using that email address.",
                        exception
                );
            }

            throw new RegistrationException(
                    "Attendee update failed: " + exception.getMessage(),
                    exception
            );
        }

        return committedAttendee;
    }

    /**
     * Deletes an attendee only when database relationships permit it.
     * An attendee with an issued ticket is protected by the ticket FK.
     */
    public void deleteAttendee(
            long attendeeId,
            long eventId,
            UserSession session
    ) {
        checkRegistrationPermission(session);
        requirePositiveAttendeeId(attendeeId);
        requirePositiveEventId(eventId);

        boolean committed = false;

        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Long existingEventId = lockAttendee(connection, attendeeId);

                if (existingEventId == null) {
                    throw new RegistrationException(
                            "The attendee no longer exists. Refresh the attendee list."
                    );
                }

                if (existingEventId != eventId) {
                    throw new RegistrationException(
                            "The selected attendee does not belong to this event."
                    );
                }

                boolean deleted = attendeeDAO.delete(connection, attendeeId);

                if (!deleted) {
                    throw new RegistrationException(
                            "The attendee could not be deleted."
                    );
                }

                connection.commit();
                committed = true;

                publishNotification(
                        "Attendee record deleted. Attendee ID: " + attendeeId
                );

            } catch (SQLException exception) {
                rollback(connection, exception);

                if ("23503".equals(exception.getSQLState())) {
                    throw new RegistrationException(
                            "This attendee has an issued ticket and cannot be "
                                    + "deleted. Keep the attendee record so the "
                                    + "ticket and attendance history remain valid.",
                            exception
                    );
                }

                throw exception;

            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }

        } catch (RegistrationException | SecurityException exception) {
            throw exception;
        } catch (SQLException exception) {
            if (committed) {
                return;
            }

            throw new RegistrationException(
                    "Attendee deletion failed: " + exception.getMessage(),
                    exception
            );
        }
    }

    /**
     * Locks the attendee row so concurrent edits/deletes cannot silently
     * change its event association during this transaction.
     */
    private Long lockAttendee(Connection connection, long attendeeId)
            throws SQLException {

        String sql = """
                SELECT event_id
                FROM attendees
                WHERE id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attendeeId);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }

                return results.getLong("event_id");
            }
        }
    }

    private void validateEvent(Connection connection, long eventId)
            throws SQLException {

        String sql = """
                SELECT status
                FROM events
                WHERE id = ?
                FOR SHARE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new RegistrationException(
                            "The selected event does not exist."
                    );
                }

                String status = results.getString("status");

                if (!"UPCOMING".equalsIgnoreCase(status)
                        && !"ONGOING".equalsIgnoreCase(status)) {
                    throw new RegistrationException(
                            "Registration is not available for an event "
                                    + "with status " + status + "."
                    );
                }
            }
        }
    }

    private void validateAttendee(Attendee attendee) {
        if (attendee == null) {
            throw new IllegalArgumentException("Attendee details are required.");
        }

        requirePositiveEventId(
                attendee.getEventId() == null ? 0 : attendee.getEventId()
        );

        if (!ValidationUtil.isNotBlank(attendee.getFullName())) {
            throw new IllegalArgumentException("Attendee full name is required.");
        }

        if (attendee.getFullName().trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Attendee name must not exceed 150 characters."
            );
        }

        String email = attendee.getEmail() == null ? "" : attendee.getEmail().trim();
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException(
                    "Enter a valid attendee email address."
            );
        }

        if (email.length() > 255) {
            throw new IllegalArgumentException(
                    "Attendee email must not exceed 255 characters."
            );
        }

        String phone = attendee.getPhone() == null
                ? ""
                : attendee.getPhone().trim();

        if (!phone.isBlank()) {
            if (!ValidationUtil.isValidPhone(phone)) {
                throw new IllegalArgumentException(
                        "Enter a valid phone number."
                );
            }

            if (phone.length() > 30) {
                throw new IllegalArgumentException(
                        "Phone number must not exceed 30 characters."
                );
            }

            attendee.setPhone(phone);
        } else {
            attendee.setPhone(null);
        }

        attendee.setFullName(attendee.getFullName().trim());
        attendee.setEmail(email.toLowerCase());

        if (attendee instanceof VipAttendee vipAttendee) {
            if (!ValidationUtil.isNotBlank(vipAttendee.getVipLevel())) {
                throw new IllegalArgumentException(
                        "VIP level is required for a VIP attendee."
                );
            }

            if (vipAttendee.getVipLevel().trim().length() > 50) {
                throw new IllegalArgumentException(
                        "VIP level must not exceed 50 characters."
                );
            }

            vipAttendee.setVipLevel(vipAttendee.getVipLevel().trim());

            if (vipAttendee.getSpecialRequirements() != null) {
                String requirements =
                        vipAttendee.getSpecialRequirements().trim();

                vipAttendee.setSpecialRequirements(
                        requirements.isEmpty() ? null : requirements
                );
            }
        }
    }

    private void checkRegistrationPermission(UserSession session) {
        if (session == null
                || !(session.hasRole("ADMIN")
                || session.hasRole("ORGANIZER")
                || session.hasRole("STAFF"))) {
            throw new SecurityException(
                    "You do not have permission to manage attendee registrations."
            );
        }
    }

    private void requirePositiveEventId(long eventId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("A valid event ID is required.");
        }
    }

    private void requirePositiveAttendeeId(long attendeeId) {
        if (attendeeId <= 0) {
            throw new IllegalArgumentException("A valid attendee ID is required.");
        }
    }

    /**
     * Identifies only the attendee email uniqueness rule. Other unique
     * violations, including ticket-number collisions, must not be reported
     * as duplicate attendee registrations.
     */
    private boolean isDuplicateAttendeeEmail(SQLException exception) {
        for (SQLException current = exception;
             current != null;
             current = current.getNextException()) {

            if (!(current instanceof PSQLException postgresException)
                    || !"23505".equals(postgresException.getSQLState())) {
                continue;
            }

            ServerErrorMessage serverError =
                    postgresException.getServerErrorMessage();

            if (serverError != null
                    && "uq_attendees_event_email".equals(
                            serverError.getConstraint()
                    )) {
                return true;
            }
        }

        return false;
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private void publishNotification(String message) {
        try {
            NotificationUtil.publish(NotificationUtil.Level.INFO, message);
        } catch (RuntimeException ignored) {
            // A notification failure must not make a committed operation fail.
        }
    }

    @FunctionalInterface
    public interface ConnectionFactory {
        Connection getConnection() throws SQLException;
    }

    public record RegistrationResult(Attendee attendee, Ticket ticket) {
    }

    public record EventOption(
            long id,
            String name,
            String venue,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String status
    ) {
        @Override
        public String toString() {
            return name + " — " + venue + " (" + status + ")";
        }
    }

    public static class RegistrationException extends RuntimeException {
        public RegistrationException(String message) {
            super(message);
        }

        public RegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
