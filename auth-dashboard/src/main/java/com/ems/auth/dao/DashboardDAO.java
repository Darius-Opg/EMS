package com.ems.auth.dao;

import com.ems.shared.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-only aggregate queries used by the application dashboard. */
public class DashboardDAO {

    private static final String COUNTS = """
            SELECT
                (SELECT COUNT(*) FROM events WHERE status <> 'CANCELLED') AS event_count,
                (SELECT COUNT(*) FROM events
                    WHERE status = 'UPCOMING' AND starts_at >= CURRENT_TIMESTAMP) AS upcoming_count,
                (SELECT COUNT(*) FROM attendees) AS attendee_count,
                (SELECT COUNT(*) FROM tickets WHERE status = 'CHECKED_IN') AS checked_in_count
            """;

    private static final String UPCOMING_EVENTS = """
            SELECT event_name, venue, starts_at
            FROM events
            WHERE status = 'UPCOMING' AND starts_at >= CURRENT_TIMESTAMP
            ORDER BY starts_at
            LIMIT 5
            """;

    public DashboardSummary loadSummary() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNTS);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Dashboard summary query returned no row.");
            }
            return new DashboardSummary(result.getLong("event_count"),
                    result.getLong("upcoming_count"), result.getLong("attendee_count"),
                    result.getLong("checked_in_count"), loadUpcomingEvents(connection));
        }
    }

    private List<UpcomingEvent> loadUpcomingEvents(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPCOMING_EVENTS);
             ResultSet results = statement.executeQuery()) {
            List<UpcomingEvent> events = new ArrayList<>();
            while (results.next()) {
                events.add(new UpcomingEvent(results.getString("event_name"),
                        results.getString("venue"),
                        results.getTimestamp("starts_at").toLocalDateTime()));
            }
            return List.copyOf(events);
        }
    }

    public record DashboardSummary(long eventCount, long upcomingEventCount,
                                   long attendeeCount, long checkedInCount,
                                   List<UpcomingEvent> upcomingEvents) {
    }

    public record UpcomingEvent(String name, String venue,
                                java.time.LocalDateTime startsAt) {
    }
}
