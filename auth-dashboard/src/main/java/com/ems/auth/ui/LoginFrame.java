package com.ems.auth.ui;

import com.ems.auth.service.AuthService;
import com.ems.shared.model.UserSession;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/** Login window. The authenticated session is passed to the application callback. */
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private final Consumer<UserSession> onAuthenticated;
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton loginButton = new JButton("Sign in");

    public LoginFrame(AuthService authService, Consumer<UserSession> onAuthenticated) {
        super("Event Management System — Sign in");
        this.authService = Objects.requireNonNull(authService, "authService");
        this.onAuthenticated = Objects.requireNonNull(onAuthenticated, "onAuthenticated");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(380, 340));
        setContentPane(buildContent());
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(this::attemptLogin);
        passwordField.addActionListener(this::attemptLogin);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JLabel heading = new JLabel("Sign in to your account");
        heading.setFont(heading.getFont().deriveFont(26f));
        content.add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 6, 0);
        fields.add(new JLabel("Username or email"), constraints);
        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 14, 0);
        fields.add(usernameField, constraints);
        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        constraints.insets = new Insets(0, 0, 6, 0);
        fields.add(new JLabel("Password"), constraints);
        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 10, 0);
        fields.add(passwordField, constraints);
        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 8, 0);
        messageLabel.setForeground(new java.awt.Color(170, 35, 35));
        fields.add(messageLabel, constraints);
        Dimension inputSize = new Dimension(210, 42);
        usernameField.setPreferredSize(inputSize);
        passwordField.setPreferredSize(inputSize);
        JPanel centeredFields = new JPanel(new GridBagLayout());
        centeredFields.add(fields);
        content.add(centeredFields, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        actions.add(loginButton, BorderLayout.EAST);
        content.add(actions, BorderLayout.SOUTH);
        return content;
    }

    private void attemptLogin(ActionEvent event) {
        loginButton.setEnabled(false);
        messageLabel.setText(" ");
        char[] password = passwordField.getPassword();
        try {
            UserSession session = authService.login(usernameField.getText(), password);
            passwordField.setText("");
            onAuthenticated.accept(session);
            dispose();
        } catch (AuthService.AuthenticationException exception) {
            messageLabel.setText(exception.getMessage());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        } finally {
            Arrays.fill(password, '\0');
            loginButton.setEnabled(true);
        }
    }
}
