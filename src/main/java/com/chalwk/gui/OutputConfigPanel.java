/**
 * SAPPDiscordBot
 * Copyright (c) 2025-2026. Jericho Crosby (Chalwk)
 * MIT License
 */

package com.chalwk.gui;

import com.chalwk.config.AppConfig;
import com.chalwk.config.ConfigManager;
import com.chalwk.config.EventConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class OutputConfigPanel extends JPanel {

    private final ConfigManager configManager;
    private final Map<String, ServerConfigPanel> serverPanels;
    private JTabbedPane configTabs;
    private JComboBox<String> serverSelector;

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

        JPanel serverManagementPanel = createServerManagementPanel();

        configTabs = new JTabbedPane();
        configTabs.addTab("Global Configuration", createGlobalConfigPanel());

        JPanel buttonPanel = createButtonPanel();

        add(serverManagementPanel, BorderLayout.NORTH);
        add(configTabs, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
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

    private JPanel createGlobalConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel channelsPanel = createGlobalChannelsPanel();

        JPanel templatesPanel = new EventTemplatesSection("Global", configManager);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, channelsPanel, templatesPanel);
        splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(200);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createGlobalChannelsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Global Channels (Fallback)"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel helpLabel = new JLabel("<html>These channels will be used when no server-specific channels are configured.</html>");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panel.add(helpLabel, gbc);

        String[] channelLabels = {"General Channel ID:", "Chat Channel ID:", "Command Channel ID:"};
        String[] channelHelp = {
                "Default channel for game events (joins, deaths, scores)",
                "Default channel for chat messages",
                "Default channel for command usage"
        };

        JTextField[] fields = new JTextField[3];

        for (int i = 0; i < channelLabels.length; i++) {
            gbc.gridy = i * 2 + 1;
            gbc.gridx = 0;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            panel.add(new JLabel(channelLabels[i]), gbc);

            gbc.gridx = 1;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            fields[i] = new JTextField();
            fields[i].setPreferredSize(new Dimension(200, 25));
            panel.add(fields[i], gbc);

            gbc.gridy = i * 2 + 2;
            gbc.gridx = 1;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(0, 5, 10, 5);
            JLabel help = new JLabel("<html><i>" + channelHelp[i] + "</i></html>");
            help.setForeground(Color.GRAY);
            help.setFont(help.getFont().deriveFont(10f));
            panel.add(help, gbc);

            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.weightx = 0.0;
        }

        globalGeneralField = fields[0];
        globalChatField = fields[1];
        globalCommandField = fields[2];

        gbc.gridy = channelLabels.length * 2 + 1;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private ServerConfigPanel createServerConfigPanel(String serverName) {
        ServerConfigPanel panel = new ServerConfigPanel(serverName, configManager);
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

        globalGeneralField.setText(config.getChannels().get("GENERAL"));
        globalChatField.setText(config.getChannels().get("CHAT"));
        globalCommandField.setText(config.getChannels().get("COMMAND"));

        for (String key : config.getChannels().keySet()) {
            if (key.contains("_")) {
                String[] parts = key.split("_");
                if (parts.length >= 2) {
                    String serverName = parts[0];
                    String channelType = parts[1];

                    if (!serverPanels.containsKey(serverName)) {
                        addServerTab(serverName);
                    }

                    ServerConfigPanel panel = serverPanels.get(serverName);
                    switch (channelType) {
                        case "GENERAL" -> panel.getGeneralField().setText(config.getChannels().get(key));
                        case "CHAT" -> panel.getChatField().setText(config.getChannels().get(key));
                        case "COMMAND" -> panel.getCommandField().setText(config.getChannels().get(key));
                    }
                }
            }
        }

        updateServerSelector();

        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    private void saveConfig() {
        try {
            AppConfig config = configManager.getConfig();

            config.getChannels().put("GENERAL", globalGeneralField.getText().trim());
            config.getChannels().put("CHAT", globalChatField.getText().trim());
            config.getChannels().put("COMMAND", globalCommandField.getText().trim());

            for (Map.Entry<String, ServerConfigPanel> entry : serverPanels.entrySet()) {
                String serverName = entry.getKey();
                ServerConfigPanel panel = entry.getValue();

                config.getChannels().put(serverName + "_GENERAL", panel.getGeneralField().getText().trim());
                config.getChannels().put(serverName + "_CHAT", panel.getChatField().getText().trim());
                config.getChannels().put(serverName + "_COMMAND", panel.getCommandField().getText().trim());

                panel.saveEventTemplates();
            }

            configManager.saveConfig(config);
            JOptionPane.showMessageDialog(this, "Configuration saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving configuration: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

                SwingUtilities.invokeLater(() -> {
                    revalidate();
                    repaint();
                });
            } else {
                JOptionPane.showMessageDialog(this, "Server already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeCurrentServer() {
        String selectedServer = (String) serverSelector.getSelectedItem();
        if (selectedServer != null && !selectedServer.equals("Global")) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Remove server '" + selectedServer + "' and all its configurations?",
                    "Confirm Removal", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                removeServerTab(selectedServer);
                updateServerSelector();

                SwingUtilities.invokeLater(() -> {
                    revalidate();
                    repaint();
                });
            }
        }
    }

    private void addServerTab(String serverName) {
        ServerConfigPanel panel = createServerConfigPanel(serverName);
        configTabs.addTab(serverName, panel);
        serverPanels.put(serverName, panel);

        configManager.getConfig().ensureServerChannels(serverName);
    }

    private void removeServerTab(String serverName) {
        ServerConfigPanel panel = serverPanels.remove(serverName);
        if (panel != null) {
            configTabs.remove(panel);

            AppConfig config = configManager.getConfig();
            config.getChannels().remove(serverName + "_GENERAL");
            config.getChannels().remove(serverName + "_CHAT");
            config.getChannels().remove(serverName + "_COMMAND");

            config.getServerEventConfigs().remove(serverName);
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
                configTabs.setSelectedIndex(0);
            } else {
                for (int i = 1; i < configTabs.getTabCount(); i++) {
                    if (configTabs.getTitleAt(i).equals(selected)) {
                        configTabs.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    private void resetConfig() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset all configuration to defaults? This will reset all channel IDs and event templates.",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            try {
                AppConfig config = configManager.getConfig();
                AppConfig defaultConfig = new AppConfig();

                config.getChannels().clear();
                config.getChannels().putAll(defaultConfig.getChannels());
                config.getEventConfigs().clear();
                config.getEventConfigs().putAll(defaultConfig.getEventConfigs());
                config.getServerEventConfigs().clear();

                configManager.saveConfig(config);
                loadConfig();

                JOptionPane.showMessageDialog(this,
                        "Configuration reset to defaults!",
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

    private static class ServerConfigPanel extends JPanel {
        private final String serverName;
        private final ConfigManager configManager;
        private JTextField generalField;
        private JTextField chatField;
        private JTextField commandField;
        private EventTemplatesSection templatesSection;

        public ServerConfigPanel(String serverName, ConfigManager configManager) {
            this.serverName = serverName;
            this.configManager = configManager;
            initializeUI();
        }

        private void initializeUI() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 10, 10, 10));

            JLabel headerLabel = new JLabel("<html><b>Server: " + serverName + "</b><br>" +
                    "Configure Discord channels and event templates specifically for this Halo server.</html>");
            headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            add(headerLabel, BorderLayout.NORTH);

            JPanel channelsPanel = createChannelsPanel();

            templatesSection = new EventTemplatesSection(serverName, configManager);

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, channelsPanel, templatesSection);
            splitPane.setResizeWeight(0.3);
            splitPane.setDividerLocation(150);

            add(splitPane, BorderLayout.CENTER);

            SwingUtilities.invokeLater(() -> {
                revalidate();
                repaint();
            });
        }

        private JPanel createChannelsPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder("Channel Configuration"));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            String[] channelLabels = {"General Channel ID:", "Chat Channel ID:", "Command Channel ID:"};
            String[] channelHelp = {
                    "Channel for game events (joins, deaths, scores) - overrides global setting",
                    "Channel for chat messages - overrides global setting",
                    "Channel for command usage - overrides global setting"
            };

            JTextField[] fields = new JTextField[3];

            for (int i = 0; i < channelLabels.length; i++) {
                gbc.gridy = i * 2;
                gbc.gridx = 0;
                gbc.gridwidth = 1;
                gbc.weightx = 0.0;
                panel.add(new JLabel(channelLabels[i]), gbc);

                gbc.gridx = 1;
                gbc.gridwidth = 2;
                gbc.weightx = 1.0;
                fields[i] = new JTextField();
                fields[i].setPreferredSize(new Dimension(200, 25));
                panel.add(fields[i], gbc);

                gbc.gridy = i * 2 + 1;
                gbc.gridx = 1;
                gbc.gridwidth = 2;
                gbc.insets = new Insets(0, 5, 5, 5);
                JLabel help = new JLabel("<html><i>" + channelHelp[i] + "</i></html>");
                help.setForeground(Color.GRAY);
                help.setFont(help.getFont().deriveFont(10f));
                panel.add(help, gbc);

                gbc.insets = new Insets(5, 5, 5, 5);
                gbc.weightx = 0.0;
            }

            generalField = fields[0];
            chatField = fields[1];
            commandField = fields[2];

            gbc.gridy = channelLabels.length * 2;
            gbc.gridx = 0;
            gbc.gridwidth = 3;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            panel.add(Box.createVerticalGlue(), gbc);

            return panel;
        }

        public void saveEventTemplates() {
            if (!"Global".equals(serverName)) {
                Map<String, EventConfig> serverConfigs = new HashMap<>();

                for (Map.Entry<String, EventConfig> entry : templatesSection.getEventConfigs().entrySet()) {
                    EventConfig original = entry.getValue();
                    serverConfigs.put(entry.getKey(), new EventConfig(
                            original.isEnabled(),
                            original.getTemplate(),
                            original.getColor(),
                            original.isUseEmbed(),
                            original.getChannelId()
                    ));
                }

                configManager.getConfig().setEventConfigsForServer(serverName, serverConfigs);
            } else {
                templatesSection.saveToConfig(configManager.getConfig().getEventConfigs());
            }
        }

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