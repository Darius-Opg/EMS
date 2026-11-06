package com.ems.auth.ui;

import com.ems.auth.dao.StaffAssignmentDAO;
import com.ems.auth.model.StaffAssignment;
import com.ems.auth.service.StaffAssignmentService;
import com.ems.shared.model.UserSession;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Assignment management for organizers and duty viewing for staff. */
public class StaffAssignmentPanel extends JPanel {

    private final UserSession session;
    private final StaffAssignmentService service;
    private final List<StaffAssignment> visibleAssignments = new ArrayList<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Staff member", "Event", "Room", "Duty", "Assigned by"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    public StaffAssignmentPanel(UserSession session) {
        this(session, new StaffAssignmentService());
    }

    public StaffAssignmentPanel(UserSession session, StaffAssignmentService service) {
        this.session = Objects.requireNonNull(session, "session");
        this.service = Objects.requireNonNull(service, "service");
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refreshAssignments());
        actions.add(refresh);
        if (session.hasRole("ORGANIZER") || session.hasRole("ADMIN")) {
            JButton assign = new JButton("Assign duty");
            assign.addActionListener(event -> showAssignmentDialog());
            JButton remove = new JButton("Remove selected");
            remove.addActionListener(event -> removeSelected());
            actions.add(assign);
            actions.add(remove);
        }
        add(actions, BorderLayout.SOUTH);
        refreshAssignments();
    }

    private void refreshAssignments() {
        try {
            List<StaffAssignment> assignments = (session.hasRole("ORGANIZER") || session.hasRole("ADMIN"))
                    ? service.findAll(session)
                    : service.findMyAssignments(session);
            visibleAssignments.clear();
            visibleAssignments.addAll(assignments);
            tableModel.setRowCount(0);
            for (StaffAssignment assignment : assignments) {
                tableModel.addRow(new Object[]{assignment.staffName(),
                        valueOrDash(assignment.eventName()), valueOrDash(assignment.roomName()),
                        assignment.duty(), assignment.assignedBy()});
            }
        } catch (StaffAssignmentService.AssignmentException exception) {
            showError(exception.getMessage());
        }
    }

    private void showAssignmentDialog() {
        try {
            JComboBox<StaffAssignmentDAO.StaffOption> staffBox = new JComboBox<>(
                    service.getActiveStaff(session).toArray(StaffAssignmentDAO.StaffOption[]::new));
            JComboBox<Choice<StaffAssignmentDAO.EventOption>> eventBox =
                    new JComboBox<>(eventChoices(service.getEvents(session)));
            JComboBox<Choice<StaffAssignmentDAO.RoomOption>> roomBox =
                    new JComboBox<>(roomChoices(service.getRooms(session)));
            JTextArea dutyArea = new JTextArea(4, 24);
            dutyArea.setLineWrap(true);
            dutyArea.setWrapStyleWord(true);

            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Staff member"));
            form.add(staffBox);
            form.add(new JLabel("Event (optional if room selected)"));
            form.add(eventBox);
            form.add(new JLabel("Room (optional if event selected)"));
            form.add(roomBox);
            form.add(new JLabel("Duty"));
            form.add(new JScrollPane(dutyArea));

            int result = JOptionPane.showConfirmDialog(this, form, "Assign staff duty",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }
            StaffAssignmentDAO.StaffOption staff = (StaffAssignmentDAO.StaffOption) staffBox.getSelectedItem();
            if (staff == null) {
                throw new StaffAssignmentService.AssignmentException("Create an active staff account first.");
            }
            StaffAssignmentDAO.EventOption event = selectedValue(eventBox);
            StaffAssignmentDAO.RoomOption room = selectedValue(roomBox);
            service.assign(session, staff.id(), event == null ? null : event.id(),
                    room == null ? null : room.id(), dutyArea.getText());
            refreshAssignments();
        } catch (StaffAssignmentService.AssignmentException exception) {
            showError(exception.getMessage());
        }
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError("Select an assignment first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        if (modelRow >= visibleAssignments.size()) {
            return;
        }
        StaffAssignment assignment = visibleAssignments.get(modelRow);
        int answer = JOptionPane.showConfirmDialog(this,
                "Remove the duty assigned to " + assignment.staffName() + "?",
                "Confirm removal", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                service.remove(session, assignment.id());
                refreshAssignments();
            } catch (StaffAssignmentService.AssignmentException exception) {
                showError(exception.getMessage());
            }
        }
    }

    private DefaultComboBoxModel<Choice<StaffAssignmentDAO.EventOption>> eventChoices(
            List<StaffAssignmentDAO.EventOption> events) {
        DefaultComboBoxModel<Choice<StaffAssignmentDAO.EventOption>> model = new DefaultComboBoxModel<>();
        model.addElement(new Choice<>("No event", null));
        events.forEach(event -> model.addElement(new Choice<>(event.name(), event)));
        return model;
    }

    private DefaultComboBoxModel<Choice<StaffAssignmentDAO.RoomOption>> roomChoices(
            List<StaffAssignmentDAO.RoomOption> rooms) {
        DefaultComboBoxModel<Choice<StaffAssignmentDAO.RoomOption>> model = new DefaultComboBoxModel<>();
        model.addElement(new Choice<>("No room", null));
        rooms.forEach(room -> model.addElement(new Choice<>(room.toString(), room)));
        return model;
    }

    private <T> T selectedValue(JComboBox<Choice<T>> comboBox) {
        Choice<T> choice = (Choice<T>) comboBox.getSelectedItem();
        return choice == null ? null : choice.value();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Staff assignments", JOptionPane.ERROR_MESSAGE);
    }

    private record Choice<T>(String label, T value) {
        @Override public String toString() { return label; }
    }
}
