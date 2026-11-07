
package com.ems.events.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.ems.events.dao.EventDAO;
import com.ems.events.dao.VendorDAO;
import com.ems.events.model.Event;
import com.ems.events.model.Vendor;
import com.ems.shared.model.UserSession;

public class EventService {

    private final EventDAO eventDAO; private final VendorDAO vendorDAO; 
    private final UserSession session;

    private static final Set<String> VALID_STATUSES = Set.of(
            "UPCOMING", "ONGOING", "COMPLETED", "CANCELLED"
    );

    // Required by ModulePanelLoader.
    public EventService() {
    this.eventDAO = new EventDAO();
    this.vendorDAO = new VendorDAO();
    this.session = null;
}

    public EventService(UserSession session) {
        this.eventDAO = new EventDAO();
        this.vendorDAO = new VendorDAO();
        this.session = session;
    }

    // CREATE
    public long createEvent(Event event) throws SQLException {
        return createEvent(event, requireSession());
    }

    public long createEvent(Event event, UserSession actor) throws SQLException {
        authorize(actor);
        validateEvent(event);

        event.setCreatedBy(actor.getUserId());
        event.setStatus(normalizeStatus(event.getStatus()));

        return eventDAO.create(event);
    }

    // UPDATE
    public boolean updateEvent(Event event) throws SQLException {
        return updateEvent(event, requireSession());
    }

    public boolean updateEvent(Event event, UserSession actor) throws SQLException {
        authorize(actor);
        validateEvent(event);

        if (event.getEventId() <= 0) {
            throw new IllegalArgumentException("Invalid event ID.");
        }

        event.setStatus(normalizeStatus(event.getStatus()));
        return eventDAO.update(event);
    }

    // DELETE
    public boolean deleteEvent(long eventId) throws SQLException {
        return deleteEvent(eventId, requireSession());
    }

