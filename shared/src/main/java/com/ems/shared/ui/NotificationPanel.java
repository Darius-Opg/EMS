package com.ems.shared.ui;

import com.ems.shared.utils.NotificationUtil;
import com.ems.shared.utils.NotificationUtil.Notification;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Displays recent in-memory notifications published through NotificationUtil. */
public final class NotificationPanel extends JPanel {

    private static final int MAX_VISIBLE_NOTIFICATIONS = 500;
    private static final String EMPTY_CARD = "empty";
    private static final String LIST_CARD = "list";
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final DefaultListModel<Notification> model = new DefaultListModel<>();
    private final Set<Long> displayedIds = new HashSet<>();
    private final AtomicBoolean attached = new AtomicBoolean();
    private final JList<Notification> notificationList = new JList<>(model);
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private AutoCloseable subscription;

    public NotificationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        notificationList.setCellRenderer(notificationRenderer());
        content.add(new JLabel("No notifications yet.", JLabel.CENTER), EMPTY_CARD);
        content.add(new JScrollPane(notificationList), LIST_CARD);
        add(content, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!attached.compareAndSet(false, true)) return;
        subscription = NotificationUtil.addListener(this::appendOnEdt);
        NotificationUtil.getRecentNotifications().forEach(this::appendOnEdt);
    }

    @Override
    public void removeNotify() {
        closeSubscription();
        super.removeNotify();
    }

    private void appendOnEdt(Notification notification) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendOnEdt(notification));
            return;
        }
        if (!displayedIds.add(notification.id())) return;
        if (model.size() == MAX_VISIBLE_NOTIFICATIONS) {
            displayedIds.remove(model.get(0).id());
            model.remove(0);
        }
        model.addElement(notification);
        cards.show(content, LIST_CARD);
        notificationList.ensureIndexIsVisible(model.size() - 1);
    }

    private ListCellRenderer<? super Notification> notificationRenderer() {
        return (list, notification, index, isSelected, cellHasFocus) -> {
            JLabel label = (JLabel) new DefaultListCellRenderer().getListCellRendererComponent(
                    list, notification, index, isSelected, cellHasFocus);
            label.setText(TIMESTAMP.format(notification.createdAt()) + "  " + notification);
            return label;
        };
    }

    private void closeSubscription() {
        if (!attached.compareAndSet(true, false)) return;
        if (subscription != null) {
            try {
                subscription.close();
            } catch (Exception ignored) {
                // Closing a listener registration has no recoverable failure mode.
            }
            subscription = null;
        }
    }
}
