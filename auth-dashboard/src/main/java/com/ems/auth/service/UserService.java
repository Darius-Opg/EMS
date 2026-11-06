package com.ems.auth.service;

import com.ems.auth.dao.UserDAO;
import com.ems.auth.model.User;
import com.ems.shared.model.UserSession;
import com.ems.shared.utils.ValidationUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Account validation and user-management operations. */
public class UserService {

    private final UserDAO userDAO;
    private final AuthorizationService authorizationService = new AuthorizationService();

    public UserService() {
        this(new UserDAO());
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO");
    }

    public User register(UserSession actor, String fullName, String username, String email,
                         char[] password, String role) {
        requireOrganizer(actor);
        String normalizedRole = normalizeRole(role);
        validateUserFields(fullName, username, email);
        if (password == null || password.length < 8) {
            throw new UserManagementException("Password must contain at least 8 characters.");
        }

        try {
            return userDAO.create(fullName.trim(), username.trim(), email.trim(),
                    PasswordHash.create(password), normalizedRole);
        } catch (SQLException exception) {
            throw mapDatabaseError(exception);
        }
    }

    public List<User> findUsers(UserSession actor, String search) {
        requireOrganizer(actor);
        try {
            return userDAO.findAll(search == null ? "" : search.trim());
        } catch (SQLException exception) {
            throw new UserManagementException("Unable to load user accounts.", exception);
        }
    }

    public User updateUser(UserSession actor, long id, String fullName, String username,
                           String email, String role) {
        requireOrganizer(actor);
        String normalizedRole = normalizeRole(role);
        validateUserFields(fullName, username, email);
        try {
            User updated = userDAO.update(id, fullName.trim(), username.trim(), email.trim(),
                    normalizedRole);
            if (updated == null) {
                throw new UserManagementException("The selected account no longer exists.");
            }
            return updated;
        } catch (SQLException exception) {
            throw mapDatabaseError(exception);
        }
    }

    public void setActive(UserSession actor, long id, boolean active) {
        requireOrganizer(actor);
        try {
            if (!userDAO.setActive(id, active)) {
                throw new UserManagementException("The selected account no longer exists.");
            }
        } catch (SQLException exception) {
            throw new UserManagementException("Unable to update the account status.", exception);
        }
    }

    private void requireOrganizer(UserSession actor) {
        authorizationService.requireAccess(actor, AuthorizationService.Feature.USER_MANAGEMENT);
    }

    private void validateUserFields(String fullName, String username, String email) {
        if (!ValidationUtil.isNotBlank(fullName) || fullName.trim().length() > 120) {
            throw new UserManagementException("Full name is required and must be at most 120 characters.");
        }
        if (!ValidationUtil.isNotBlank(username) || username.trim().length() > 50) {
            throw new UserManagementException("Username is required and must be at most 50 characters.");
        }
        if (!ValidationUtil.isValidEmail(email) || email.trim().length() > 255) {
            throw new UserManagementException("Enter a valid email address (up to 255 characters).");
        }
    }

    private String normalizeRole(String role) {
        if (role == null) {
            throw new UserManagementException("Choose an account role.");
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("ADMIN") && !normalized.equals("ORGANIZER")
                && !normalized.equals("STAFF")) {
            throw new UserManagementException("Role must be ADMIN, ORGANIZER, or STAFF.");
        }
        return normalized;
    }

    private UserManagementException mapDatabaseError(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            if ("23505".equals(current.getSQLState())) {
                String detail = current.getMessage() == null ? "" : current.getMessage();
                if (detail.contains("uq_users_username_lower")) {
                    return new UserManagementException("That username is already in use.", exception);
                }
                if (detail.contains("uq_users_email_lower")) {
                    return new UserManagementException("That email address is already in use.", exception);
                }
                return new UserManagementException("A username or email address is already in use.", exception);
            }
        }
        return new UserManagementException("Unable to save the user account.", exception);
    }

    public static final class UserManagementException extends RuntimeException {
        public UserManagementException(String message) {
            super(message);
        }

        public UserManagementException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
