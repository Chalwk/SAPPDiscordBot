package com.chalwk.gui;

import com.chalwk.model.RawEvent;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class EventLogPanel extends JPanel {

    private final EventTableModel tableModel;
    private final JTable eventTable;
    private final JCheckBox autoScrollCheckbox;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public EventLogPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table model and table
        tableModel = new EventTableModel();
        eventTable = new JTable(tableModel);

        // Table styling
        eventTable.setFillsViewportHeight(true);
        eventTable.setRowHeight(25);
        eventTable.setShowGrid(true);
        eventTable.setGridColor(new Color(240, 240, 240));
        eventTable.setSelectionBackground(new Color(220, 240, 255));
        eventTable.setSelectionForeground(Color.BLACK);
        eventTable.setFont(eventTable.getFont().deriveFont(12f));

        // Header styling
        eventTable.getTableHeader().setFont(eventTable.getFont().deriveFont(Font.BOLD));
        eventTable.getTableHeader().setBackground(new Color(70, 130, 180));
        eventTable.getTableHeader().setForeground(Color.BLACK);
        eventTable.getTableHeader().setReorderingAllowed(false);

        eventTable.setAutoCreateRowSorter(true);
        eventTable.setDefaultRenderer(Object.class, new EventLogCellRenderer());

        JScrollPane scrollPane = new JScrollPane(eventTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder("Event Log"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        scrollPane.setPreferredSize(new Dimension(800, 400));

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));

        autoScrollCheckbox = new JCheckBox("Auto-scroll to latest events", true);
        autoScrollCheckbox.setFont(autoScrollCheckbox.getFont().deriveFont(Font.BOLD));

        JButton clearButton = new JButton("Clear Log");
        clearButton.setBackground(new Color(198, 40, 40));
        clearButton.setForeground(Color.BLACK);
        clearButton.setFocusPainted(false);

        clearButton.addActionListener(e -> clearLog());

        controlPanel.add(autoScrollCheckbox);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(clearButton);

        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void addEvent(RawEvent event, String serverName, String status) {
        SwingUtilities.invokeLater(() -> {
            tableModel.addEvent(event, serverName, status);
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

    private static class EventLogEntry {
        String timestamp;
        String serverName;
        String eventType;
        String subtype;
        String content;
        String status;

        EventLogEntry(RawEvent event, String serverName, String status) {
            this.timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            this.serverName = serverName;
            this.eventType = event.getEvent_type();
            this.subtype = event.getSubtype();
            this.status = status;

            // Build content from event data
            this.content = buildContentFromData(event.getData());

            // Truncate long content for display
            if (this.content.length() > 100) {
                this.content = this.content.substring(0, 97) + "...";
            }
        }

        private String buildContentFromData(Map<String, Object> data) {
            if (data == null || data.isEmpty()) {
                return "No data";
            }

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append(": ").append(entry.getValue());

                // Limit the total length
                if (sb.length() > 80) {
                    sb.append("...");
                    break;
                }
            }
            return sb.toString();
        }
    }

    private static class EventLogCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
            }

            // Color code status column
            if (column == 5 && value != null) {
                String status = value.toString().toLowerCase();
                if (status.contains("success") || status.contains("processed")) {
                    c.setForeground(new Color(0, 128, 0)); // Dark green
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else if (status.contains("fail") || status.contains("error")) {
                    c.setForeground(Color.RED);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(Color.BLACK);
                }
            }

            return c;
        }
    }

    private static class EventTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Time", "Server", "Event Type", "Subtype", "Content", "Status"};
        private final List<EventLogEntry> events = new ArrayList<>();

        public void addEvent(RawEvent event, String serverName, String status) {
            events.add(new EventLogEntry(event, serverName, status));
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
                case 1 -> entry.serverName;
                case 2 -> entry.eventType;
                case 3 -> entry.subtype != null ? entry.subtype : "";
                case 4 -> entry.content;
                case 5 -> entry.status;
                default -> "";
            };
        }
    }
}