package com.chalwk;

import com.chalwk.config.ConfigManager;
import com.chalwk.discord.DiscordBot;
import com.chalwk.discord.EventProcessor;
import com.chalwk.file.FileWatcher;
import com.chalwk.gui.MainFrame;
import com.chalwk.gui.TrayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class SAPPDiscordBot {

    private static final Logger logger = LoggerFactory.getLogger(SAPPDiscordBot.class);
    private static ConfigManager configManager;
    private static DiscordBot discordBot;
    private static FileWatcher fileWatcher;
    private static MainFrame mainFrame;
    private static TrayManager trayManager;
    private static EventProcessor eventProcessor;

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Could not set system look and feel", e);
        }

        // Initialize configuration
        configManager = new ConfigManager();

        // Start GUI in Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            mainFrame = new MainFrame(configManager);
            trayManager = new TrayManager(mainFrame);

            if (configManager.getConfig().isAutoStart()) {
                startBot();
            }
        });
    }

    public static void startBot() {
        try {
            if (discordBot != null && discordBot.isRunning()) {
                JOptionPane.showMessageDialog(mainFrame, "Bot is already running!", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Initialize Discord bot
            discordBot = new DiscordBot(configManager);
            if (!discordBot.start()) {
                JOptionPane.showMessageDialog(mainFrame, "Failed to start Discord bot. Check your token and configuration.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Initialize event processor with UI listener
            eventProcessor = new EventProcessor();
            eventProcessor.setEventListener(event -> {
                mainFrame.addEventLog(event, "Processed");
            });

            // Initialize file watcher
            fileWatcher = new FileWatcher(configManager, discordBot, eventProcessor);
            fileWatcher.startWatching();

            mainFrame.updateStatus(true);
            logger.info("SAPP Discord Bot started successfully");

            // Show success message
            trayManager.showTrayMessage("SAPP Discord Bot started successfully");

        } catch (Exception e) {
            logger.error("Failed to start bot", e);
            JOptionPane.showMessageDialog(mainFrame, "Failed to start bot: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void stopBot() {
        try {
            if (fileWatcher != null) {
                fileWatcher.stopWatching();
                fileWatcher = null;
            }

            if (discordBot != null) {
                discordBot.stop();
                discordBot = null;
            }

            if (eventProcessor != null) {
                eventProcessor.setEventListener(null);
                eventProcessor = null;
            }

            mainFrame.updateStatus(false);
            logger.info("SAPP Discord Bot stopped successfully");

            // Show stopped message
            trayManager.showTrayMessage("SAPP Discord Bot stopped");

        } catch (Exception e) {
            logger.error("Error stopping bot", e);
        }
    }

    public static boolean isBotRunning() {
        return discordBot != null && discordBot.isRunning();
    }
}