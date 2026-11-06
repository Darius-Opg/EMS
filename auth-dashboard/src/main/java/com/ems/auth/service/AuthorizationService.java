package com.ems.auth.service;

import com.ems.shared.model.UserSession;

import java.util.ArrayList;
import java.util.List;

/** Central role-to-feature access policy for auth-dashboard navigation and services. */
public final class AuthorizationService {

    public boolean canAccess(UserSession session, Feature feature) {
        if (session == null || feature == null) {
            return false;
        }
        if (session.hasRole("ADMIN")) {
            return true;
        }
        if (session.hasRole("ORGANIZER")) {
            return switch (feature) {
                case DASHBOARD, EVENTS, VENDORS, ATTENDEES, TICKETS, CHECK_IN,
                        ROOMS, STAFF_ASSIGNMENTS, ATTENDANCE_REPORTS, NOTIFICATIONS -> true;
                case USER_MANAGEMENT, MY_DUTIES -> false;
            };
        }
        if (!session.hasRole("STAFF")) return false;
        return switch (feature) {
            case DASHBOARD, ATTENDEES, CHECK_IN, MY_DUTIES, NOTIFICATIONS -> true;
            case USER_MANAGEMENT, EVENTS, VENDORS, TICKETS, ROOMS,
                    STAFF_ASSIGNMENTS, ATTENDANCE_REPORTS -> false;
        };
    }

    public void requireAccess(UserSession session, Feature feature) {
        if (!canAccess(session, feature)) {
            throw new AccessDeniedException("You do not have permission to access "
                    + feature.getLabel() + ".");
        }
    }

    public List<Feature> availableFeatures(UserSession session) {
        List<Feature> available = new ArrayList<>();
        for (Feature feature : Feature.values()) {
            if (canAccess(session, feature)) {
                available.add(feature);
            }
        }
        return List.copyOf(available);
    }

    public enum Feature {
        DASHBOARD("Dashboard"),
        USER_MANAGEMENT("User accounts"),
        EVENTS("Event management"),
        VENDORS("Vendor management"),
        ATTENDEES("Attendee management"),
        TICKETS("Ticket management"),
        CHECK_IN("Check-in"),
        ROOMS("Room management"),
        STAFF_ASSIGNMENTS("Staff assignments"),
        MY_DUTIES("My duties"),
        ATTENDANCE_REPORTS("Attendance reports"),
        NOTIFICATIONS("Notifications");

        private final String label;

        Feature(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static final class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
