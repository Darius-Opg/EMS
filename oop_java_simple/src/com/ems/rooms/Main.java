package com.ems.rooms;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Run graphical panels cleanly inside the Swing UI event dispatch threads
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Event Management System - Room Module Panel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(850, 500);
            frame.setLocationRelativeTo(null); // Center application display block on desktop screens

            RoomService service = new RoomService();
            RoomManagementPanel panel = new RoomManagementPanel(service);
            
            frame.add(panel);
            frame.setVisible(true);
        });
    }
}
