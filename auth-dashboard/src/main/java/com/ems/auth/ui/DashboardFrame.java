package com.ems.auth.ui;

import com.ems.auth.service.AuthService;
import com.ems.auth.service.AuthorizationService;
import com.ems.shared.model.UserSession;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.Objects;

/** Main application window. Module screens can be added as their panels are delivered. */
public class DashboardFrame extends JFrame {

    public DashboardFrame(AuthService authService, UserSession session, Runnable onLogout) {
        super("Event Management System");
        Objects.requireNonNull(authService, "authService");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(onLogout, "onLogout");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("  " + session.getFullName() + "  •  " + session.getRole()),
                BorderLayout.WEST);
        javax.swing.JButton logout = new javax.swing.JButton("Log out");
        logout.addActionListener(event -> {
            dispose();
            onLogout.run();
        });
        header.add(logout, BorderLayout.EAST);
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        AuthorizationService authorization = new AuthorizationService();
        if (authorization.canAccess(session, AuthorizationService.Feature.DASHBOARD))
            tabs.addTab(session.hasRole("ADMIN") ? "Admin dashboard" : "Dashboard",
                    new DashboardPanel(session));
        if (authorization.canAccess(session, AuthorizationService.Feature.USER_MANAGEMENT))
            tabs.addTab("User accounts", new UserManagementPanel(session));
        if (authorization.canAccess(session, AuthorizationService.Feature.STAFF_ASSIGNMENTS))
            tabs.addTab("Staff assignments", new StaffAssignmentPanel(session));
        if (authorization.canAccess(session, AuthorizationService.Feature.MY_DUTIES))
            tabs.addTab("My duties", new StaffAssignmentPanel(session));
        if (authorization.canAccess(session, AuthorizationService.Feature.EVENTS))
            tabs.addTab("Events", unavailableModule("Event management"));
        if (authorization.canAccess(session, AuthorizationService.Feature.ATTENDEES))
            tabs.addTab("Attendees", unavailableModule("Attendee management"));
        if (authorization.canAccess(session, AuthorizationService.Feature.TICKETS)
                || authorization.canAccess(session, AuthorizationService.Feature.CHECK_IN))
            tabs.addTab("Tickets and check-in", unavailableModule("Ticket and check-in"));
        if (authorization.canAccess(session, AuthorizationService.Feature.ROOMS))
            tabs.addTab("Rooms", unavailableModule("Room management"));
        if (authorization.canAccess(session, AuthorizationService.Feature.VENDORS))
            tabs.addTab("Vendors", unavailableModule("Vendor management"));
        if (authorization.canAccess(session, AuthorizationService.Feature.ATTENDANCE_REPORTS))
            tabs.addTab("Reports", unavailableModule("Attendance reports"));
        if (authorization.canAccess(session, AuthorizationService.Feature.NOTIFICATIONS))
            tabs.addTab("Notifications", unavailableModule("Notifications"));
        add(tabs, BorderLayout.CENTER);
        setSize(1000, 650);
        setLocationRelativeTo(null);
    }

    private JPanel unavailableModule(String moduleName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(moduleName + " panel is not available yet.", JLabel.CENTER),
                BorderLayout.CENTER);
        return panel;
    }
}
