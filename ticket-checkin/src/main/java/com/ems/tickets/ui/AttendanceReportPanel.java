package com.ems.tickets.ui;

import com.ems.shared.model.UserSession;
import com.ems.tickets.model.Ticket;
import com.ems.tickets.service.TicketService;
import com.ems.tickets.service.CheckInService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Displays attendance reports for an event.
 */
public class AttendanceReportPanel extends JPanel {

    private final UserSession session;
    private final TicketService ticketService;

    private final JTextField eventIdField = new JTextField(10);

    private final JLabel registeredLabel = new JLabel("Registered attendees: 0");
    private final JLabel issuedLabel = new JLabel("Tickets issued: 0");
    private final JLabel checkedInLabel = new JLabel("Checked in: 0");
    private final JLabel notCheckedInLabel = new JLabel("Not checked in: 0");
    private final JLabel vipLabel = new JLabel("VIP attendees: 0");
    private final JLabel checkedInVipLabel = new JLabel("Checked-in VIP: 0");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Ticket Number", "Attendee", "Email",
                    "Event", "VIP", "Status", "Issued At", "Checked In At"}, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable ticketTable = new JTable(tableModel);

    public AttendanceReportPanel(UserSession session, TicketService ticketService, CheckInService checkInService) {
        this.session = Objects.requireNonNull(session, "session");
        this.ticketService = Objects.requireNonNull(ticketService, "ticketService");
        Objects.requireNonNull(checkInService, "checkInService");

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(createTopPanel(), BorderLayout.NORTH);

        ticketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(ticketTable), BorderLayout.CENTER);

        JButton exportButton = new JButton("Export CSV");
        exportButton.addActionListener(event -> exportCsv());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(exportButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {

        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JLabel title = new JLabel("Event Attendance Report");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(title, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Event ID:"));
        searchPanel.add(eventIdField);

        JButton loadButton = new JButton("Load Report");
        loadButton.addActionListener(event -> loadReport());
        searchPanel.add(loadButton);

        panel.add(searchPanel, BorderLayout.CENTER);

        JPanel summaryPanel = new JPanel(new GridLayout(2, 3, 10, 8));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Attendance Summary"));

        summaryPanel.add(registeredLabel);
        summaryPanel.add(issuedLabel);
        summaryPanel.add(checkedInLabel);
        summaryPanel.add(notCheckedInLabel);
        summaryPanel.add(vipLabel);
        summaryPanel.add(checkedInVipLabel);

        panel.add(summaryPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadReport() {

        try {
            long eventId = Long.parseLong(eventIdField.getText().trim());

            if (eventId <= 0) {
                throw new IllegalArgumentException("Event ID must be greater than zero.");
            }

            long[] summary = ticketService.getAttendanceSummary(eventId);
            List<Ticket> tickets = ticketService.findByEventId(eventId);

            registeredLabel.setText("Registered attendees: " + summary[0]);
            issuedLabel.setText("Tickets issued: " + summary[1]);
            checkedInLabel.setText("Checked in: " + summary[2]);
            notCheckedInLabel.setText("Not checked in: " + summary[3]);
            vipLabel.setText("VIP attendees: " + summary[4]);
            checkedInVipLabel.setText("Checked-in VIP: " + summary[5]);

            tableModel.setRowCount(0);

            for (Ticket ticket : tickets) {
                tableModel.addRow(new Object[]{
                        ticket.getTicketNumber(),
                        ticket.getAttendeeName(),
                        ticket.getAttendeeEmail(),
                        ticket.getEventName(),
                        ticket.isVip() ? "Yes" : "No",
                        ticket.getStatus(),
                        ticket.getIssuedAt(),
                        ticket.getCheckedInAt()
                });
            }

        } catch (NumberFormatException exception) {
            showError("Please enter a valid numeric event ID.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showError("Could not load attendance report: " + exception.getMessage());
        }
    }

    private void exportCsv() {

        if (tableModel.getRowCount() == 0) {
            showError("Load an attendance report before exporting.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("attendance-report.csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        try (FileWriter writer = new FileWriter(file)) {

            writer.write("Ticket Number,Attendee,Email,Event,VIP,Status,Issued At,Checked In At\n");

            for (int row = 0; row < tableModel.getRowCount(); row++) {

                for (int column = 0; column < tableModel.getColumnCount(); column++) {

                    Object value = tableModel.getValueAt(row, column);
                    String text = value == null ? "" : value.toString();

                    text = text.replace("\"", "\"\"");
                    writer.write("\"" + text + "\"");

                    if (column < tableModel.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }

                writer.write("\n");
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Attendance report exported successfully.",
                    "Export complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException exception) {
            showError("Could not export CSV: " + exception.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Attendance Report",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
