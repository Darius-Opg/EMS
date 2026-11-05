package com.ems.shared.utils;


import javax.swing.JOptionPane;
import java.awt.Component;

public final class NotificationUtil {

    private NotificationUtil() {
    }

    public static void showInfo(
            Component parent,
            String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(
            Component parent,
            String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Warning",
                JOptionPane.WARNING_MESSAGE);
    }

    public static void showError(
            Component parent,
            String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(
            Component parent,
            String message) {
        return JOptionPane.showConfirmDialog(
                parent,
                message,
                "Confirm Action",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
