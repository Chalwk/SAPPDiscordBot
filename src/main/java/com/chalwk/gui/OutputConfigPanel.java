package com.chalwk.gui;

import com.chalwk.config.AppConfig;
import com.chalwk.config.ConfigManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class OutputConfigPanel extends JPanel {

    private final ConfigManager configManager;
    private final Map<String, ServerChannelPanel> serverPanels;
    private JTabbedPane channelTabs;
    private JComboBox<String> serverSelector;

    // Global channel fields
    private JTextField globalGeneralField;
    private JTextField globalChatField;
    private JTextField globalCommandField;

    public OutputConfigPanel(ConfigManager configManager) {
        this.configManager = configManager;
        this.serverPanels = new HashMap<>();
        initializeUI();
        loadConfig();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Server management panel
        JPanel serverManagementPanel = createServerManagementPanel();

        // Channel tabs
        channelTabs = new JTabbedPane();

        // Global channels tab (for default/fallback)
        channelTabs.addTab("Global Channels", createGlobalChannelsPanel());

        // Buttons Panel
        JPanel buttonPanel = createButtonPanel();

        add(serverManagementPanel, BorderLayout.NORTH);
        add(channelTabs, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createServerManagementPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Server Management"));

        panel.add(new JLabel("Manage Servers:"));

        serverSelector = new JComboBox<>();
        serverSelector.setPreferredSize(new Dimension(150, 25));
        serverSelector.addActionListener(e -> updateServerTabs());
        panel.add(serverSelector);

        JButton addServerButton = new JButton("Add Server");
        addServerButton.addActionListener(e -> addNewServer());
        panel.add(addServerButton);

        JButton removeServerButton = new JButton("Remove Server");
        removeServerButton.addActionListener(e -> removeCurrentServer());
        panel.add(removeServerButton);

        return panel;
    }

    private JPanel createGlobalChannelsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Global channels configuration
        JLabel helpLabel = new JLabel("<html><b>Global Channels (Fallback):</b><br>" +
                "These channels will be used when no server-specific channels are configured.</html>");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(helpLabel, gbc);

        String[] channelLabels = {"General Channel ID:", "Chat Channel ID:", "Command Channel ID:"};
        String[] channelHelp = {
                "Default channel for game events (joins, deaths, scores)",
                "Default channel for chat messages",
                "Default channel for command usage"
        };

        for (int i = 0; i < channelLabels.length; i++) {
            gbc.gridy = i * 2 + 1;
            gbc.gridx = 0;
            gbc.gridwidth = 1;
            panel.add(new JLabel(channelLabels[i]), gbc);

            gbc.gridx = 1;
            JTextField field = new JTextField(30);
            panel.add(field, gbc);

            // Store reference to global fields
            switch (i) {
                case 0 -> globalGeneralField = field;
                case 1 -> globalChatField = field;
                case 2 -> globalCommandField = field;
            }

            // Help text
            gbc.gridy = i * 2 + 2;
            gbc.gridx = 1;
            gbc.insets = new Insets(0, 5, 10, 5);
            JLabel help = new JLabel("<html><i>" + channelHelp[i] + "</i></html>");
            help.setForeground(Color.GRAY);
            help.setFont(help.getFont().deriveFont(10f));
            panel.add(help, gbc);

            gbc.insets = new Insets(5, 5, 5, 5); // reset
        }

        return panel;
    }

    private ServerChannelPanel createServerChannelsPanel(String serverName) {
        ServerChannelPanel panel = new ServerChannelPanel(serverName);
        serverPanels.put(serverName, panel);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveButton = createStyledButton("Save Configuration", new Color(46, 125, 50));
        JButton resetButton = createStyledButton("Reset to Defaults", new Color(198, 40, 40));

        saveButton.setForeground(Color.BLACK);
        resetButton.setForeground(Color.BLACK);

        saveButton.addActionListener(e -> saveConfig());
        resetButton.addActionListener(e -> resetConfig());

        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);

        return buttonPanel;
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

        // Load global channels
        globalGeneralField.setText(config.getChannels().get("GENERAL"));
        globalChatField.setText(config.getChannels().get("CHAT"));
        globalCommandField.setText(config.getChannels().get("COMMAND"));

        // Find all server-specific channels and create tabs for them
        for (String key : config.getChannels().keySet()) {
            if (key.contains("_")) {
                String[] parts = key.split("_");
                if (parts.length >= 2) {
                    String serverName = parts[0];
                    String channelType = parts[1];

                    if (!serverPanels.containsKey(serverName)) {
                        addServerTab(serverName);
                    }

                    ServerChannelPanel panel = serverPanels.get(serverName);
                    switch (channelType) {
                        case "GENERAL" -> panel.getGeneralField().setText(config.getChannels().get(key));
                        case "CHAT" -> panel.getChatField().setText(config.getChannels().get(key));
                        case "COMMAND" -> panel.getCommandField().setText(config.getChannels().get(key));
                    }
                }
            }
        }

        updateServerSelector();
    }

    private void saveConfig() {
        try {
            AppConfig config = configManager.getConfig();

            // Save global channels
            config.getChannels().put("GENERAL", globalGeneralField.getText().trim());
            config.getChannels().put("CHAT", globalChatField.getText().trim());
            config.getChannels().put("COMMAND", globalCommandField.getText().trim());

            // Save server-specific channels
            for (Map.Entry<String, ServerChannelPanel> entry : serverPanels.entrySet()) {
                String serverName = entry.getKey();
                ServerChannelPanel panel = entry.getValue();

                config.getChannels().put(serverName + "_GENERAL", panel.getGeneralField().getText().trim());
                config.getChannels().put(serverName + "_CHAT", panel.getChatField().getText().trim());
                config.getChannels().put(serverName + "_COMMAND", panel.getCommandField().getText().trim());
            }

            configManager.saveConfig(config);
            JOptionPane.showMessageDialog(this, "Configuration saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addNewServer() {
        String serverName = JOptionPane.showInputDialog(this, "Enter server name:", "Add New Server", JOptionPane.QUESTION_MESSAGE);
        if (serverName != null && !serverName.trim().isEmpty()) {
            serverName = serverName.trim();
            if (!serverPanels.containsKey(serverName)) {
                addServerTab(serverName);
                updateServerSelector();
                serverSelector.setSelectedItem(serverName);
            } else {
                JOptionPane.showMessageDialog(this, "Server already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeCurrentServer() {
        String selectedServer = (String) serverSelector.getSelectedItem();
        if (selectedServer != null && !selectedServer.equals("Global")) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Remove server '" + selectedServer + "' and all its channel configurations?",
                    "Confirm Removal", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                removeServerTab(selectedServer);
                updateServerSelector();
            }
        }
    }

    private void addServerTab(String serverName) {
        ServerChannelPanel panel = createServerChannelsPanel(serverName);
        channelTabs.addTab(serverName, panel);
        serverPanels.put(serverName, panel);
    }

    private void removeServerTab(String serverName) {
        ServerChannelPanel panel = serverPanels.remove(serverName);
        if (panel != null) {
            channelTabs.remove(panel);
        }
    }

    private void updateServerSelector() {
        serverSelector.removeAllItems();
        serverSelector.addItem("Global");
        for (String serverName : serverPanels.keySet()) {
            serverSelector.addItem(serverName);
        }
    }

    private void updateServerTabs() {
        String selected = (String) serverSelector.getSelectedItem();
        if (selected != null) {
            if (selected.equals("Global")) {
                channelTabs.setSelectedIndex(0);
            } else {
                // Find the tab index for this server
                for (int i = 1; i < channelTabs.getTabCount(); i++) {
                    if (channelTabs.getTitleAt(i).equals(selected)) {
                        channelTabs.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    private void resetConfig() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset output configuration to defaults? This will reset all channel IDs.",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            try {
                AppConfig config = configManager.getConfig();
                AppConfig defaultConfig = new AppConfig();

                // Reset channels to defaults
                config.getChannels().clear();
                config.getChannels().putAll(defaultConfig.getChannels());

                // Reset event configurations to defaults
                config.getEventConfigs().clear();
                config.getEventConfigs().putAll(defaultConfig.getEventConfigs());

                configManager.saveConfig(config);
                loadConfig();

                JOptionPane.showMessageDialog(this,
                        "Output configuration reset to defaults!",
                        "Reset Complete",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error resetting configuration: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Custom panel for server channels
    private static class ServerChannelPanel extends JPanel {
        private final String serverName;
        private JTextField generalField;
        private JTextField chatField;
        private JTextField commandField;

        public ServerChannelPanel(String serverName) {
            this.serverName = serverName;
            initializeUI();
        }

        private void initializeUI() {
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(10, 10, 10, 10));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Server header
            JLabel headerLabel = new JLabel("<html><b>Server: " + serverName + "</b><br>" +
                    "Configure Discord channels specifically for this Halo server.</html>");
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            add(headerLabel, gbc);

            // Channel fields
            String[] channelLabels = {"General Channel ID:", "Chat Channel ID:", "Command Channel ID:"};
            String[] channelHelp = {
                    "Channel for game events (joins, deaths, scores) - overrides global setting",
                    "Channel for chat messages - overrides global setting",
                    "Channel for command usage - overrides global setting"
            };

            for (int i = 0; i < channelLabels.length; i++) {
                gbc.gridy = i * 2 + 1;
                gbc.gridx = 0;
                gbc.gridwidth = 1;
                add(new JLabel(channelLabels[i]), gbc);

                gbc.gridx = 1;
                JTextField field = new JTextField(30);
                add(field, gbc);

                // Store references
                switch (i) {
                    case 0 -> generalField = field;
                    case 1 -> chatField = field;
                    case 2 -> commandField = field;
                }

                // Help text
                gbc.gridy = i * 2 + 2;
                gbc.gridx = 1;
                gbc.insets = new Insets(0, 5, 10, 5);
                JLabel help = new JLabel("<html><i>" + channelHelp[i] + "</i></html>");
                help.setForeground(Color.GRAY);
                help.setFont(help.getFont().deriveFont(10f));
                add(help, gbc);

                gbc.insets = new Insets(5, 5, 5, 5); // reset
            }
        }

        // Getters
        public JTextField getGeneralField() {
            return generalField;
        }

        public JTextField getChatField() {
            return chatField;
        }

        public JTextField getCommandField() {
            return commandField;
        }
    }
}