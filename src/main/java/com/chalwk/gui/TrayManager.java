package com.chalwk.gui;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TrayManager {

    private final MainFrame mainFrame;
    private final boolean traySupported;
    private TrayIcon trayIcon;

    public TrayManager(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.traySupported = initializeTray();
    }

    private boolean initializeTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray not supported on this system");
            return false;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = createTrayImage();

            PopupMenu popup = getPopupMenu();

            trayIcon = new TrayIcon(image, "SAPP Discord Bot", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> mainFrame.restoreFromTray());

            tray.add(trayIcon);
            return true;

        } catch (AWTException e) {
            System.err.println("Could not add tray icon: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Tray initialization failed: " + e.getMessage());
            return false;
        }
    }

    @NotNull
    private PopupMenu getPopupMenu() {
        PopupMenu popup = new PopupMenu();

        MenuItem restoreItem = new MenuItem("Restore");
        restoreItem.addActionListener(e -> mainFrame.restoreFromTray());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.confirmExit();
            } else {
                System.exit(0);
            }
        });

        popup.add(restoreItem);
        popup.addSeparator();
        popup.add(exitItem);
        return popup;
    }

    private Image createTrayImage() {
        // Try multiple approaches to load the icon for cross-platform compatibility (windows/linux)
        try {
            // Approach 1: Load from classpath
            Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
            if (image != null) return image;

            // Approach 2: Load from file system
            image = Toolkit.getDefaultToolkit().getImage("icon.png");
            if (image != null) return image;

            // Approach 3: Create a simple generated icon as fallback
            return createFallbackIcon();

        } catch (Exception e) {
            return createFallbackIcon();
        }
    }

    private Image createFallbackIcon() {
        int size = 16;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillOval(0, 0, size, size);
        g2d.setColor(Color.WHITE);
        g2d.drawString("S", 4, 12);
        g2d.dispose();
        return image;
    }

    public void showTrayMessage(String message) {
        if (trayIcon != null && traySupported) {
            try {
                trayIcon.displayMessage("SAPP Discord Bot", message, TrayIcon.MessageType.INFO);
            } catch (Exception e) {
                System.err.println("Could not show tray message: " + e.getMessage());
            }
        }
    }
}