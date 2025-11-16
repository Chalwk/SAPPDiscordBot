package com.chalwk.file;

import com.chalwk.config.ConfigManager;
import com.chalwk.discord.DiscordBot;
import com.chalwk.discord.EventProcessor;
import com.chalwk.model.DiscordEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileWatcher {

    private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);

    private final ConfigManager configManager;
    private final DiscordBot discordBot;
    private final EventProcessor eventProcessor;
    private final JSONParser jsonParser;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Long> lastModifiedMap;
    private boolean isWatching;

    // Updated constructor to accept EventProcessor
    public FileWatcher(ConfigManager configManager, DiscordBot discordBot, EventProcessor eventProcessor) {
        this.configManager = configManager;
        this.discordBot = discordBot;
        this.eventProcessor = eventProcessor;
        this.jsonParser = new JSONParser();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.lastModifiedMap = new HashMap<>();
    }

    // Keep the old constructor for backward compatibility
    public FileWatcher(ConfigManager configManager, DiscordBot discordBot) {
        this(configManager, discordBot, new EventProcessor());
    }

    public void startWatching() {
        if (isWatching) {
            logger.warn("File watcher is already running");
            return;
        }

        String watchDir = configManager.getConfig().getWatchDirectory();
        File directory = new File(watchDir);

        if (!directory.exists() || !directory.isDirectory()) {
            logger.error("Watch directory does not exist or is not a directory: {}", watchDir);
            return;
        }

        int pollInterval = configManager.getConfig().getPollInterval();

        scheduler.scheduleAtFixedRate(this::checkForChanges, 0, pollInterval, TimeUnit.MILLISECONDS);
        isWatching = true;

        logger.info("Started watching directory: {}", watchDir);
        logger.info("Poll interval: {} ms", pollInterval);
    }

    public void stopWatching() {
        if (!isWatching) {
            return;
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        isWatching = false;
        lastModifiedMap.clear();
        logger.info("File watcher stopped");
    }

    private void checkForChanges() {
        try {
            File directory = new File(configManager.getConfig().getWatchDirectory());
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));

            if (files == null) return;

            for (File file : files) {
                long lastModified = file.lastModified();
                String filePath = file.getAbsolutePath();
                Long previousModified = lastModifiedMap.get(filePath);

                if (previousModified == null || lastModified > previousModified) {
                    // File is new or modified
                    processFile(file);
                    lastModifiedMap.put(filePath, lastModified);
                }
            }
        } catch (Exception e) {
            logger.error("Error checking for file changes", e);
        }
    }

    private void processFile(File file) {
        try {
            List<DiscordEvent> events = jsonParser.parseEvents(file);
            for (DiscordEvent event : events) {
                eventProcessor.processEvent(event, discordBot);
            }
            logger.debug("Processed {} events from file: {}", events.size(), file.getName());
        } catch (Exception e) {
            logger.error("Error processing file: {}", file.getName(), e);
        }
    }

    public boolean isWatching() {
        return isWatching;
    }
}