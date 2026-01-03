/**
 * SAPPDiscordBot
 * Copyright (c) 2025-2026. Jericho Crosby (Chalwk)
 * MIT License
 */

package com.chalwk.gui;

import com.chalwk.config.AppConfig;
import com.chalwk.config.ConfigManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;

public class ConfigPanel extends JPanel {

    private final ConfigManager configManager;
    private JPasswordField discordTokenField;
    private JTextField watchDirectoryField;
    private JCheckBox autoStartCheckbox;
    private JSpinner pollIntervalSpinner;
    private JCheckBox showTokenCheckbox;

    public ConfigPanel(ConfigManager configManager) {
        this.configManager = configManager;
        initializeUI();
        loadConfig();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder("Bot Configuration"),
                new EmptyBorder(5, 5, 5, 5)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        JLabel tokenLabel = new JLabel("Discord Bot Token:");
        tokenLabel.setFont(tokenLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(tokenLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        discordTokenField = new JPasswordField(40);
        mainPanel.add(discordTokenField, gbc);

        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        showTokenCheckbox = new JCheckBox("Show token");
        showTokenCheckbox.addActionListener(e -> toggleTokenVisibility());
        mainPanel.add(showTokenCheckbox, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(2, 8, 8, 8);
        JLabel tokenHelp = new JLabel("<html><i>Get this from Discord Developer Portal → Your Bot → Token</i></html>");
        tokenHelp.setForeground(Color.GRAY);
        tokenHelp.setFont(tokenHelp.getFont().deriveFont(10f));
        mainPanel.add(tokenHelp, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 8, 8, 8);
        JLabel watchDirLabel = new JLabel("Watch Directory:");
        watchDirLabel.setFont(watchDirLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(watchDirLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        watchDirectoryField = new JTextField(30);
        mainPanel.add(watchDirectoryField, gbc);

        gbc.gridx = 2;
        JButton browseButton = new JButton("Browse");
        browseButton.setBackground(new Color(70, 130, 180));
        browseButton.setForeground(Color.BLACK);
        browseButton.addActionListener(e -> browseDirectory());
        mainPanel.add(browseButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(2, 8, 8, 8);
        JLabel watchDirHelp = new JLabel("<html><i>Directory where Halo server JSON event files are written. Each server should have its own JSON file.</i></html>");
        watchDirHelp.setForeground(Color.GRAY);
        watchDirHelp.setFont(watchDirHelp.getFont().deriveFont(10f));
        mainPanel.add(watchDirHelp, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 8, 8, 8);
        JLabel pollLabel = new JLabel("Poll Interval (ms):");
        pollLabel.setFont(pollLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(pollLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        pollIntervalSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 10000, 100));
        mainPanel.add(pollIntervalSpinner, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(2, 8, 8, 8);
        JLabel pollHelp = new JLabel("<html><i>How often to check for new events (lower = faster detection, higher = less CPU usage)</i></html>");
        pollHelp.setForeground(Color.GRAY);
        pollHelp.setFont(pollHelp.getFont().deriveFont(10f));
        mainPanel.add(pollHelp, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(8, 8, 8, 8);
        autoStartCheckbox = new JCheckBox("Start bot automatically on application launch");
        autoStartCheckbox.setFont(autoStartCheckbox.getFont().deriveFont(Font.BOLD));
        mainPanel.add(autoStartCheckbox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));

        JButton saveButton = createStyledButton("Save Configuration", new Color(46, 125, 50));
        JButton resetButton = createStyledButton("Reset to Defaults", new Color(198, 40, 40));

        saveButton.setForeground(Color.BLACK);
        resetButton.setForeground(Color.BLACK);

        saveButton.addActionListener(e -> saveConfig());
        resetButton.addActionListener(e -> resetConfig());

        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel, BorderLayout.NORTH);
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker()),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        return button;
    }

    private void toggleTokenVisibility() {
        if (showTokenCheckbox.isSelected()) {
            discordTokenField.setEchoChar((char) 0);
        } else {
            discordTokenField.setEchoChar('•');
        }
    }

    private void browseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Watch Directory");

        String currentDir = watchDirectoryField.getText();
        if (currentDir != null && !currentDir.trim().isEmpty()) {
            chooser.setCurrentDirectory(new File(currentDir));
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            watchDirectoryField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadConfig() {
        AppConfig config = configManager.getConfig();
        discordTokenField.setText(config.getDiscordToken());
        watchDirectoryField.setText(config.getWatchDirectory());
        pollIntervalSpinner.setValue(config.getPollInterval());
        autoStartCheckbox.setSelected(config.isAutoStart());
    }

    private void saveConfig() {
        try {
            AppConfig config = new AppConfig();
            config.setDiscordToken(new String(discordTokenField.getPassword()).trim());
            config.setWatchDirectory(watchDirectoryField.getText().trim());
            config.setPollInterval((Integer) pollIntervalSpinner.getValue());
            config.setAutoStart(autoStartCheckbox.isSelected());

            configManager.saveConfig(config);
            JOptionPane.showMessageDialog(this,
                    "Configuration saved successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error saving configuration: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetConfig() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset bot configuration settings to defaults?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            try {
                AppConfig config = configManager.getConfig();

                config.setDiscordToken("");
                config.setWatchDirectory("./discord_events");
                config.setPollInterval(1000);
                config.setAutoStart(false);

                configManager.saveConfig(config);
                loadConfig();

                JOptionPane.showMessageDialog(this,
                        "Bot configuration reset to defaults!",
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
}