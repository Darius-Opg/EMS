package com.ems.auth.ui;

import com.ems.auth.model.User;
import com.ems.auth.service.UserService;
import com.ems.shared.model.UserSession;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** User account search and management screen. */
public class UserManagementPanel extends JPanel {

    private final UserService userService;
    private final UserSession session;
    private final JTextField searchField = new JTextField(24);
    private List<User> visibleUsers = List.of();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Full name", "Username", "Email", "Role", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable usersTable = new JTable(tableModel);

    public UserManagementPanel(UserSession session) {
        this(session, new UserService());
    }

    public UserManagementPanel(UserSession session, UserService userService) {
        this.session = Objects.requireNonNull(session, "session");
        this.userService = Objects.requireNonNull(userService, "userService");
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        searchPanel.add(new JLabel("Search username or full name:"));
        searchPanel.add(searchField);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(event -> refreshUsers());
        searchField.addActionListener(event -> refreshUsers());
        searchPanel.add(searchButton);
        add(searchPanel, BorderLayout.NORTH);

        usersTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(usersTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        JButton addButton = new JButton("Add user");
        JButton editButton = new JButton("Edit selected");
        JButton statusButton = new JButton("Activate / deactivate");
        JButton refreshButton = new JButton("Refresh");
        addButton.addActionListener(event -> showUserEditor(null));
        editButton.addActionListener(event -> editSelectedUser());
        statusButton.addActionListener(event -> toggleSelectedUser());
        refreshButton.addActionListener(event -> refreshUsers());
        actions.add(addButton);
        actions.add(editButton);
        actions.add(statusButton);
        actions.add(refreshButton);
        add(actions, BorderLayout.SOUTH);

        refreshUsers();
    }

    private void refreshUsers() {
        try {
            List<User> users = userService.findUsers(session, searchField.getText());
            visibleUsers = users;
            tableModel.setRowCount(0);
            for (User user : users) {
                tableModel.addRow(new Object[]{
                        user.getFullName(), user.getUsername(), user.getEmail(),
                        user.getRole(), user.isActive() ? "Active" : "Inactive"});
            }
        } catch (UserService.UserManagementException exception) {
            showError(exception.getMessage());
        }
    }

    private void editSelectedUser() {
        User selected = getSelectedUser();
        if (selected != null) {
            showUserEditor(selected);
        }
    }

    private void showUserEditor(User selected) {
        boolean creating = selected == null;
        JTextField fullNameField = new JTextField(24);
        JTextField usernameField = new JTextField(24);
        JTextField emailField = new JTextField(24);
        JComboBox<String> roleField = new JComboBox<>(new String[]{"ADMIN", "ORGANIZER", "STAFF"});
        JPasswordField passwordField = new JPasswordField(24);
        if (!creating) {
            fullNameField.setText(selected.getFullName());
            usernameField.setText(selected.getUsername());
            emailField.setText(selected.getEmail());
            roleField.setSelectedItem(selected.getRole());
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Full name"));
        form.add(fullNameField);
        form.add(new JLabel("Username"));
        form.add(usernameField);
        form.add(new JLabel("Email"));
        form.add(emailField);
        form.add(new JLabel("Role"));
        form.add(roleField);
        if (creating) {
            form.add(new JLabel("Password (8+ characters)"));
            form.add(passwordField);
        }

        int result = JOptionPane.showConfirmDialog(this, form,
                creating ? "Create user account" : "Edit user account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        char[] password = creating ? passwordField.getPassword() : null;
        try {
            if (creating) {
                userService.register(session, fullNameField.getText(), usernameField.getText(),
                        emailField.getText(), password, (String) roleField.getSelectedItem());
            } else {
                userService.updateUser(session, selected.getId(), fullNameField.getText(),
                        usernameField.getText(), emailField.getText(),
                        (String) roleField.getSelectedItem());
            }
            refreshUsers();
        } catch (UserService.UserManagementException exception) {
            showError(exception.getMessage());
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    private void toggleSelectedUser() {
        User selected = getSelectedUser();
        if (selected == null) {
            return;
        }
        boolean activate = !selected.isActive();
        String action = activate ? "activate" : "deactivate";
        int answer = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + action + " " + selected.getUsername() + "?",
                "Confirm account status", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            userService.setActive(session, selected.getId(), activate);
            refreshUsers();
        } catch (UserService.UserManagementException exception) {
            showError(exception.getMessage());
        }
    }

    private User getSelectedUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0) {
            showError("Select a user account first.");
            return null;
        }
        int modelRow = usersTable.convertRowIndexToModel(row);
        return modelRow < visibleUsers.size() ? visibleUsers.get(modelRow) : null;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "User management",
                JOptionPane.ERROR_MESSAGE);
    }
}
