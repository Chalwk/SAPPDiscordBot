package com.chalwk.gui;

import com.chalwk.SAPPDiscordBot;
import com.chalwk.config.ConfigManager;
import com.chalwk.model.RawEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {

    private final ConfigManager configManager;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private ConfigPanel configPanel;
    private EventLogPanel eventLogPanel;
    private JTabbedPane tabbedPane;

    public MainFrame(ConfigManager configManager) {
        this.configManager = configManager;
        initializeUI();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("SAPP Discord Bot");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                minimizeToTray();
            }
        });

        createMenuBar();
        createMainPanel();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> confirmExit());
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> refreshView());
        viewMenu.add(refreshItem);

        JMenu helpMenu = new JMenu("Help");

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());

        helpMenu.addSeparator();
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void createMainPanel() {
        tabbedPane = new JTabbedPane();

        // Configuration tab
        configPanel = new ConfigPanel(configManager);
        tabbedPane.addTab("Bot Configuration", configPanel);

        // Output Configuration tab
        OutputConfigPanel outputConfigPanel = new OutputConfigPanel(configManager);
        tabbedPane.addTab("Output Configuration", outputConfigPanel);

        // Event Log tab
        eventLogPanel = new EventLogPanel();
        tabbedPane.addTab("Event Log", eventLogPanel);

        // Status and controls panel
        JPanel controlPanel = createControlPanel();

        // Main layout
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusPanel.add(statusLabel);

        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        startButton = new JButton("Start Bot");
        stopButton = new JButton("Stop Bot");
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> SAPPDiscordBot.startBot());
        stopButton.addActionListener(e -> SAPPDiscordBot.stopBot());

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        controlPanel.add(statusPanel, BorderLayout.WEST);
        controlPanel.add(buttonPanel, BorderLayout.EAST);

        return controlPanel;
    }

    public void updateStatus(boolean running) {
        SwingUtilities.invokeLater(() -> {
            if (running) {
                statusLabel.setText("Status: Running");
                statusLabel.setForeground(Color.GREEN.darker());
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                tabbedPane.setSelectedIndex(1);
            } else {
                statusLabel.setText("Status: Stopped");
                statusLabel.setForeground(Color.RED);
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });
    }

    public void addEventLog(RawEvent event, String serverName, String status) {
        if (eventLogPanel != null) {
            eventLogPanel.addEvent(event, serverName, status);
        }
    }

    private void minimizeToTray() {
        setVisible(false);
        JOptionPane.showMessageDialog(this,
                "Application minimized to system tray. Right-click the tray icon to restore or exit.",
                "Minimized to Tray",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void confirmExit() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            if (SAPPDiscordBot.isBotRunning()) {
                SAPPDiscordBot.stopBot();
            }
            System.exit(0);
        }
    }

    private void refreshView() {
        if (configPanel != null) {
            configPanel.revalidate();
            configPanel.repaint();
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "SAPP Discord Bot v1.0.0\n\n" +
                        "Advanced Halo server event monitoring and Discord integration.\n" +
                        "Supports all event types including embeds with fields and colors.\n\n" +
                        "Copyright © 2025 Jericho Crosby (Chalwk)",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void restoreFromTray() {
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
    }
}