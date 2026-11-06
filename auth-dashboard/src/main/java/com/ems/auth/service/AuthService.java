package com.ems.auth.service;

import com.ems.auth.dao.UserDAO;
import com.ems.shared.model.UserSession;

import java.sql.SQLException;
import java.util.Objects;

/** Authentication rules and the active session for this application instance. */
public class AuthService {

    private final UserDAO userDAO;
    private volatile UserSession currentSession;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO");
    }

    public synchronized UserSession login(String identifier, char[] password) {
        currentSession = null;
        if (identifier == null || identifier.isBlank() || password == null || password.length == 0) {
            throw new AuthenticationException(Failure.INVALID_CREDENTIALS,
                    "Enter your username or email and password.");
        }

        try {
            UserDAO.AuthenticationAccount account = userDAO.findForAuthentication(identifier.trim())
                    .orElseThrow(() -> new AuthenticationException(
                            Failure.INVALID_CREDENTIALS, "Invalid username/email or password."));

            if (!PasswordHash.verify(password, account.passwordHash())) {
                throw new AuthenticationException(
                        Failure.INVALID_CREDENTIALS, "Invalid username/email or password.");
            }
            if (!account.active()) {
                throw new AuthenticationException(
                        Failure.INACTIVE_ACCOUNT,
                        "This account is inactive. Contact an organizer for assistance.");
            }

            currentSession = new UserSession(
                    account.id(), account.fullName(), account.username(), account.role());
            return currentSession;
        } catch (SQLException exception) {
            throw new AuthenticationException(
                    Failure.SERVICE_UNAVAILABLE,
                    "Unable to reach the user database. Check your connection and try again.",
                    exception);
        }
    }

    public UserSession getCurrentSession() {
        return currentSession;
    }

    public synchronized void logout() {
        currentSession = null;
    }

    public enum Failure {
        INVALID_CREDENTIALS,
        INACTIVE_ACCOUNT,
        SERVICE_UNAVAILABLE
    }

    public static final class AuthenticationException extends RuntimeException {
        private final Failure failure;

        public AuthenticationException(Failure failure, String message) {
            super(message);
            this.failure = failure;
        }

        public AuthenticationException(Failure failure, String message, Throwable cause) {
            super(message, cause);
            this.failure = failure;
        }

        public Failure getFailure() {
            return failure;
        }
    }
}
