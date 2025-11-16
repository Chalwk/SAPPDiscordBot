package com.chalwk.gui;

import com.chalwk.model.DiscordEvent;
import com.chalwk.model.EventEmbed;
import com.chalwk.model.EventMessage;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventLogPanel extends JPanel {

    private final EventTableModel tableModel;
    private final JTable eventTable;
    private final JScrollPane scrollPane;
    private final JCheckBox autoScrollCheckbox;
    private final JButton clearButton;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public EventLogPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model and table
        tableModel = new EventTableModel();
        eventTable = new JTable(tableModel);
        eventTable.setAutoCreateRowSorter(true);
        eventTable.setFillsViewportHeight(true);

        // Custom renderer for better appearance
        eventTable.setDefaultRenderer(Object.class, new EventLogCellRenderer());

        scrollPane = new JScrollPane(eventTable);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        autoScrollCheckbox = new JCheckBox("Auto-scroll", true);
        clearButton = new JButton("Clear Log");

        clearButton.addActionListener(e -> clearLog());

        controlPanel.add(autoScrollCheckbox);
        controlPanel.add(clearButton);

        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void addEvent(DiscordEvent event, String status) {
        SwingUtilities.invokeLater(() -> {
            tableModel.addEvent(event, status);

            // Auto-scroll to bottom if enabled
            if (autoScrollCheckbox.isSelected()) {
                scrollToBottom();
            }
        });
    }

    public void clearLog() {
        tableModel.clearEvents();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            int lastRow = tableModel.getRowCount() - 1;
            if (lastRow >= 0) {
                eventTable.scrollRectToVisible(eventTable.getCellRect(lastRow, 0, true));
            }
        });
    }

    private class EventTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Time", "Type", "Channel", "Content", "Status"};
        private final List<EventLogEntry> events = new ArrayList<>();

        public void addEvent(DiscordEvent event, String status) {
            events.add(new EventLogEntry(event, status));
            fireTableRowsInserted(events.size() - 1, events.size() - 1);
        }

        public void clearEvents() {
            int size = events.size();
            events.clear();
            fireTableRowsDeleted(0, size - 1);
        }

        @Override
        public int getRowCount() {
            return events.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= events.size()) return "";

            EventLogEntry entry = events.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> entry.timestamp;
                case 1 -> entry.type;
                case 2 -> entry.channel;
                case 3 -> entry.content;
                case 4 -> entry.status;
                default -> "";
            };
        }
    }

    private static class EventLogEntry {
        String timestamp;
        String type;
        String channel;
        String content;
        String status;

        EventLogEntry(DiscordEvent event, String status) {
            this.timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            this.status = status;

            if (event.getMessage() != null) {
                this.type = "Message";
                this.channel = event.getMessage().getChannel_id();
                this.content = event.getMessage().getText();
            } else if (event.getEmbed() != null) {
                this.type = "Embed";
                this.channel = event.getEmbed().getChannel_id();
                this.content = event.getEmbed().getTitle() != null ?
                        event.getEmbed().getTitle() : "No Title";
            } else {
                this.type = "Unknown";
                this.channel = "N/A";
                this.content = "Invalid Event";
            }

            // Truncate long content for display
            if (this.content != null && this.content.length() > 100) {
                this.content = this.content.substring(0, 97) + "...";
            }
        }
    }

    private static class EventLogCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Color code status column
            if (column == 4 && value != null) {
                String status = value.toString();
                if (status.equalsIgnoreCase("success")) {
                    c.setForeground(Color.GREEN.darker());
                } else if (status.equalsIgnoreCase("failed")) {
                    c.setForeground(Color.RED);
                } else {
                    c.setForeground(Color.BLACK);
                }
            }

            return c;
        }
    }
}