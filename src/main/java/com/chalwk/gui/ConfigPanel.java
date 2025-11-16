package com.chalwk.gui;

import com.chalwk.config.AppConfig;
import com.chalwk.config.ConfigManager;

import javax.swing.*;
import java.awt.*;

public class ConfigPanel extends JPanel {

    private final ConfigManager configManager;
    private JTextField discordTokenField;
    private JTextField watchDirectoryField;
    private JCheckBox autoStartCheckbox;
    private JSpinner pollIntervalSpinner;

    public ConfigPanel(ConfigManager configManager) {
        this.configManager = configManager;
        initializeUI();
        loadConfig();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Discord Token
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Discord Bot Token:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        discordTokenField = new JTextField(40);
        formPanel.add(discordTokenField, gbc);

        // Watch Directory
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        formPanel.add(new JLabel("Watch Directory:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1;
        watchDirectoryField = new JTextField(30);
        formPanel.add(watchDirectoryField, gbc);
        gbc.gridx = 2;
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> browseDirectory());
        formPanel.add(browseButton, gbc);

        // Poll Interval
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        formPanel.add(new JLabel("Poll Interval (ms):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        pollIntervalSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 10000, 100));
        formPanel.add(pollIntervalSpinner, gbc);

        // Auto Start
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
        autoStartCheckbox = new JCheckBox("Start bot automatically on application launch");
        formPanel.add(autoStartCheckbox, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save Configuration");
        JButton resetButton = new JButton("Reset to Defaults");

        saveButton.addActionListener(e -> saveConfig());
        resetButton.addActionListener(e -> resetConfig());

        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);
        formPanel.add(buttonPanel, gbc);

        add(formPanel, BorderLayout.NORTH);
    }

    private void browseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
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
            config.setDiscordToken(discordTokenField.getText().trim());
            config.setWatchDirectory(watchDirectoryField.getText().trim());
            config.setPollInterval((Integer) pollIntervalSpinner.getValue());
            config.setAutoStart(autoStartCheckbox.isSelected());

            configManager.saveConfig(config);
            JOptionPane.showMessageDialog(this, "Configuration saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving configuration: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetConfig() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset all settings to defaults?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            configManager.resetToDefaults();
            loadConfig();
        }
    }
}