package com.ems.attendees.ui;

import com.ems.attendees.model.Attendee;
import com.ems.attendees.model.VipAttendee;
import com.ems.attendees.service.RegistrationService;
import com.ems.shared.model.UserSession;
import com.ems.tickets.model.Ticket;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AttendeeManagementPanel extends JPanel {

    private final UserSession session;
    private final RegistrationService registrationService;

    private JComboBox<RegistrationService.EventOption> eventCombo;
    private JTextField searchField;
    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JCheckBox vipCheckBox;
    private JTextField vipLevelField;
    private JTextArea requirementsArea;

    private JTable attendeeTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    private JButton saveButton;
    private TitledBorder formBorder;

    private Long editingAttendeeId;
    private boolean refreshingEvents;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public AttendeeManagementPanel(
            UserSession session,
            RegistrationService registrationService
    ) {
        if (session == null || registrationService == null) {
            throw new IllegalArgumentException(
                    "User session and registration service are required."
            );
        }

        this.session = session;
        this.registrationService = registrationService;

        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        createTopPanel();
        createAttendeeTable();
        createRegistrationPanel();
        createStatusPanel();

        refreshEvents();
    }

    private void createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        eventCombo = new JComboBox<>();
        eventCombo.setPreferredSize(new Dimension(300, 28));
        eventCombo.addActionListener(e -> {
            if (!refreshingEvents) {
                clearForm();
                loadAttendees();
            }
        });

        searchField = new JTextField(16);

        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");

        searchButton.addActionListener(e -> searchAttendees());
        refreshButton.addActionListener(e -> refreshEvents());

        topPanel.add(new JLabel("Event:"));
        topPanel.add(eventCombo);
        topPanel.add(new JLabel("Search name, email or phone:"));
        topPanel.add(searchField);
        topPanel.add(searchButton);
        topPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);
    }

    private void createAttendeeTable() {
        String[] columns = {
                "ID",
                "Full Name",
                "Email",
                "Phone",
                "VIP",
                "VIP Level",
                "Registered At"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        attendeeTable = new JTable(tableModel);
        attendeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        attendeeTable.setAutoCreateRowSorter(true);
        attendeeTable.setRowHeight(24);

        add(new JScrollPane(attendeeTable), BorderLayout.CENTER);
    }

    private void createRegistrationPanel() {
        JPanel sidePanel = new JPanel(new BorderLayout(8, 8));
        sidePanel.setPreferredSize(new Dimension(310, 0));

        JPanel recordActions = new JPanel(new GridLayout(1, 2, 8, 0));

        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");

        editButton.addActionListener(e -> editSelectedAttendee());
        deleteButton.addActionListener(e -> deleteSelectedAttendee());

        recordActions.add(editButton);
        recordActions.add(deleteButton);

        sidePanel.add(recordActions, BorderLayout.NORTH);

        JPanel registrationPanel = new JPanel();
        registrationPanel.setLayout(
                new BoxLayout(registrationPanel, BoxLayout.Y_AXIS)
        );

        formBorder = BorderFactory.createTitledBorder("Register Attendee");
        registrationPanel.setBorder(formBorder);

        nameField = new JTextField(20);
        emailField = new JTextField(20);
        phoneField = new JTextField(20);

        vipCheckBox = new JCheckBox("Register as VIP");
        vipLevelField = new JTextField(20);
        requirementsArea = new JTextArea(4, 18);
        requirementsArea.setLineWrap(true);
        requirementsArea.setWrapStyleWord(true);

        vipLevelField.setEnabled(false);
        requirementsArea.setEnabled(false);

        vipCheckBox.addActionListener(e -> updateVipFields());

        addFormField(registrationPanel, "Full Name", nameField);
        addFormField(registrationPanel, "Email Address", emailField);
        addFormField(registrationPanel, "Phone Number", phoneField);

        registrationPanel.add(vipCheckBox);
        registrationPanel.add(Box.createVerticalStrut(5));

        addFormField(registrationPanel, "VIP Level", vipLevelField);

        JLabel requirementsLabel = new JLabel("Special Requirements");
        requirementsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        registrationPanel.add(requirementsLabel);
        registrationPanel.add(Box.createVerticalStrut(4));

        JScrollPane requirementsScroll = new JScrollPane(requirementsArea);
        requirementsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        registrationPanel.add(requirementsScroll);
        registrationPanel.add(Box.createVerticalStrut(12));

        saveButton = new JButton("Register and Issue Ticket");
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.addActionListener(e -> saveAttendee());

        JButton clearButton = new JButton("Clear Form / Cancel Edit");
        clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearButton.addActionListener(e -> clearForm());

        registrationPanel.add(saveButton);
        registrationPanel.add(Box.createVerticalStrut(8));
        registrationPanel.add(clearButton);

        sidePanel.add(registrationPanel, BorderLayout.CENTER);
        add(sidePanel, BorderLayout.EAST);
    }

    private void addFormField(
            JPanel panel,
            String label,
            JComponent field
    ) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(fieldLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(field);
        panel.add(Box.createVerticalStrut(9));
    }

    private void createStatusPanel() {
        statusLabel = new JLabel("Select an event to view its attendees.");
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void refreshEvents() {
        try {
            RegistrationService.EventOption previous =
                    (RegistrationService.EventOption) eventCombo.getSelectedItem();

            List<RegistrationService.EventOption> events =
                    registrationService.getRegistrableEvents(session);

            refreshingEvents = true;

            try {
                eventCombo.removeAllItems();

                for (RegistrationService.EventOption event : events) {
                    eventCombo.addItem(event);
                }

                if (previous != null) {
                    for (int i = 0; i < eventCombo.getItemCount(); i++) {
                        if (eventCombo.getItemAt(i).id() == previous.id()) {
                            eventCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            } finally {
                refreshingEvents = false;
            }

            if (events.isEmpty()) {
                tableModel.setRowCount(0);
                clearForm();
                statusLabel.setText(
                        "No upcoming or ongoing events are currently accepting registrations."
                );
            } else {
                loadAttendees();
            }

        } catch (Exception exception) {
            refreshingEvents = false;
            showError("Unable to load events: " + exception.getMessage());
        }
    }

    private void loadAttendees() {
        RegistrationService.EventOption selectedEvent =
                (RegistrationService.EventOption) eventCombo.getSelectedItem();

        if (selectedEvent == null || tableModel == null) {
            return;
        }

        try {
            List<Attendee> attendees =
                    registrationService.getAttendeesForEvent(
                            selectedEvent.id(),
                            session
                    );

            displayAttendees(attendees);

            statusLabel.setText(
                    attendees.size() + " attendee(s) registered for "
                            + selectedEvent.name() + "."
            );

        } catch (Exception exception) {
            showError("Unable to load attendees: " + exception.getMessage());
        }
    }

    private void searchAttendees() {
        RegistrationService.EventOption selectedEvent =
                (RegistrationService.EventOption) eventCombo.getSelectedItem();

        if (selectedEvent == null) {
            showError("Select an event before searching.");
            return;
        }

        try {
            List<Attendee> attendees =
                    registrationService.searchAttendees(
                            selectedEvent.id(),
                            searchField.getText(),
                            session
                    );

            displayAttendees(attendees);

            statusLabel.setText(
                    attendees.size() + " matching attendee(s) found."
            );

        } catch (Exception exception) {
            showError("Attendee search failed: " + exception.getMessage());
        }
    }

    private void displayAttendees(List<Attendee> attendees) {
        tableModel.setRowCount(0);

        for (Attendee attendee : attendees) {
            String vipLevel = attendee instanceof VipAttendee vip
                    ? vip.getVipLevel()
                    : "";

            String registeredAt = attendee.getRegisteredAt() == null
                    ? ""
                    : DATE_FORMAT.format(attendee.getRegisteredAt());

            tableModel.addRow(new Object[]{
                    attendee.getId(),
                    attendee.getFullName(),
                    attendee.getEmail(),
                    attendee.getPhone(),
                    attendee.isVip() ? "Yes" : "No",
                    vipLevel,
                    registeredAt
            });
        }
    }

    private void editSelectedAttendee() {
        Long attendeeId = getSelectedAttendeeId();

        if (attendeeId == null) {
            showError("Select an attendee from the table before editing.");
            return;
        }

        RegistrationService.EventOption selectedEvent =
                (RegistrationService.EventOption) eventCombo.getSelectedItem();

        if (selectedEvent == null) {
            showError("Select an event first.");
            return;
        }

        try {
            Attendee attendee =
                    registrationService.getAttendeeById(attendeeId, session);

            if (attendee.getEventId() == null || selectedEvent.id() != attendee.getEventId()) {
                showError("The selected attendee does not belong to this event.");
                return;
            }

            editingAttendeeId = attendee.getId();

            nameField.setText(attendee.getFullName());
            emailField.setText(attendee.getEmail());
            phoneField.setText(attendee.getPhone());

            if (attendee instanceof VipAttendee vip) {
                vipCheckBox.setSelected(true);
                vipLevelField.setText(vip.getVipLevel());
                requirementsArea.setText(vip.getSpecialRequirements());
            } else {
                vipCheckBox.setSelected(false);
                vipLevelField.setText("");
                requirementsArea.setText("");
            }

            updateVipFields();

            formBorder.setTitle("Edit Attendee #" + attendeeId);
            saveButton.setText("Save Changes");

            statusLabel.setText(
                    "Editing " + attendee.getFullName()
                            + ". Save Changes when finished."
            );

        } catch (Exception exception) {
            showError("Unable to load attendee: " + exception.getMessage());
        }
    }

    private void deleteSelectedAttendee() {
        Long attendeeId = getSelectedAttendeeId();

        if (attendeeId == null) {
            showError("Select an attendee from the table before deleting.");
            return;
        }

        RegistrationService.EventOption selectedEvent =
                (RegistrationService.EventOption) eventCombo.getSelectedItem();

        if (selectedEvent == null) {
            showError("Select an event first.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete attendee #" + attendeeId + "?\n\n"
                        + "An attendee with an issued ticket cannot be deleted.",
                "Confirm Attendee Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            registrationService.deleteAttendee(
                    attendeeId,
                    selectedEvent.id(),
                    session
            );

            if (attendeeId.equals(editingAttendeeId)) {
                clearForm();
            }

            loadAttendees();

            JOptionPane.showMessageDialog(
                    this,
                    "Attendee record deleted successfully.",
                    "Attendee Deleted",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception exception) {
            showError("Unable to delete attendee: " + exception.getMessage());
        }
    }

    private Long getSelectedAttendeeId() {
        int selectedViewRow = attendeeTable.getSelectedRow();

        if (selectedViewRow < 0) {
            return null;
        }

        int modelRow = attendeeTable.convertRowIndexToModel(selectedViewRow);
        Object value = tableModel.getValueAt(modelRow, 0);

        if (value instanceof Number number) {
            return number.longValue();
        }

        return null;
    }

    private void saveAttendee() {
        RegistrationService.EventOption selectedEvent =
                (RegistrationService.EventOption) eventCombo.getSelectedItem();

        if (selectedEvent == null) {
            showError("There is no eligible event selected.");
            return;
        }

        try {
            Attendee attendee = buildAttendee(selectedEvent.id());

            if (editingAttendeeId == null) {
                RegistrationService.RegistrationResult result =
                        registrationService.register(attendee, session);

                Ticket ticket = result.ticket();

                JOptionPane.showMessageDialog(
                        this,
                        "Attendee registered successfully.\n\n"
                                + "Attendee: " + attendee.getFullName() + "\n"
                                + "Event: " + selectedEvent.name() + "\n"
                                + "Ticket Number: " + ticket.getTicketNumber() + "\n"
                                + "VIP: " + (attendee.isVip() ? "Yes" : "No"),
                        "Registration Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {
                attendee.setId(editingAttendeeId);

                registrationService.updateAttendee(attendee, session);

                JOptionPane.showMessageDialog(
                        this,
                        "Attendee details updated successfully.\n\n"
                                + "Attendee: " + attendee.getFullName() + "\n"
                                + "Event: " + selectedEvent.name() + "\n"
                                + "VIP: " + (attendee.isVip() ? "Yes" : "No"),
                        "Attendee Updated",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            clearForm();
            searchField.setText("");
            loadAttendees();

        } catch (Exception exception) {
            showError(
                    (editingAttendeeId == null
                            ? "Registration failed: "
                            : "Update failed: ")
                            + exception.getMessage()
            );
        }
    }

    private Attendee buildAttendee(long eventId) {
        if (vipCheckBox.isSelected()) {
            return new VipAttendee(
                    eventId,
                    nameField.getText(),
                    emailField.getText(),
                    phoneField.getText(),
                    vipLevelField.getText(),
                    requirementsArea.getText()
            );
        }

        return new Attendee(
                eventId,
                nameField.getText(),
                emailField.getText(),
                phoneField.getText()
        );
    }

    private void updateVipFields() {
        boolean vip = vipCheckBox.isSelected();
        vipLevelField.setEnabled(vip);
        requirementsArea.setEnabled(vip);
    }

    private void clearForm() {
        editingAttendeeId = null;

        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");

        vipCheckBox.setSelected(false);
        vipLevelField.setText("");
        requirementsArea.setText("");

        updateVipFields();

        formBorder.setTitle("Register Attendee");
        saveButton.setText("Register and Issue Ticket");

        if (formBorder.getBorder() != null) {
            repaint();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Attendee Management",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
