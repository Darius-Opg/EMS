package com.ems.auth.ui;

import com.ems.auth.dao.DashboardDAO;
import com.ems.auth.dao.DashboardDAO.DashboardSummary;
import com.ems.auth.dao.DashboardDAO.UpcomingEvent;
import com.ems.shared.model.UserSession;
import com.ems.shared.utils.EventTime;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.Objects;

/** Dashboard summary backed by aggregate data from the shared database. */
public class DashboardPanel extends JPanel {

    private final DashboardDAO dashboardDAO;
    private final JLabel status = new JLabel(" ");
    private final JLabel[] countLabels = {
            new JLabel("—", JLabel.CENTER), new JLabel("—", JLabel.CENTER),
            new JLabel("—", JLabel.CENTER), new JLabel("—", JLabel.CENTER)
    };
    private final DefaultTableModel eventModel = new DefaultTableModel(
            new Object[]{"Event", "Venue", "Starts"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public DashboardPanel() {
        this(null, new DashboardDAO());
    }

    public DashboardPanel(DashboardDAO dashboardDAO) {
        this(null, dashboardDAO);
    }

    public DashboardPanel(UserSession session) {
        this(session, new DashboardDAO());
    }

    public DashboardPanel(UserSession session, DashboardDAO dashboardDAO) {
        this.dashboardDAO = Objects.requireNonNull(dashboardDAO, "dashboardDAO");
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        String[] headings = {"Events", "Upcoming events", "Registered attendees", "Guests checked in"};
        JPanel metrics = new JPanel(new GridLayout(1, headings.length, 12, 0));
        for (int i = 0; i < headings.length; i++) {
            JPanel card = new JPanel(new BorderLayout(4, 8));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(12, 8, 12, 8)));
            card.add(new JLabel(headings[i], JLabel.CENTER), BorderLayout.NORTH);
            countLabels[i].setFont(countLabels[i].getFont().deriveFont(24f));
            card.add(countLabels[i], BorderLayout.CENTER);
            metrics.add(card);
        }

        JPanel body = new JPanel(new BorderLayout(8, 8));
        JPanel overview = new JPanel(new BorderLayout(2, 2));
        String role = session == null ? "" : session.getRole();
        String dashboardTitle = switch (role == null ? "" : role.toUpperCase(java.util.Locale.ROOT)) {
            case "ADMIN" -> "Admin dashboard";
            case "ORGANIZER" -> "Organizer dashboard";
            case "STAFF" -> "Staff dashboard";
            default -> "Dashboard";
        };
        overview.add(new JLabel(dashboardTitle), BorderLayout.NORTH);
        overview.add(new JLabel("Upcoming events"), BorderLayout.SOUTH);
        body.add(overview, BorderLayout.NORTH);
        body.add(new JScrollPane(new JTable(eventModel)), BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(metrics, BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refreshSummary());
        top.add(refresh, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        refreshSummary();
    }

    public void refreshSummary() {
        try {
            DashboardSummary summary = dashboardDAO.loadSummary();
            countLabels[0].setText(Long.toString(summary.eventCount()));
            countLabels[1].setText(Long.toString(summary.upcomingEventCount()));
            countLabels[2].setText(Long.toString(summary.attendeeCount()));
            countLabels[3].setText(Long.toString(summary.checkedInCount()));
            eventModel.setRowCount(0);
            for (UpcomingEvent event : summary.upcomingEvents()) {
                eventModel.addRow(new Object[]{event.name(), event.venue(),
                        EventTime.formatDisplay(event.startsAt())});
            }
            status.setText(" ");
        } catch (SQLException | RuntimeException exception) {
            status.setText("Dashboard data is unavailable. Check the database connection and refresh.");
        }
    }
}
