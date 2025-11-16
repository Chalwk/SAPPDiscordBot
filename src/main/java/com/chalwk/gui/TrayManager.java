package com.chalwk.gui;

import javax.swing.*;
import java.awt.*;

public class TrayManager {

    private final MainFrame mainFrame;
    private TrayIcon trayIcon;

    public TrayManager(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initializeTray();
    }

    private void initializeTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray not supported");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();

        // Create tray icon
        Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
        if (image == null) {
            // Fallback to default icon
            image = new ImageIcon().getImage();
        }

        // Create popup menu
        PopupMenu popup = new PopupMenu();

        MenuItem restoreItem = new MenuItem("Restore");
        restoreItem.addActionListener(e -> mainFrame.restoreFromTray());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        popup.add(restoreItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, "SAPP Discord Bot", popup);
        trayIcon.setImageAutoSize(true);

        // Add double-click listener to restore
        trayIcon.addActionListener(e -> mainFrame.restoreFromTray());

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("Could not add tray icon: " + e.getMessage());
        }
    }

    public void showTrayMessage(String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage("SAPP Discord Bot", message, TrayIcon.MessageType.INFO);
        }
    }
}