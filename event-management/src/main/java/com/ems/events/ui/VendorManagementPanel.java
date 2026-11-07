
package com.ems.events.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.ems.events.model.Event;
import com.ems.events.model.Vendor;
import com.ems.events.service.EventService;
import com.ems.shared.model.UserSession;

public class VendorManagementPanel extends JPanel {

    private final UserSession session;
    private final EventService eventService;

    private final DefaultTableModel tableModel;
    private final JTable vendorTable;
    private final JTextField searchField;

    private final Map<Long, String> eventNames = new HashMap<>();

    public VendorManagementPanel(UserSession session, EventService eventService) {
        this.session = session;
        this.eventService = eventService;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        tableModel = new DefaultTableModel(
                new String[]{
                        "ID", "Event", "Vendor Name", "Service Type",
                        "Contact Person", "Phone", "Email"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        vendorTable = new JTable(tableModel);
        vendorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vendorTable.setAutoCreateRowSorter(true);

        searchField = new JTextField(20);

        add(createTopPanel(), BorderLayout.NORTH);
        add(new JScrollPane(vendorTable), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        loadVendors();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");

        panel.add(new JLabel("Search vendors:"));
        panel.add(searchField);
        panel.add(searchButton);
        panel.add(refreshButton);

        searchButton.addActionListener(e -> searchVendors());
        searchField.addActionListener(e -> searchVendors());

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadVendors();
        });

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addButton = new JButton("Add Vendor");
        JButton editButton = new JButton("Edit Vendor");
        JButton deleteButton = new JButton("Delete Vendor");

        boolean canManage = session != null &&
                (session.hasRole("ADMIN") || session.hasRole("ORGANIZER"));

        addButton.setEnabled(canManage);
        editButton.setEnabled(canManage);
        deleteButton.setEnabled(canManage);

        addButton.addActionListener(e -> addVendor());
        editButton.addActionListener(e -> editVendor());
        deleteButton.addActionListener(e -> deleteVendor());

        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);

        return panel;
    }

    private void loadVendors() {
        try {
            refreshEventNames();
            displayVendors(eventService.getAllVendors());
        } catch (Exception e) {
            showError("Could not load vendors: " + e.getMessage());
        }
    }

    private void refreshEventNames() throws Exception {
        eventNames.clear();

        List<Event> events = eventService.getAllEvents();

        for (Event event : events) {
            eventNames.put(event.getEventId(), event.getName());
        }
    }

    private void searchVendors() {
        try {
            refreshEventNames();
            List<Vendor> vendors =
                    eventService.searchVendors(searchField.getText());

            displayVendors(vendors);

        } catch (Exception e) {
            showError("Vendor search failed: " + e.getMessage());
        }
    }

    private void displayVendors(List<Vendor> vendors) {
        tableModel.setRowCount(0);

        for (Vendor vendor : vendors) {
            tableModel.addRow(new Object[]{
                    vendor.getVendorId(),
                    eventNames.getOrDefault(vendor.getEventId(), "Event #" + vendor.getEventId()),
                    vendor.getVendorName(),
                    vendor.getServiceType(),
                    vendor.getContactPerson(),
                    vendor.getPhone(),
                    vendor.getEmail()
            });
        }
    }

    private void addVendor() {
        Vendor vendor = showVendorForm(null);

        if (vendor == null) {
            return;
        }

        try {
            eventService.createVendor(vendor, session);
            showMessage("Vendor registered successfully.");
            loadVendors();

        } catch (Exception e) {
            showError("Could not register vendor: " + e.getMessage());
        }
    }

    private void editVendor() {
        Vendor selected = getSelectedVendor();

        if (selected == null) {
            showError("Please select a vendor to edit.");
            return;
        }

        Vendor updated = showVendorForm(selected);

        if (updated == null) {
            return;
        }

        try {
            eventService.updateVendor(updated, session);
            showMessage("Vendor updated successfully.");
            loadVendors();

        } catch (Exception e) {
            showError("Could not update vendor: " + e.getMessage());
        }
    }

    private void deleteVendor() {
        Vendor selected = getSelectedVendor();

        if (selected == null) {
            showError("Please select a vendor to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete vendor: " + selected.getVendorName() + "?",
                "Confirm deletion",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean deleted = eventService.deleteVendor(
                    selected.getVendorId(), session
            );

            if (deleted) {
                showMessage("Vendor deleted successfully.");
            } else {
                showError("Vendor could not be found.");
            }

            loadVendors();

        } catch (Exception e) {
            showError("Could not delete vendor: " + e.getMessage());
        }
    }

    private Vendor getSelectedVendor() {
        int selectedRow = vendorTable.getSelectedRow();

        if (selectedRow < 0) {
            return null;
        }

        int modelRow = vendorTable.convertRowIndexToModel(selectedRow);
        long vendorId = ((Number) tableModel.getValueAt(modelRow, 0)).longValue();

        try {
            return eventService.getAllVendors().stream()
                    .filter(v -> v.getVendorId() == vendorId)
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            showError("Could not retrieve vendor: " + e.getMessage());
            return null;
        }
    }

    private Vendor showVendorForm(Vendor existing) {
        JComboBox<Event> eventBox = new JComboBox<>();

        try {
            List<Event> events = eventService.getAllEvents();

            for (Event event : events) {
                eventBox.addItem(event);
            }

        } catch (Exception e) {
            showError("Could not load events: " + e.getMessage());
            return null;
        }

        eventBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );

                if (value instanceof Event event) {
                    setText(event.getName() + " (ID: " + event.getEventId() + ")");
                }

                return this;
            }
        });

        JTextField nameField = new JTextField(
                existing == null ? "" : existing.getVendorName()
        );

        JTextField serviceField = new JTextField(
                existing == null ? "" : existing.getServiceType()
        );

        JTextField contactField = new JTextField(
                existing == null || existing.getContactPerson() == null
                        ? "" : existing.getContactPerson()
        );

        JTextField phoneField = new JTextField(
                existing == null || existing.getPhone() == null
                        ? "" : existing.getPhone()
        );

        JTextField emailField = new JTextField(
                existing == null || existing.getEmail() == null
                        ? "" : existing.getEmail()
        );

        if (existing != null) {
            for (int i = 0; i < eventBox.getItemCount(); i++) {
                Event event = eventBox.getItemAt(i);

                if (event.getEventId() == existing.getEventId()) {
                    eventBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

        form.add(new JLabel("Associated event:"));
        form.add(eventBox);

        form.add(new JLabel("Vendor name:"));
        form.add(nameField);

        form.add(new JLabel("Service type:"));
        form.add(serviceField);

        form.add(new JLabel("Contact person:"));
        form.add(contactField);

        form.add(new JLabel("Phone:"));
        form.add(phoneField);

        form.add(new JLabel("Email:"));
        form.add(emailField);

        int result = JOptionPane.showConfirmDialog(
                this,
                form,
                existing == null ? "Register Vendor" : "Edit Vendor",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        Event selectedEvent = (Event) eventBox.getSelectedItem();

        if (selectedEvent == null) {
            showError("Please select an event.");
            return null;
        }

        Vendor vendor = existing == null ? new Vendor() : existing;

        vendor.setEventId(selectedEvent.getEventId());
        vendor.setVendorName(nameField.getText().trim());
        vendor.setServiceType(serviceField.getText().trim());
        vendor.setContactPerson(contactField.getText().trim());
        vendor.setPhone(phoneField.getText().trim());
        vendor.setEmail(emailField.getText().trim());

        return vendor;
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Vendor Management",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
