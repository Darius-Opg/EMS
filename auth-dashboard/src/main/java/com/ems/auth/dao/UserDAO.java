package com.ems.auth.dao;

import com.ems.auth.model.Organizer;
import com.ems.auth.model.Admin;
import com.ems.auth.model.Staff;
import com.ems.auth.model.User;
import com.ems.shared.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Database operations used by authentication. */
public class UserDAO {

    private static final String FIND_FOR_AUTHENTICATION = """
            SELECT id, full_name, username, role, password_hash, is_active
            FROM users
            WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)
            ORDER BY CASE WHEN LOWER(username) = LOWER(?) THEN 0 ELSE 1 END
            LIMIT 1
            """;

    public Optional<AuthenticationAccount> findForAuthentication(String identifier)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_FOR_AUTHENTICATION)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            statement.setString(3, identifier);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(new AuthenticationAccount(
                        resultSet.getLong("id"),
                        resultSet.getString("full_name"),
                        resultSet.getString("username"),
                        resultSet.getString("role"),
                        resultSet.getString("password_hash"),
                        resultSet.getBoolean("is_active")));
            }
        }
    }

    public List<User> findAll(String search) throws SQLException {
        String query = """
                SELECT id, full_name, username, email, role, is_active
                FROM users
                WHERE (? = '' OR username ILIKE ? OR full_name ILIKE ?)
                ORDER BY LOWER(full_name), id
                """;
        String pattern = "%" + search.trim() + "%";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, search.trim());
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet results = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (results.next()) {
                    users.add(readUser(results));
                }
                return users;
            }
        }
    }

    public User create(String fullName, String username, String email,
                       String passwordHash, String role) throws SQLException {
        String query = """
                INSERT INTO users (full_name, username, email, password_hash, role)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id, full_name, username, email, role, is_active
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, fullName);
            statement.setString(2, username);
            statement.setString(3, email);
            statement.setString(4, passwordHash);
            statement.setString(5, role);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("The new account was not returned by the database.");
                }
                return readUser(results);
            }
        }
    }

    public User update(long id, String fullName, String username, String email, String role)
            throws SQLException {
        String query = """
                UPDATE users
                SET full_name = ?, username = ?, email = ?, role = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                RETURNING id, full_name, username, email, role, is_active
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, fullName);
            statement.setString(2, username);
            statement.setString(3, email);
            statement.setString(4, role);
            statement.setLong(5, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? readUser(results) : null;
            }
        }
    }

    public boolean setActive(long id, boolean active) throws SQLException {
        String query = """
                UPDATE users
                SET is_active = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setBoolean(1, active);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    private User readUser(ResultSet results) throws SQLException {
        long id = results.getLong("id");
        String fullName = results.getString("full_name");
        String username = results.getString("username");
        String email = results.getString("email");
        String role = results.getString("role");
        boolean active = results.getBoolean("is_active");

        return switch (role) {
            case "ADMIN" -> new Admin(id, fullName, username, email, active);
            case "ORGANIZER" -> new Organizer(id, fullName, username, email, active);
            case "STAFF" -> new Staff(id, fullName, username, email, active);
            default -> throw new SQLException("Unsupported user role in database: " + role);
        };
    }

    public record AuthenticationAccount(
            long id,
            String fullName,
            String username,
            String role,
            String passwordHash,
            boolean active) {
    }
}
