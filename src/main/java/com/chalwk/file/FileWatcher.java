package com.chalwk.file;

import com.chalwk.config.ConfigManager;
import com.chalwk.discord.DiscordBot;
import com.chalwk.discord.EventProcessor;
import com.chalwk.model.DiscordEvent;
import com.chalwk.model.EmbedField;
import com.chalwk.model.EventEmbed;
import com.chalwk.model.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.MessageDigest;
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
    private final Map<String, FileState> fileStates;
    private boolean isWatching;

    public FileWatcher(ConfigManager configManager, DiscordBot discordBot, EventProcessor eventProcessor) {
        this.configManager = configManager;
        this.discordBot = discordBot;
        this.eventProcessor = eventProcessor;
        this.jsonParser = new JSONParser();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.fileStates = new HashMap<>();
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

        // Clear all existing JSON files when starting
        clearExistingFiles();

        int pollInterval = configManager.getConfig().getPollInterval();

        scheduler.scheduleAtFixedRate(this::checkForChanges, 0, pollInterval, TimeUnit.MILLISECONDS);
        isWatching = true;

        logger.info("Started watching directory: {}", watchDir);
        logger.info("Poll interval: {} ms", pollInterval);
    }

    private void clearExistingFiles() {
        try {
            File directory = new File(configManager.getConfig().getWatchDirectory());
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));

            if (files != null) {
                for (File file : files) {
                    clearFile(file);
                    logger.info("Cleared existing file: {}", file.getName());
                }
                logger.info("Cleared {} existing JSON files", files.length);
            }
        } catch (Exception e) {
            logger.error("Error clearing existing files", e);
        }
    }

    private void clearFile(File file) {
        try {
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write("[]");
            writer.close();
        } catch (Exception e) {
            logger.error("Failed to clear file: {}", file.getName(), e);
        }
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
        fileStates.clear();
        logger.info("File watcher stopped");
    }

    private void checkForChanges() {
        try {
            File directory = new File(configManager.getConfig().getWatchDirectory());
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));

            if (files == null) return;

            for (File file : files) {
                processFileIfChanged(file);
            }
        } catch (Exception e) {
            logger.error("Error checking for file changes", e);
        }
    }

    private void processFileIfChanged(File file) {
        try {
            String filePath = file.getAbsolutePath();
            long lastModified = file.lastModified();
            long fileSize = file.length();

            FileState state = fileStates.get(filePath);

            // If file is new or modified, process it
            if (state == null || lastModified > state.lastModified || fileSize != state.fileSize) {
                if (state == null) {
                    state = new FileState();
                    fileStates.put(filePath, state);
                    logger.debug("New file detected: {}", file.getName());
                } else {
                    logger.debug("File modified: {} (size: {}->{}, time: {}->{})",
                            file.getName(), state.fileSize, fileSize, state.lastModified, lastModified);
                }

                state.lastModified = lastModified;
                state.fileSize = fileSize;

                processNewEvents(file, state);
            }
        } catch (Exception e) {
            logger.error("Error processing file changes: {}", file.getName(), e);
        }
    }

    private void processNewEvents(File file, FileState state) {
        try {
            List<DiscordEvent> allEvents = jsonParser.parseEvents(file);
            List<DiscordEvent> newEvents = new ArrayList<>();

            // If file was cleared, or we have no state, process all events
            if (allEvents.isEmpty() || state.processedEventHashes.isEmpty()) {
                state.processedEventHashes.clear(); // Reset state
            }

            // Filter out already processed events
            for (DiscordEvent event : allEvents) {
                String eventHash = generateEventHash(event);
                if (!state.processedEventHashes.contains(eventHash)) {
                    newEvents.add(event);
                    state.processedEventHashes.add(eventHash);
                    logger.debug("New event found: {}", eventHash.substring(0, 8));
                } else {
                    logger.debug("Skipping already processed event: {}", eventHash.substring(0, 8));
                }
            }

            // Process new events
            for (DiscordEvent event : newEvents) {
                eventProcessor.processEvent(event, discordBot);
            }

            // Log processing summary
            if (!newEvents.isEmpty()) {
                logger.info("Processed {} new events from file: {}", newEvents.size(), file.getName());
            }

            state.lastProcessedCount = newEvents.size();

            // Clean up old hashes periodically
            cleanupOldHashes(state, allEvents.size());

        } catch (Exception e) {
            logger.error("Error processing new events from file: {}", file.getName(), e);
            // Reset state on error to avoid persistent issues
            state.processedEventHashes.clear();
        }
    }

    private String generateEventHash(DiscordEvent event) {
        try {
            StringBuilder content = new StringBuilder();

            if (event.getMessage() != null) {
                EventMessage msg = event.getMessage();
                content.append("MSG:")
                        .append(msg.getChannel_id()).append(":")
                        .append(msg.getText()).append(":");
            } else if (event.getEmbed() != null) {
                EventEmbed embed = event.getEmbed();
                content.append("EMBED:")
                        .append(embed.getChannel_id()).append(":")
                        .append(embed.getTitle()).append(":")
                        .append(embed.getDescription()).append(":")
                        .append(embed.getFooter()).append(":");

                if (embed.getFields() != null) {
                    for (EmbedField field : embed.getFields()) {
                        content.append(field.getName()).append(":")
                                .append(field.getValue()).append(":");
                    }
                }
            }

            // Add timestamp to prevent identical events from having same hash
            content.append(System.currentTimeMillis());

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.toString().getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            // Fallback to UUID
            return UUID.randomUUID().toString();
        }
    }

    private void cleanupOldHashes(FileState state, int currentEventCount) {
        // If we have too many stored hashes, clear them and start fresh
        // This prevents memory leaks if files grow very large
        if (state.processedEventHashes.size() > 1000) { // Adjust threshold as needed
            logger.debug("Cleaning up old event hashes, stored: {}", state.processedEventHashes.size());
            state.processedEventHashes.clear();
        }
    }

    public boolean isWatching() {
        return isWatching;
    }

    // Track state for each file
    private static class FileState {
        long lastModified;
        long fileSize;
        Set<String> processedEventHashes; // Track processed events by content hash
        int lastProcessedCount; // Number of events processed in last run

        FileState() {
            this.processedEventHashes = new HashSet<>();
            this.lastProcessedCount = 0;
        }
    }
}