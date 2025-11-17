package com.chalwk.gui;

import com.chalwk.config.AppConfig;
import com.chalwk.config.ConfigManager;
import com.chalwk.config.EventConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

public class OutputConfigPanel extends JPanel {

    private final ConfigManager configManager;

    private JTextField generalChannelField;
    private JTextField chatChannelField;
    private JTextField commandChannelField;

    public OutputConfigPanel(ConfigManager configManager) {
        this.configManager = configManager;
        initializeUI();
        loadConfig();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();

        // Channels Tab
        tabbedPane.addTab("Channels", createChannelsPanel());

        // Events Tab
        tabbedPane.addTab("Events", createEventsPanel());
        add(tabbedPane, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = createStyledButton("Save Configuration", new Color(46, 125, 50));
        JButton resetButton = createStyledButton("Reset to Defaults", new Color(198, 40, 40));

        saveButton.setForeground(Color.BLACK);
        resetButton.setForeground(Color.BLACK);

        saveButton.addActionListener(e -> saveConfig());
        resetButton.addActionListener(e -> resetConfig());

        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createChannelsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // General Channel
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("General Channel ID:"), gbc);
        gbc.gridx = 1;
        generalChannelField = new JTextField(30);
        panel.add(generalChannelField, gbc);

        // Chat Channel
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Chat Channel ID:"), gbc);
        gbc.gridx = 1;
        chatChannelField = new JTextField(30);
        panel.add(chatChannelField, gbc);

        // Command Channel
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Command Channel ID:"), gbc);
        gbc.gridx = 1;
        commandChannelField = new JTextField(30);
        panel.add(commandChannelField, gbc);

        // Help text
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JLabel helpLabel = new JLabel("<html><i>Enter Discord channel IDs where different types of events will be sent.</i></html>");
        helpLabel.setForeground(Color.GRAY);
        panel.add(helpLabel, gbc);

        return panel;
    }

    private JPanel createEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create table model for events
        String[] columnNames = {"Enable", "Event", "Template", "Color", "Embed", "Channel"};
        EventTableModel model = new EventTableModel(columnNames);

        // Populate with events
        AppConfig config = configManager.getConfig();
        for (Map.Entry<String, EventConfig> entry : config.getEventConfigs().entrySet()) {
            model.addEvent(entry.getKey(), entry.getValue());
        }

        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(2).setPreferredWidth(300); // Wider template column

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new TitledBorder("Event Configuration"));

        panel.add(scrollPane, BorderLayout.CENTER);

        // Quick templates panel
        panel.add(createQuickTemplatesPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createQuickTemplatesPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(new TitledBorder("Quick Templates"));

        JButton defaultTemplatesBtn = new JButton("Load Default Templates");
        defaultTemplatesBtn.addActionListener(e -> loadDefaultTemplates());

        panel.add(defaultTemplatesBtn);
        return panel;
    }

    private void loadDefaultTemplates() {
        // TODO: Implementation to load default templates
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private void loadConfig() {
        AppConfig config = configManager.getConfig();
        generalChannelField.setText(config.getChannels().get("GENERAL"));
        chatChannelField.setText(config.getChannels().get("CHAT"));
        commandChannelField.setText(config.getChannels().get("COMMAND"));
    }

    private void saveConfig() {
        try {
            AppConfig config = configManager.getConfig();

            // Save channels
            config.getChannels().put("GENERAL", generalChannelField.getText().trim());
            config.getChannels().put("CHAT", chatChannelField.getText().trim());
            config.getChannels().put("COMMAND", commandChannelField.getText().trim());

            configManager.saveConfig(config);
            JOptionPane.showMessageDialog(this, "Configuration saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetConfig() {
        int result = JOptionPane.showConfirmDialog(this,
                "Reset all settings to defaults?", "Confirm Reset", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            configManager.resetToDefaults();
            loadConfig();
        }
    }

    private static class EventTableRow {
        String eventName;
        EventConfig config;

        EventTableRow(String eventName, EventConfig config) {
            this.eventName = eventName;
            this.config = config;
        }
    }

    // Table model for events
    private static class EventTableModel extends javax.swing.table.AbstractTableModel {
        private final String[] columnNames;
        private final java.util.List<EventTableRow> data = new java.util.ArrayList<>();

        public EventTableModel(String[] columnNames) {
            this.columnNames = columnNames;
        }

        public void addEvent(String eventName, EventConfig config) {
            data.add(new EventTableRow(eventName, config));
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return switch (col) {
                case 0 -> Boolean.class; // Enable
                case 3, 5 -> String.class; // Color, Channel
                case 4 -> Boolean.class; // Embed
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        @Override
        public Object getValueAt(int row, int col) {
            EventTableRow eventRow = data.get(row);
            return switch (col) {
                case 0 -> eventRow.config.isEnabled();
                case 1 -> eventRow.eventName;
                case 2 -> eventRow.config.getTemplate();
                case 3 -> eventRow.config.getColor();
                case 4 -> eventRow.config.isUseEmbed();
                case 5 -> eventRow.config.getChannelId();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            EventTableRow eventRow = data.get(row);
            switch (col) {
                case 0 -> eventRow.config.setEnabled((Boolean) value);
                case 2 -> eventRow.config.setTemplate((String) value);
                case 3 -> eventRow.config.setColor((String) value);
                case 4 -> eventRow.config.setUseEmbed((Boolean) value);
                case 5 -> eventRow.config.setChannelId((String) value);
            }
            fireTableCellUpdated(row, col);
        }
    }
}