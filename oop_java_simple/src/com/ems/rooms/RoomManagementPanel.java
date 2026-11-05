package com.ems.rooms;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class RoomManagementPanel extends JPanel {
    private RoomService roomService;
    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JTextField txtSearch, txtName, txtCapacity, txtEventId;
    private JButton btnCreate, btnCheckIn, btnExit, btnMetrics;
    private int selectedRoomId = -1;

    public RoomManagementPanel(RoomService roomService) {
        this.roomService = roomService;
        setLayout(new BorderLayout(10, 10));
        initComponents();
        refreshTableData();
    }

    private void initComponents() {
        // Search Top Bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Search by Name:"));
        txtSearch = new JTextField(15);
        txtSearch.addCaretListener(e -> filterTableData());
        topPanel.add(txtSearch);
        add(topPanel, BorderLayout.NORTH);

        // Center Table Layout Setup
        String[] columns = {"ID", "Room Name", "Event ID", "Capacity", "Occupancy", "Remaining Slot", "Fill Percentage"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        roomTable = new JTable(tableModel);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = roomTable.getSelectedRow();
            if (selectedRow >= 0) {
                selectedRoomId = (int) tableModel.getValueAt(selectedRow, 0);
                txtName.setText(tableModel.getValueAt(selectedRow, 1).toString());
                txtEventId.setText(tableModel.getValueAt(selectedRow, 2).toString());
                txtCapacity.setText(tableModel.getValueAt(selectedRow, 3).toString());
            }
        });
        add(new JScrollPane(roomTable), BorderLayout.CENTER);

        // Sidebar Control Dashboard Panel
        JPanel formPanel = new JPanel(new GridLayout(7, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Management Controls"));

        formPanel.add(new JLabel("Room Name:"));
        txtName = new JTextField();
        formPanel.add(txtName);

        formPanel.add(new JLabel("Capacity:"));
        txtCapacity = new JTextField();
        formPanel.add(txtCapacity);

        formPanel.add(new JLabel("Event ID:"));
        txtEventId = new JTextField();
        formPanel.add(txtEventId);

        btnCreate = new JButton("Register New");
        btnCreate.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                int cap = Integer.parseInt(txtCapacity.getText().trim());
                int evId = Integer.parseInt(txtEventId.getText().trim());
                roomService.registerRoom(name, cap, evId);
                refreshTableData();
                JOptionPane.showMessageDialog(this, "Room Added Successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error inputting fields: " + ex.getMessage());
            }
        });
        formPanel.add(btnCreate);

        btnCheckIn = new JButton("Check-In Attendee (+)");
        btnCheckIn.addActionListener(e -> {
            if (selectedRoomId == -1) return;
            boolean checkedIn = roomService.handleAttendeeCheckIn(selectedRoomId);
            if (!checkedIn) {
                JOptionPane.showMessageDialog(this, "🛑 Cannot check-in! Room is at full capacity limit.", "Room Alert", JOptionPane.WARNING_MESSAGE);
            }
            refreshTableData();
        });
        formPanel.add(btnCheckIn);

        btnExit = new JButton("Exit Attendee (-)");
        btnExit.addActionListener(e -> {
            if (selectedRoomId == -1) return;
            roomService.handleAttendeeExit(selectedRoomId);
            refreshTableData();
        });
        formPanel.add(btnExit);

        btnMetrics = new JButton("View Event Metrics");
        btnMetrics.addActionListener(e -> {
            try {
                int eventId = Integer.parseInt(txtEventId.getText().trim());
                int totalCap = roomService.getAssignedCapacityForEvent(eventId);
                int totalReg = roomService.getRegisteredAttendeeCountForEvent(eventId);
                String msg = "Event ID: " + eventId + "\n" +
                             "Total Allocated Space Capacity: " + totalCap + "\n" +
                             "Total Registered Ticket Owners: " + totalReg + "\n" +
                             (totalReg > totalCap ? "⚠️ WARNING: This event is overbooked!" : "✅ Safe space allocation.");
                JOptionPane.showMessageDialog(this, msg, "Event Analytics", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please select a room or type a valid Event ID.");
            }
        });
        formPanel.add(btnMetrics);

        add(formPanel, BorderLayout.EAST);
    }

    private void refreshTableData() {
        updateTableRows(roomService.getAllRooms());
    }

    private void filterTableData() {
        updateTableRows(roomService.searchRoomsByName(txtSearch.getText().trim()));
    }

    private void updateTableRows(List<Room> list) {
        tableModel.setRowCount(0);
        for (Room r : list) {
            tableModel.addRow(new Object[]{
                r.getId(), r.getName(), r.getEventId(), r.getCapacity(),
                r.getCurrentOccupancy(), r.getRemainingCapacity(),
                String.format("%.1f%%", r.getOccupancyPercentage())
            });
        }
    }
}
