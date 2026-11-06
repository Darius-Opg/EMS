package com.ems;

import com.ems.auth.service.AuthService;
import com.ems.auth.ui.LoginFrame;
import com.ems.auth.ui.DashboardFrame;
import com.ems.shared.model.UserSession;
import com.ems.shared.utils.NotificationUtil;

import javax.swing.SwingUtilities;

/** Application entry point. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> showLogin(new AuthService()));
    }

    private static void showLogin(AuthService authService) {
        LoginFrame loginFrame = new LoginFrame(authService,
                session -> showHome(authService, session));
        loginFrame.setVisible(true);
    }

    private static void showHome(AuthService authService, UserSession session) {
        DashboardFrame frame = new DashboardFrame(authService, session, () -> {
            authService.logout();
            NotificationUtil.clearHistory();
            showLogin(authService);
        });
        frame.setVisible(true);
    }
}
