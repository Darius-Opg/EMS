package com.ems.shared.config;



import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:postgresql://localhost:5432/event_management_system";

    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private DatabaseConnection() {
    }

    private static String getConfig(String key) {
        String value = System.getenv(key);

        if (value == null || value.isBlank()) {
            value = DOTENV.get(key);
        }

        return value;
    }

    public static Connection getConnection() throws SQLException {

        String url = getConfig("EMS_DB_URL");
        String username = getConfig("EMS_DB_USER");
        String password = getConfig("EMS_DB_PASSWORD");

        if (url == null || url.isBlank()) {
            url = DEFAULT_URL;
        }

        if (username == null || username.isBlank()) {
            throw new SQLException(
                    "Missing configuration: EMS_DB_USER"
            );
        }

        if (password == null || password.isBlank()) {
            throw new SQLException(
                    "Missing configuration: EMS_DB_PASSWORD"
            );
        }

        return DriverManager.getConnection(
                url,
                username,
                password
        );
    }
}
