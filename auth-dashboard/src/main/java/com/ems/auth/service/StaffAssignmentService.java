package com.ems.auth.service;

import com.ems.auth.dao.StaffAssignmentDAO;
import com.ems.auth.model.StaffAssignment;
import com.ems.shared.model.UserSession;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Staff duty assignment operations with role checks in the service layer. */
public class StaffAssignmentService {

    private final StaffAssignmentDAO assignmentDAO;

    public StaffAssignmentService() {
        this(new StaffAssignmentDAO());
    }

    public StaffAssignmentService(StaffAssignmentDAO assignmentDAO) {
        this.assignmentDAO = Objects.requireNonNull(assignmentDAO, "assignmentDAO");
    }

    public StaffAssignment assign(UserSession organizer, long staffId,
                                  Long eventId, Long roomId, String duty) {
        requireOrganizer(organizer);
        if (staffId <= 0 || (eventId != null && eventId <= 0) || (roomId != null && roomId <= 0)) {
            throw new AssignmentException("Choose valid staff, event, and room values.");
        }
        if (eventId == null && roomId == null) {
            throw new AssignmentException("Choose an event, a room, or both.");
        }
        if (duty == null || duty.isBlank() || duty.trim().length() > 500) {
            throw new AssignmentException("Duty details are required and must be at most 500 characters.");
        }
        try {
            return assignmentDAO.create(staffId, eventId, roomId, duty.trim(), organizer.getUserId());
        } catch (SQLException exception) {
            throw new AssignmentException(
                    "Unable to save the assignment. Check that the staff member, event, and room exist.",
                    exception);
        }
    }

    public List<StaffAssignment> findAll(UserSession organizer) {
        requireOrganizer(organizer);
        try {
            return assignmentDAO.findAll();
        } catch (SQLException exception) {
            throw new AssignmentException("Unable to load staff assignments.", exception);
        }
    }

    public List<StaffAssignment> findMyAssignments(UserSession staff) {
        requireStaff(staff);
        try {
            return assignmentDAO.findForStaff(staff.getUserId());
        } catch (SQLException exception) {
            throw new AssignmentException("Unable to load your assigned duties.", exception);
        }
    }

    public void remove(UserSession organizer, long assignmentId) {
        requireOrganizer(organizer);
        try {
            if (!assignmentDAO.delete(assignmentId)) {
                throw new AssignmentException("That assignment no longer exists.");
            }
        } catch (SQLException exception) {
            throw new AssignmentException("Unable to remove the assignment.", exception);
        }
    }

    public List<StaffAssignmentDAO.StaffOption> getActiveStaff(UserSession organizer) {
        requireOrganizer(organizer);
        try {
            return assignmentDAO.findActiveStaff();
        } catch (SQLException exception) {
            throw new AssignmentException("Unable to load active staff accounts.", exception);
        }
    }

    public List<StaffAssignmentDAO.EventOption> getEvents(UserSession organizer) {
        requireOrganizer(organizer);
        try {
            return assignmentDAO.findEvents();
        } catch (SQLException exception) {
            throw new AssignmentException("Unable to load events.", exception);
        }
    }

    public List<StaffAssignmentDAO.RoomOption> getRooms(UserSession organizer) {
        requireOrganizer(organizer);
        try {
            return assignmentDAO.findRooms();
        } catch (SQLException exception) {
            throw new AssignmentException("Unable to load rooms.", exception);
        }
    }

    private void requireOrganizer(UserSession session) {
        if (session == null || (!session.hasRole("ORGANIZER") && !session.hasRole("ADMIN"))) {
            throw new AssignmentException("Only admins or organizers can manage staff assignments.");
        }
    }

    private void requireStaff(UserSession session) {
        if (session == null || (!session.hasRole("STAFF") && !session.hasRole("ADMIN"))) {
            throw new AssignmentException("Only staff or admins can view assigned duties.");
        }
    }

    public static final class AssignmentException extends RuntimeException {
        public AssignmentException(String message) { super(message); }
        public AssignmentException(String message, Throwable cause) { super(message, cause); }
    }
}
