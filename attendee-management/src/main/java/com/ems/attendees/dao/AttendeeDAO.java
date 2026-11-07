package com.ems.attendees.dao;

import com.ems.attendees.model.Attendee;
import com.ems.attendees.model.VipAttendee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AttendeeDAO {

    private static final String SELECT_COLUMNS = """
            SELECT
                id,
                event_id,
                full_name,
                email,
                phone,
                is_vip,
                vip_level,
                special_requirements,
                registered_at
            FROM attendees
            """;

    /**
     * Inserts an attendee using a caller-managed connection.
     *
     * The caller owns the transaction and connection lifecycle.
     * Returns the generated attendee ID after a successful insert.
     */
    public long insert(Connection connection, Attendee attendee)
            throws SQLException {

        if (connection == null || attendee == null) {
            throw new IllegalArgumentException(
                    "Connection and attendee are required."
            );
        }

        String sql = """
                INSERT INTO attendees (
                    event_id,
                    full_name,
                    email,
                    phone,
                    is_vip,
                    vip_level,
                    special_requirements
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id, registered_at
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, attendee.getEventId());
            statement.setString(2, attendee.getFullName());
            statement.setString(3, attendee.getEmail());
            statement.setString(4, attendee.getPhone());
            statement.setBoolean(5, attendee.isVip());

            if (attendee instanceof VipAttendee vipAttendee) {
                statement.setString(6, vipAttendee.getVipLevel());
                statement.setString(
                        7,
                        vipAttendee.getSpecialRequirements()
                );
            } else {
                statement.setNull(6, Types.VARCHAR);
                statement.setNull(7, Types.VARCHAR);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException(
                            "Attendee insertion did not return a generated ID."
                    );
                }

                attendee.setId(resultSet.getLong("id"));
                attendee.setRegisteredAt(
                        resultSet.getObject(
                                "registered_at",
                                java.time.OffsetDateTime.class
                        )
                );

                return attendee.getId();
            }
        }
    }

    /**
     * Retrieves an attendee by its ID.
     */
    public Attendee findById(Connection connection, long attendeeId)
            throws SQLException {

        String sql = SELECT_COLUMNS + " WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attendeeId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAttendee(resultSet);
                }
            }
        }

        return null;
    }

    /**
     * Retrieves all attendees registered for a particular event.
     */
    public List<Attendee> findByEvent(Connection connection, long eventId)
            throws SQLException {

        String sql = SELECT_COLUMNS
                + " WHERE event_id = ? ORDER BY registered_at DESC, id DESC";

        List<Attendee> attendees = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    attendees.add(mapAttendee(resultSet));
                }
            }
        }

        return attendees;
    }

    /**
     * Searches attendees by name, email, or phone within an event.
     */
    public List<Attendee> search(
            Connection connection,
            long eventId,
            String searchTerm
    ) throws SQLException {

        String sql = SELECT_COLUMNS + """
                 WHERE event_id = ?
                   AND (
                       full_name ILIKE ?
                       OR email ILIKE ?
                       OR phone ILIKE ?
                   )
                 ORDER BY registered_at DESC, id DESC
                """;

        List<Attendee> attendees = new ArrayList<>();
        String pattern = "%" + searchTerm.trim() + "%";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    attendees.add(mapAttendee(resultSet));
                }
            }
        }

        return attendees;
    }

    /**
     * Updates an existing attendee.
     */
    public boolean update(Connection connection, Attendee attendee)
            throws SQLException {

        if (attendee == null || attendee.getId() == null) {
            throw new IllegalArgumentException(
                    "An attendee with an ID is required."
            );
        }

        String sql = """
                UPDATE attendees
                SET
                    event_id = ?,
                    full_name = ?,
                    email = ?,
                    phone = ?,
                    is_vip = ?,
                    vip_level = ?,
                    special_requirements = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, attendee.getEventId());
            statement.setString(2, attendee.getFullName());
            statement.setString(3, attendee.getEmail());
            statement.setString(4, attendee.getPhone());
            statement.setBoolean(5, attendee.isVip());

            if (attendee instanceof VipAttendee vipAttendee) {
                statement.setString(6, vipAttendee.getVipLevel());
                statement.setString(
                        7,
                        vipAttendee.getSpecialRequirements()
                );
            } else {
                statement.setNull(6, Types.VARCHAR);
                statement.setNull(7, Types.VARCHAR);
            }

            statement.setLong(8, attendee.getId());

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Deletes an attendee where database constraints permit it.
     *
     * An attendee with an existing ticket cannot be deleted because
     * the tickets foreign key uses ON DELETE RESTRICT.
     */
    public boolean delete(Connection connection, long attendeeId)
            throws SQLException {

        String sql = "DELETE FROM attendees WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attendeeId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Maps a database record to the appropriate attendee model.
     */
    private Attendee mapAttendee(ResultSet resultSet) throws SQLException {

        boolean isVip = resultSet.getBoolean("is_vip");

        Attendee attendee;

        if (isVip) {
            VipAttendee vipAttendee = new VipAttendee();

            vipAttendee.setVipLevel(
                    resultSet.getString("vip_level")
            );

            vipAttendee.setSpecialRequirements(
                    resultSet.getString("special_requirements")
            );

            attendee = vipAttendee;
        } else {
            attendee = new Attendee();
        }

        attendee.setId(resultSet.getLong("id"));
        attendee.setEventId(resultSet.getLong("event_id"));
        attendee.setFullName(resultSet.getString("full_name"));
        attendee.setEmail(resultSet.getString("email"));
        attendee.setPhone(resultSet.getString("phone"));
        attendee.setRegisteredAt(
                resultSet.getObject(
                        "registered_at",
                        java.time.OffsetDateTime.class
                )
        );

        return attendee;
    }
}
