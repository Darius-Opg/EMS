package com.ems.events.dao;

import com.ems.events.model.Vendor;
import com.ems.shared.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VendorDAO {

    // CREATE
    public long create(Vendor vendor) throws SQLException {
        String sql = """
                INSERT INTO vendors
                (event_id, vendor_name, service_type, contact_person, phone, email)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, vendor.getEventId());
            stmt.setString(2, vendor.getVendorName());
            stmt.setString(3, vendor.getServiceType());
            stmt.setString(4, vendor.getContactPerson());
            stmt.setString(5, vendor.getPhone());
            stmt.setString(6, vendor.getEmail());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    vendor.setVendorId(id);
                    return id;
                }
            }
        }

        throw new SQLException("Vendor creation failed: no ID was returned.");
    }

    // READ ONE
    public Optional<Vendor> findById(long id) throws SQLException {
        String sql = "SELECT * FROM vendors WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    // READ ALL VENDORS
    public List<Vendor> findAll() throws SQLException {
        String sql = "SELECT * FROM vendors ORDER BY vendor_name";
        List<Vendor> vendors = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                vendors.add(mapRow(rs));
            }
        }

        return vendors;
    }

    // READ VENDORS BELONGING TO ONE EVENT
    public List<Vendor> findByEventId(long eventId) throws SQLException {
        String sql = "SELECT * FROM vendors WHERE event_id = ? ORDER BY vendor_name";
        List<Vendor> vendors = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, eventId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vendors.add(mapRow(rs));
                }
            }
        }

        return vendors;
    }

    // UPDATE
    public boolean update(Vendor vendor) throws SQLException {
        String sql = """
                UPDATE vendors
                SET event_id = ?, vendor_name = ?, service_type = ?,
                    contact_person = ?, phone = ?, email = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, vendor.getEventId());
            stmt.setString(2, vendor.getVendorName());
            stmt.setString(3, vendor.getServiceType());
            stmt.setString(4, vendor.getContactPerson());
            stmt.setString(5, vendor.getPhone());
            stmt.setString(6, vendor.getEmail());
            stmt.setLong(7, vendor.getVendorId());

            return stmt.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM vendors WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // CONVERT DATABASE ROW INTO VENDOR OBJECT
    private Vendor mapRow(ResultSet rs) throws SQLException {
        Vendor vendor = new Vendor();

        vendor.setVendorId(rs.getLong("id"));
        vendor.setEventId(rs.getLong("event_id"));
        vendor.setVendorName(rs.getString("vendor_name"));
        vendor.setServiceType(rs.getString("service_type"));
        vendor.setContactPerson(rs.getString("contact_person"));
        vendor.setPhone(rs.getString("phone"));
        vendor.setEmail(rs.getString("email"));

        return vendor;
    }
}