package com.ems.tickets.ui;

import com.ems.shared.model.UserSession;
import com.ems.tickets.model.Ticket;
import com.ems.tickets.service.TicketService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TicketManagementPanel extends JPanel {

    private final UserSession session;
    private final TicketService ticketService;

    private JTextField ticketSearchField;
    private JTextField eventIdField;
    private JTable ticketTable;
    private DefaultTableModel tableModel;

    public TicketManagementPanel(UserSession session,
                                 TicketService ticketService) {

        this.session = session;
        this.ticketService = ticketService;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        createSearchPanel();
        createTicketTable();
        createActionPanel();

        loadAllTickets();
    }

    private void createSearchPanel() {

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        ticketSearchField = new JTextField(15);
        eventIdField = new JTextField(6);

        JButton searchButton = new JButton("Search Ticket");
        JButton filterButton = new JButton("Filter by Event");
        JButton refreshButton = new JButton("Refresh");

        searchButton.addActionListener(e -> searchTicket());
        filterButton.addActionListener(e -> filterByEvent());
        refreshButton.addActionListener(e -> loadAllTickets());

        searchPanel.add(new JLabel("Ticket Number:"));
        searchPanel.add(ticketSearchField);
        searchPanel.add(searchButton);

        searchPanel.add(new JLabel("Event ID:"));
        searchPanel.add(eventIdField);
        searchPanel.add(filterButton);
        searchPanel.add(refreshButton);

        add(searchPanel, BorderLayout.NORTH);
    }

    private void createTicketTable() {

        String[] columns = {
                "ID",
                "Ticket Number",
                "Attendee",
                "Email",
                "Phone",
                "Event",
                "VIP",
                "Status",
                "Issued At",
                "Checked In At"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ticketTable = new JTable(tableModel);
        ticketTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        ticketTable.setAutoCreateRowSorter(true);

        add(new JScrollPane(ticketTable), BorderLayout.CENTER);
    }

    private void createActionPanel() {

        JPanel actionPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        JButton cancelButton = new JButton("Cancel Selected Ticket");

        cancelButton.addActionListener(e -> cancelSelectedTicket());

        boolean canCancel =
                session.hasRole("ADMIN")
                || session.hasRole("ORGANIZER");

        cancelButton.setEnabled(canCancel);

        actionPanel.add(cancelButton);

        add(actionPanel, BorderLayout.SOUTH);
    }

    private void loadAllTickets() {

        try {
            List<Ticket> tickets = ticketService.findAll();
            displayTickets(tickets);

        } catch (Exception ex) {
            showError("Unable to load tickets: " + ex.getMessage());
        }
    }

    private void searchTicket() {

        String ticketNumber = ticketSearchField.getText().trim();

        if (ticketNumber.isEmpty()) {
            showError("Enter a ticket number to search.");
            return;
        }

        try {
            Ticket ticket =
                    ticketService.findByTicketNumber(ticketNumber);

            tableModel.setRowCount(0);

            if (ticket != null) {
                addTicketRow(ticket);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "No ticket found with that number.",
                        "Search Result",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (Exception ex) {
            showError("Search failed: " + ex.getMessage());
        }
    }

    private void filterByEvent() {

        String eventText = eventIdField.getText().trim();

        if (eventText.isEmpty()) {
            showError("Enter an event ID.");
            return;
        }

        try {
            long eventId = Long.parseLong(eventText);

            if (eventId <= 0) {
                showError("Event ID must be a positive number.");
                return;
            }

            List<Ticket> tickets =
                    ticketService.findByEventId(eventId);

            displayTickets(tickets);

        } catch (NumberFormatException ex) {
            showError("Event ID must be a valid number.");

        } catch (Exception ex) {
            showError("Unable to filter tickets: " + ex.getMessage());
        }
    }

    private void displayTickets(List<Ticket> tickets) {

        tableModel.setRowCount(0);

        for (Ticket ticket : tickets) {
            addTicketRow(ticket);
        }
    }

    private void addTicketRow(Ticket ticket) {

        tableModel.addRow(new Object[]{
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getAttendeeName(),
                ticket.getAttendeeEmail(),
                ticket.getAttendeePhone(),
                ticket.getEventName(),
                ticket.isVip() ? "Yes" : "No",
                ticket.getStatus(),
                ticket.getIssuedAt(),
                ticket.getCheckedInAt()
        });
    }

    private void cancelSelectedTicket() {

        int selectedRow = ticketTable.getSelectedRow();

        if (selectedRow == -1) {
            showError("Select a ticket first.");
            return;
        }

        int modelRow =
                ticketTable.convertRowIndexToModel(selectedRow);

        long ticketId =
                ((Number) tableModel.getValueAt(modelRow, 0)).longValue();

        String status =
                String.valueOf(tableModel.getValueAt(modelRow, 7));

        if (!"VALID".equals(status)) {
            showError("Only valid tickets can be cancelled.");
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel this ticket?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean cancelled =
                    ticketService.cancelTicket(ticketId, session);

            if (cancelled) {
                JOptionPane.showMessageDialog(
                        this,
                        "Ticket cancelled successfully."
                );
                loadAllTickets();

            } else {
                showError("Ticket could not be cancelled.");
            }

        } catch (Exception ex) {
            showError("Cancellation failed: " + ex.getMessage());
        }
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Ticket Management",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
