
package com.ems.events.ui;

import com.ems.events.model.Event;
import com.ems.events.service.EventService;
import com.ems.shared.model.UserSession;
import com.ems.shared.utils.EventTime;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class EventManagementPanel extends JPanel {

    private final UserSession session;
    private final EventService eventService;

    private final DefaultTableModel tableModel;
    private final JTable eventTable;
    private final JTextField searchField;
    private final JComboBox<String> statusFilter;

    private static final String[] STATUSES = {
            "UPCOMING", "ONGOING", "COMPLETED", "CANCELLED"
    };

    public EventManagementPanel(UserSession session, EventService eventService) {
        this.session = session;
        this.eventService = eventService;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Event Name", "Venue", "Start (EAT)", "End (EAT)", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        eventTable = new JTable(tableModel);
        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventTable.setAutoCreateRowSorter(true);

        searchField = new JTextField(20);
        statusFilter = new JComboBox<>(
                new String[]{"ALL", "UPCOMING", "ONGOING", "COMPLETED", "CANCELLED"}
        );

        add(createTopPanel(), BorderLayout.NORTH);
        add(new JScrollPane(eventTable), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        loadEvents();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");

        panel.add(new JLabel("Search:"));
        panel.add(searchField);
        panel.add(searchButton);
        panel.add(new JLabel("Status:"));
        panel.add(statusFilter);
        panel.add(refreshButton);

        searchButton.addActionListener(e -> searchEvents());
        searchField.addActionListener(e -> searchEvents());
        statusFilter.addActionListener(e -> loadEvents());
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedItem("ALL");
            loadEvents();
        });

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addButton = new JButton("Add Event");
        JButton editButton = new JButton("Edit Event");
        JButton deleteButton = new JButton("Delete Event");

        boolean canManage = session != null &&
                (session.hasRole("ADMIN") || session.hasRole("ORGANIZER"));

        addButton.setEnabled(canManage);
        editButton.setEnabled(canManage);
        deleteButton.setEnabled(canManage);

        addButton.addActionListener(e -> addEvent());
        editButton.addActionListener(e -> editEvent());
        deleteButton.addActionListener(e -> deleteEvent());

        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);

        return panel;
    }

    private void loadEvents() {
        try {
            String status = (String) statusFilter.getSelectedItem();

            List<Event> events = "ALL".equals(status)
                    ? eventService.getAllEvents()
                    : eventService.getEventsByStatus(status);

            displayEvents(events);

        } catch (Exception e) {
            showError("Could not load events: " + e.getMessage());
        }
    }

    private void searchEvents() {
        try {
            List<Event> events = eventService.searchEvents(searchField.getText());

            String status = (String) statusFilter.getSelectedItem();

            if (!"ALL".equals(status)) {
                events = events.stream()
                        .filter(event -> status.equalsIgnoreCase(event.getStatus()))
                        .toList();
            }

            displayEvents(events);

        } catch (Exception e) {
            showError("Search failed: " + e.getMessage());
        }
    }

    private void displayEvents(List<Event> events) {
        tableModel.setRowCount(0);

        for (Event event : events) {
            tableModel.addRow(new Object[]{
                    event.getEventId(),
                    event.getName(),
                    event.getVenue(),
                    formatDate(event.getStartDateTime()),
                    formatDate(event.getEndDateTime()),
                    event.getStatus()
            });
        }
    }

    private void addEvent() {
        Event event = showEventForm(null);

        if (event == null) {
            return;
        }

        try {
            eventService.createEvent(event, session);
            showMessage("Event created successfully.");
            loadEvents();
        } catch (Exception e) {
            showError("Could not create event: " + e.getMessage());
        }
    }

    private void editEvent() {
        Event selected = getSelectedEvent();

        if (selected == null) {
            showError("Please select an event to edit.");
            return;
        }

        Event updated = showEventForm(selected);

        if (updated == null) {
            return;
        }

        try {
            eventService.updateEvent(updated, session);
            showMessage("Event updated successfully.");
            loadEvents();
        } catch (Exception e) {
            showError("Could not update event: " + e.getMessage());
        }
    }

    private void deleteEvent() {
        Event selected = getSelectedEvent();

        if (selected == null) {
            showError("Please select an event to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete event: " + selected.getName() + "?",
                "Confirm deletion",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean deleted = eventService.deleteEvent(selected.getEventId(), session);

            if (deleted) {
                showMessage("Event deleted successfully.");
            } else {
                showError("Event could not be found.");
            }

            loadEvents();

        } catch (Exception e) {
            showError("Could not delete event: " + e.getMessage());
        }
    }

    private Event getSelectedEvent() {
        int selectedRow = eventTable.getSelectedRow();

        if (selectedRow < 0) {
            return null;
        }

        int modelRow = eventTable.convertRowIndexToModel(selectedRow);
        long eventId = ((Number) tableModel.getValueAt(modelRow, 0)).longValue();

        try {
            return eventService.getEventById(eventId).orElse(null);
        } catch (Exception e) {
            showError("Could not retrieve selected event: " + e.getMessage());
            return null;
        }
    }

    private Event showEventForm(Event existing) {
        JTextField nameField = new JTextField(
                existing == null ? "" : existing.getName()
        );

        JTextArea descriptionArea = new JTextArea(
                existing == null || existing.getDescription() == null
                        ? "" : existing.getDescription(), 4, 25
        );

        JTextField venueField = new JTextField(
                existing == null ? "" : existing.getVenue()
        );

        JTextField startField = new JTextField(
                existing == null ? "" : EventTime.formatInput(existing.getStartDateTime())
        );

        JTextField endField = new JTextField(
                existing == null ? "" : EventTime.formatInput(existing.getEndDateTime())
        );

        JComboBox<String> statusBox = new JComboBox<>(STATUSES);

        if (existing != null) {
            statusBox.setSelectedItem(existing.getStatus());
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

        form.add(new JLabel("Event name:"));
        form.add(nameField);

        form.add(new JLabel("Description:"));
        form.add(new JScrollPane(descriptionArea));

        form.add(new JLabel("Venue:"));
        form.add(venueField);

        form.add(new JLabel("Start (" + EventTime.ZONE_LABEL + "; yyyy-MM-dd HH:mm):"));
        form.add(startField);

        form.add(new JLabel("End (" + EventTime.ZONE_LABEL + "; yyyy-MM-dd HH:mm):"));
        form.add(endField);

        form.add(new JLabel("Status:"));
        form.add(statusBox);

        int result = JOptionPane.showConfirmDialog(
                this,
                form,
                existing == null ? "Create Event" : "Edit Event",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        try {
            OffsetDateTime start = EventTime.parseInput(startField.getText().trim());
            OffsetDateTime end = EventTime.parseInput(endField.getText().trim());

            Event event = existing == null ? new Event() : existing;

            event.setName(nameField.getText().trim());
            event.setDescription(descriptionArea.getText().trim());
            event.setVenue(venueField.getText().trim());
            event.setStartDateTime(start);
            event.setEndDateTime(end);
            event.setStatus((String) statusBox.getSelectedItem());

            return event;

        } catch (DateTimeParseException e) {
            showError("Invalid date format. Use yyyy-MM-dd HH:mm in "
                    + EventTime.ZONE_LABEL + ".");
            return null;
        }
    }

    private String formatDate(OffsetDateTime date) {
        return EventTime.formatDisplay(date);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Event Management",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
