package com.ems.shared.utils;


import javax.swing.JOptionPane;
import java.awt.Component;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class NotificationUtil {

    private static final int MAX_NOTIFICATIONS = 500;
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final List<Notification> HISTORY = new ArrayList<>();
    private static final CopyOnWriteArrayList<Consumer<Notification>> LISTENERS =
            new CopyOnWriteArrayList<>();

    private NotificationUtil() {
    }

    public static void showInfo(
            Component parent,
            String message) {
        publish(Level.INFO, message);
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(
            Component parent,
            String message) {
        publish(Level.WARNING, message);
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Warning",
                JOptionPane.WARNING_MESSAGE);
    }

    public static void showError(
            Component parent,
            String message) {
        publish(Level.ERROR, message);
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

    /** Adds a notification to the shared in-memory feed and informs active panels. */
    public static Notification publish(Level level, String message) {
        if (level == null) throw new IllegalArgumentException("Notification level is required.");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Notification message is required.");
        }
        Notification notification = new Notification(
                NEXT_ID.incrementAndGet(), Instant.now(), level, message.trim());
        synchronized (HISTORY) {
            HISTORY.add(notification);
            if (HISTORY.size() > MAX_NOTIFICATIONS) HISTORY.remove(0);
        }
        LISTENERS.forEach(listener -> listener.accept(notification));
        return notification;
    }

    public static List<Notification> getRecentNotifications() {
        synchronized (HISTORY) {
            return List.copyOf(HISTORY);
        }
    }

    /** Clears notifications from the previous application session, for example on logout. */
    public static void clearHistory() {
        synchronized (HISTORY) {
            HISTORY.clear();
        }
    }

    /** Registers a listener and returns a handle that removes it when closed. */
    public static AutoCloseable addListener(Consumer<Notification> listener) {
        if (listener == null) throw new IllegalArgumentException("Notification listener is required.");
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    public enum Level { INFO, WARNING, ERROR }

    public record Notification(long id, Instant createdAt, Level level, String message) {
        @Override
        public String toString() {
            return "[" + level + "] " + message;
        }
    }
}
