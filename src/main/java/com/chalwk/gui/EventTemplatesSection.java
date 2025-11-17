package com.chalwk.gui;

import com.chalwk.config.AppConfig;
import com.chalwk.config.ConfigManager;
import com.chalwk.config.EventConfig;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class EventTemplatesSection extends JPanel {

    private final String serverName;
    private final EventTemplateTableModel tableModel;
    private final JTable eventTable;

    public EventTemplatesSection(String serverName, ConfigManager configManager) {
        this.serverName = serverName;

        // Use server-specific configs if available, otherwise use global configs as base
        Map<String, EventConfig> baseConfigs = configManager.getConfig().getEventConfigs();
        Map<String, EventConfig> workingConfigs = new HashMap<>();

        // Copy the base configs to working configs
        for (Map.Entry<String, EventConfig> entry : baseConfigs.entrySet()) {
            workingConfigs.put(entry.getKey(), new EventConfig(
                    entry.getValue().isEnabled(),
                    entry.getValue().getTemplate(),
                    entry.getValue().getColor(),
                    entry.getValue().isUseEmbed(),
                    entry.getValue().getChannelId()
            ));
        }

        // If we have server-specific configs, apply them over the base
        if (!"Global".equals(serverName)) {
            Map<String, EventConfig> serverConfigs = configManager.getConfig().getEventConfigsForServer(serverName);
            if (serverConfigs != null && serverConfigs != baseConfigs) {
                workingConfigs.putAll(serverConfigs);
            }
        }

        this.tableModel = new EventTemplateTableModel(workingConfigs, serverName, configManager);
        this.eventTable = new JTable(tableModel);
        initializeUI();
    }

    // Add getter method for table model
    public EventTemplateTableModel getTableModel() {
        return tableModel;
    }

    // Add getter method for event configs
    public Map<String, EventConfig> getEventConfigs() {
        return tableModel.getEventConfigs();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Event Templates for " + serverName));

        // Table setup
        eventTable.setRowHeight(25);
        eventTable.getTableHeader().setReorderingAllowed(false);

        // Set column widths
        eventTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // Enabled
        eventTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Event Type
        eventTable.getColumnModel().getColumn(2).setPreferredWidth(300); // Template
        eventTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Color
        eventTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Use Embed
        eventTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Channel

        JScrollPane scrollPane = new JScrollPane(eventTable);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        // Control buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.addActionListener(e -> resetToDefaults());

        controlPanel.add(resetButton);

        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void saveToConfig(Map<String, EventConfig> eventConfigs) {
        tableModel.saveToConfig(eventConfigs);
    }

    private void resetToDefaults() {
        int result = JOptionPane.showConfirmDialog(this,
                "Reset all event templates for " + serverName + " to default values?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            tableModel.resetToDefaults();
        }
    }

    // Table model for event templates
    private static class EventTemplateTableModel extends AbstractTableModel {
        private final String[] columnNames = {
                "Enabled", "Event Type", "Template", "Color", "Use Embed", "Channel"
        };

        private final Map<String, EventConfig> eventConfigs;
        private final String[] eventKeys;
        private final String serverName;
        private final ConfigManager configManager;

        public EventTemplateTableModel(Map<String, EventConfig> eventConfigs, String serverName, ConfigManager configManager) {
            this.eventConfigs = eventConfigs;
            this.serverName = serverName;
            this.configManager = configManager;
            this.eventKeys = eventConfigs.keySet().toArray(new String[0]);
        }

        // Add getter for eventConfigs
        public Map<String, EventConfig> getEventConfigs() {
            return eventConfigs;
        }

        @Override
        public int getRowCount() {
            return eventKeys.length;
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
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 4 -> Boolean.class; // Enabled, Use Embed
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true; // All cells are editable
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String eventKey = eventKeys[rowIndex];
            EventConfig config = eventConfigs.get(eventKey);

            return switch (columnIndex) {
                case 0 -> config.isEnabled();
                case 1 -> eventKey;
                case 2 -> config.getTemplate();
                case 3 -> config.getColor() != null ? config.getColor() : "Default";
                case 4 -> config.isUseEmbed();
                case 5 -> config.getChannelId();
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            String eventKey = eventKeys[rowIndex];
            EventConfig config = eventConfigs.get(eventKey);

            switch (columnIndex) {
                case 0 -> config.setEnabled((Boolean) aValue);
                case 2 -> config.setTemplate((String) aValue);
                case 3 -> config.setColor(aValue.equals("Default") ? null : (String) aValue);
                case 4 -> config.setUseEmbed((Boolean) aValue);
                case 5 -> config.setChannelId((String) aValue);
            }

            fireTableCellUpdated(rowIndex, columnIndex);

            // Auto-save server-specific configurations
            if (!"Global".equals(serverName)) {
                configManager.getConfig().setEventConfigsForServer(serverName, eventConfigs);
                try {
                    configManager.saveConfig(configManager.getConfig());
                } catch (Exception e) {
                    // Log error but don't show dialog to avoid spamming user during typing
                    System.err.println("Error auto-saving configuration: " + e.getMessage());
                }
            }
        }

        public void saveToConfig(Map<String, EventConfig> targetConfigs) {
            if ("Global".equals(serverName)) {
                targetConfigs.clear();
                targetConfigs.putAll(eventConfigs);
            }
        }

        public void resetToDefaults() {
            // Create a new default config to get default values
            AppConfig defaultConfig = new AppConfig();
            Map<String, EventConfig> defaults = defaultConfig.getEventConfigs();

            for (String eventKey : eventKeys) {
                EventConfig defaultConfigValue = defaults.get(eventKey);
                if (defaultConfigValue != null) {
                    EventConfig currentConfig = eventConfigs.get(eventKey);
                    currentConfig.setEnabled(defaultConfigValue.isEnabled());
                    currentConfig.setTemplate(defaultConfigValue.getTemplate());
                    currentConfig.setColor(defaultConfigValue.getColor());
                    currentConfig.setUseEmbed(defaultConfigValue.isUseEmbed());
                    currentConfig.setChannelId(defaultConfigValue.getChannelId());
                }
            }

            fireTableDataChanged();

            // Auto-save after reset
            if (!"Global".equals(serverName)) {
                configManager.getConfig().setEventConfigsForServer(serverName, eventConfigs);
                try {
                    configManager.saveConfig(configManager.getConfig());
                } catch (Exception e) {
                    System.err.println("Error saving after reset: " + e.getMessage());
                }
            }
        }
    }
}