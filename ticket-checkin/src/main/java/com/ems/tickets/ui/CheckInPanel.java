package com.ems.tickets.ui;

import com.ems.shared.model.UserSession;
import com.ems.tickets.model.Ticket;
import com.ems.tickets.service.CheckInService;
import com.ems.tickets.service.TicketService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class CheckInPanel extends JPanel {

    private final UserSession session;
    private final CheckInService checkInService;
    private final TicketService ticketService;

    private JTextField ticketNumberField;
    private JTextArea detailsArea;
    private JButton validateButton;
    private JButton checkInButton;

    private Ticket validatedTicket;

    public CheckInPanel(UserSession session,
                        CheckInService checkInService,
                        TicketService ticketService) {

        this.session = session;
        this.checkInService = checkInService;
        this.ticketService = ticketService;

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        createHeader();
        createCheckInForm();
        createDetailsArea();

        checkInButton.setEnabled(false);
    }

    private void createHeader() {

        JLabel heading = new JLabel("ATTENDEE CHECK-IN");
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        heading.setHorizontalAlignment(SwingConstants.CENTER);

        add(heading, BorderLayout.NORTH);
    }

    private void createCheckInForm() {

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));

        ticketNumberField = new JTextField(20);

        validateButton = new JButton("Validate Ticket");
        checkInButton = new JButton("Confirm Check-In");

        validateButton.addActionListener(e -> validateTicket());
        checkInButton.addActionListener(e -> checkInAttendee());

        ticketNumberField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                invalidateValidation();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                invalidateValidation();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                invalidateValidation();
            }
        });

        ticketNumberField.addActionListener(e -> validateTicket());

        formPanel.add(new JLabel("Enter Ticket Number:"));
        formPanel.add(ticketNumberField);
        formPanel.add(validateButton);
        formPanel.add(checkInButton);

        add(formPanel, BorderLayout.CENTER);
    }

    private void createDetailsArea() {

        detailsArea = new JTextArea(12, 45);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        detailsArea.setText("Enter a ticket number and select Validate Ticket.");

        JScrollPane scrollPane = new JScrollPane(detailsArea);
        scrollPane.setBorder(
                BorderFactory.createTitledBorder("Ticket Information")
        );

        add(scrollPane, BorderLayout.SOUTH);
    }

    private void validateTicket() {

        String ticketNumber = ticketNumberField.getText().trim();

        validatedTicket = null;
        checkInButton.setEnabled(false);

        if (ticketNumber.isEmpty()) {
            showMessage("Please enter a ticket number.");
            return;
        }

        try {
            Ticket ticket = checkInService.validateTicket(ticketNumber);

            validatedTicket = ticket;
            displayTicket(ticket);

            checkInButton.setEnabled(true);

            JOptionPane.showMessageDialog(
                    this,
                    "Ticket is valid. The attendee can be checked in.",
                    "Validation Successful",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {
            detailsArea.setText("TICKET VALIDATION FAILED\n\n" + ex.getMessage());
            showError(ex.getMessage());
        }
    }

    private void checkInAttendee() {

        if (validatedTicket == null) {
            showMessage("Validate the ticket before checking in.");
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Confirm check-in for " +
                        validatedTicket.getAttendeeName() + "?",
                "Confirm Attendee Check-In",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Ticket checkedInTicket = checkInService.checkIn(
                    validatedTicket.getTicketNumber(),
                    session
            );

            validatedTicket = checkedInTicket;
            displayTicket(checkedInTicket);

            checkInButton.setEnabled(false);

            JOptionPane.showMessageDialog(
                    this,
                    "Attendee checked in successfully.",
                    "Check-In Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {
            validatedTicket = null;
            checkInButton.setEnabled(false);
            detailsArea.append("\n\nCHECK-IN FAILED\n" + ex.getMessage());
            showError(ex.getMessage());
        }
    }

    private void invalidateValidation() {
        if (validatedTicket != null) {
            validatedTicket = null;
            checkInButton.setEnabled(false);
            detailsArea.setText("Ticket number changed. Validate the ticket again before check-in.");
        }
    }

    private void displayTicket(Ticket ticket) {

        detailsArea.setText(
                "TICKET INFORMATION\n" +
                "----------------------------------------\n" +
                "Ticket Number : " + ticket.getTicketNumber() + "\n" +
                "Attendee      : " + ticket.getAttendeeName() + "\n" +
                "Email         : " + ticket.getAttendeeEmail() + "\n" +
                "Phone         : " + ticket.getAttendeePhone() + "\n" +
                "Event         : " + ticket.getEventName() + "\n" +
                "VIP Attendee  : " + (ticket.isVip() ? "Yes" : "No") + "\n" +
                "Status        : " + ticket.getStatus() + "\n" +
                "Issued At     : " + ticket.getIssuedAt() + "\n" +
                "Checked In At : " + ticket.getCheckedInAt() + "\n" +
                "----------------------------------------"
        );
    }

    private void showMessage(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Check-In",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Check-In Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
