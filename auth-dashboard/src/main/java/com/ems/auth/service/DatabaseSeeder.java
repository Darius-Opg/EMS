package com.ems.auth.service;

import com.ems.shared.config.DatabaseConnection;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/** Applies the configured SQL seed file and creates the initial admin account. */
public final class DatabaseSeeder {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    private DatabaseSeeder() {
    }

    public static void main(String[] args) throws IOException, SQLException {
        String seedPath = config("EMS_DB_SEED_FILE");
        String fullName = config("EMS_ADMIN_FULL_NAME");
        String username = config("EMS_ADMIN_USERNAME");
        String email = config("EMS_ADMIN_EMAIL");
        String role = config("EMS_ADMIN_ROLE");
        String configuredPassword = config("EMS_ADMIN_PASSWORD");
        if (isBlank(seedPath) || isBlank(fullName) || isBlank(username)
                || isBlank(email) || isBlank(configuredPassword)) {
            throw new IllegalStateException("Set EMS_DB_SEED_FILE and all EMS_ADMIN_* values in .env.");
        }
        if (configuredPassword.equals("change_this_immediately")) {
            throw new IllegalStateException("Set a private EMS_ADMIN_PASSWORD in .env before seeding.");
        }
        if (configuredPassword.length() < 8) {
            throw new IllegalStateException("EMS_ADMIN_PASSWORD must contain at least 8 characters.");
        }
        role = isBlank(role) ? "ADMIN" : role.trim().toUpperCase(java.util.Locale.ROOT);
        if (!role.equals("ADMIN") && !role.equals("ORGANIZER") && !role.equals("STAFF")) {
            throw new IllegalStateException("EMS_ADMIN_ROLE must be ADMIN, ORGANIZER, or STAFF.");
        }

        Path seedFile = Path.of(seedPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(seedFile) || !Files.isReadable(seedFile)) {
            throw new IOException("Seed SQL file is missing or unreadable: " + seedFile);
        }
        String sql = Files.readString(seedFile).replaceAll("(?m)--[^\\r\\n]*", "");
        char[] password = configuredPassword.toCharArray();
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                upsertInitialUser(connection, fullName.trim(), username.trim(),
                        email.trim(), PasswordHash.create(password), role);
                try (Statement statement = connection.createStatement()) {
                    for (String command : sql.split(";")) {
                        String trimmed = command.trim();
                        if (!trimmed.isEmpty()) {
                            statement.execute(trimmed);
                        }
                    }
                }
                connection.commit();
                System.out.println("Seed file and initial user applied with role " + role + ".");
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static void upsertInitialUser(Connection connection, String fullName,
                                           String username, String email, String hash, String role)
            throws SQLException {
        String sql = """
                INSERT INTO users (full_name, username, email, password_hash, role)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (LOWER(username)) DO UPDATE
                SET full_name = EXCLUDED.full_name,
                    email = EXCLUDED.email,
                    password_hash = EXCLUDED.password_hash,
                    role = EXCLUDED.role,
                    is_active = TRUE,
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fullName);
            statement.setString(2, username);
            statement.setString(3, email);
            statement.setString(4, hash);
            statement.setString(5, role);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Unable to create or update the configured initial user.");
            }
        }
    }

    private static String config(String key) {
        String value = System.getenv(key);
        return isBlank(value) ? DOTENV.get(key) : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