    public boolean deleteEvent(long eventId, UserSession actor) throws SQLException {
        authorize(actor);

        if (eventId <= 0) {
            throw new IllegalArgumentException("Invalid event ID.");
        }

        try {
            return eventDAO.delete(eventId);
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                throw new SQLException(
                        "This event cannot be deleted because it is linked to attendees, rooms, or staff assignments.",
                        e
                );
            }
            throw e;
        }
    }

    // READ ONE
    public Optional<Event> getEventById(long eventId) throws SQLException {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Invalid event ID.");
        }
        return eventDAO.findById(eventId);
    }

    // READ ALL
    public List<Event> getAllEvents() throws SQLException {
        return eventDAO.findAll();
    }

    // FILTER BY STATUS
    public List<Event> getEventsByStatus(String status) throws SQLException {
        return eventDAO.findByStatus(normalizeStatus(status));
    }

    // SEARCH EVENTS
    public List<Event> searchEvents(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return getAllEvents();
        }

        String search = keyword.trim().toLowerCase(Locale.ROOT);

        return getAllEvents().stream()
                .filter(event ->
                        contains(event.getName(), search)
                        || contains(event.getDescription(), search)
                        || contains(event.getVenue(), search))
                .toList();
    }

    
    // VENDOR MANAGEMENT

    public List<Vendor> getAllVendors() throws SQLException {
        return vendorDAO.findAll();
    }

    public List<Vendor> getVendorsByEventId(long eventId) throws SQLException {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Invalid event ID.");
        }

        return vendorDAO.findByEventId(eventId);
    }

    public List<Vendor> searchVendors(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return getAllVendors();
        }

        String search = keyword.trim().toLowerCase(Locale.ROOT);

        return getAllVendors().stream()
                .filter(v ->
                        contains(v.getVendorName(), search)
                        || contains(v.getServiceType(), search)
                        || contains(v.getContactPerson(), search)
                        || contains(v.getPhone(), search)
                        || contains(v.getEmail(), search))
                .toList();
    }

    public long createVendor(Vendor vendor, UserSession actor) throws SQLException {
        authorize(actor);
        validateVendor(vendor);

        if (eventDAO.findById(vendor.getEventId()).isEmpty()) {
            throw new IllegalArgumentException("The selected event does not exist.");
        }

        return vendorDAO.create(vendor);
    }

    public boolean updateVendor(Vendor vendor, UserSession actor) throws SQLException {
        authorize(actor);
        validateVendor(vendor);

        if (vendor.getVendorId() <= 0) {
            throw new IllegalArgumentException("Invalid vendor ID.");
        }

        if (eventDAO.findById(vendor.getEventId()).isEmpty()) {
            throw new IllegalArgumentException("The selected event does not exist.");
        }

        return vendorDAO.update(vendor);
    }

    public boolean deleteVendor(long vendorId, UserSession actor) throws SQLException {
        authorize(actor);

        if (vendorId <= 0) {
            throw new IllegalArgumentException("Invalid vendor ID.");
        }

        return vendorDAO.delete(vendorId);
    }

    private void validateVendor(Vendor vendor) {
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor details are required.");
        }

        if (vendor.getEventId() <= 0) {
            throw new IllegalArgumentException("An event must be selected.");
        }

        if (vendor.getVendorName() == null || vendor.getVendorName().isBlank()) {
            throw new IllegalArgumentException("Vendor name is required.");
        }

        if (vendor.getVendorName().trim().length() > 150) {
            throw new IllegalArgumentException("Vendor name cannot exceed 150 characters.");
        }

        if (vendor.getServiceType() == null || vendor.getServiceType().isBlank()) {
            throw new IllegalArgumentException("Service type is required.");
        }

        if (vendor.getServiceType().trim().length() > 100) {
            throw new IllegalArgumentException("Service type cannot exceed 100 characters.");
        }

        if (vendor.getContactPerson() != null && vendor.getContactPerson().length() > 120) {
            throw new IllegalArgumentException("Contact person cannot exceed 120 characters.");
        }

        if (vendor.getPhone() != null && vendor.getPhone().length() > 30) {
            throw new IllegalArgumentException("Phone number cannot exceed 30 characters.");
        }

        if (vendor.getEmail() != null && vendor.getEmail().length() > 255) {
            throw new IllegalArgumentException("Email cannot exceed 255 characters.");
        }
    }

    // AUTHORIZATION
    private void authorize(UserSession actor) {
        if (actor == null) {
            throw new SecurityException("An authenticated user session is required.");
        }

        if (!actor.hasRole("ADMIN") && !actor.hasRole("ORGANIZER")) {
            throw new SecurityException(
                    "Only administrators and organizers can manage events."
            );
        }
    }

    private UserSession requireSession() {
        if (session == null) {
            throw new SecurityException("An authenticated user session is required.");
        }
        return session;
    }

    // VALIDATION
    private void validateEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be empty.");
        }

        if (event.getName() == null || event.getName().isBlank()) {
            throw new IllegalArgumentException("Event name is required.");
        }

        if (event.getName().trim().length() > 150) {
            throw new IllegalArgumentException("Event name cannot exceed 150 characters.");
        }

        if (event.getVenue() == null || event.getVenue().isBlank()) {
            throw new IllegalArgumentException("Event venue is required.");
        }

        if (event.getVenue().trim().length() > 200) {
            throw new IllegalArgumentException("Venue cannot exceed 200 characters.");
        }

        if (event.getStartDateTime() == null || event.getEndDateTime() == null) {
            throw new IllegalArgumentException("Event start and end dates are required.");
        }

        if (!event.getEndDateTime().isAfter(event.getStartDateTime())) {
            throw new IllegalArgumentException("Event end date must be after its start date.");
        }

        normalizeStatus(event.getStatus());
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Event status is required.");
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid event status. Use UPCOMING, ONGOING, COMPLETED, or CANCELLED."
            );
        }

        return normalized;
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }
}
