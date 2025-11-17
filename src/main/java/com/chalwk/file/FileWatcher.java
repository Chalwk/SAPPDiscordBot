package com.chalwk.file;

import com.chalwk.config.ConfigManager;
import com.chalwk.discord.DiscordBot;
import com.chalwk.discord.EventProcessor;
import com.chalwk.model.RawEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
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
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final Map<String, FileState> fileStates;
    private boolean isWatching;
    private EventListener eventListener;

    public FileWatcher(ConfigManager configManager, DiscordBot discordBot, EventProcessor eventProcessor) {
        this.configManager = configManager;
        this.discordBot = discordBot;
        this.eventProcessor = eventProcessor;
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.fileStates = new HashMap<>();
    }

    public void setEventListener(EventListener listener) {
        this.eventListener = listener;
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

        // Create directory if it doesn't exist
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                logger.info("Created watch directory: {}", watchDir);
            } else {
                logger.error("Failed to create watch directory: {}", watchDir);
                return;
            }
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
            FileWriter writer = new FileWriter(file);
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
            List<RawEvent> allEvents = parseRawEvents(file);

            if (allEvents.isEmpty()) {
                return; // No events to process
            }

            // Extract server name from filename (remove .json extension)
            String serverName = file.getName().replace(".json", "");

            List<RawEvent> eventsToProcess = new ArrayList<>();

            if (state.processedEventHashes.isEmpty()) {
                eventsToProcess.addAll(allEvents);
                logger.debug("Processing all {} events from file: {}", allEvents.size(), file.getName());
            } else {
                // Filter out already processed events
                for (RawEvent event : allEvents) {
                    String eventHash = generateEventHash(event);
                    if (!state.processedEventHashes.contains(eventHash)) {
                        eventsToProcess.add(event);
                        logger.debug("New event found: {}", eventHash.substring(0, 8));
                    }
                }
            }

            // Process events and track successful ones
            for (RawEvent event : eventsToProcess) {
                String eventHash = generateEventHash(event);

                // Process the raw event (this will apply templates from Java config)
                eventProcessor.processRawEvent(event, discordBot);

                // Mark as successfully processed
                state.processedEventHashes.add(eventHash);

                // Notify UI with server name
                if (eventListener != null) {
                    eventListener.onEventProcessed(event, serverName);
                }

                logger.debug("Successfully processed event: {} from server '{}'",
                        event.getEvent_type(), serverName);
            }

            // Clear the file after processing all events
            if (!eventsToProcess.isEmpty()) {
                clearFileAfterProcessing(file, state);
                logger.info("Processed {} events from server '{}' and cleared file",
                        eventsToProcess.size(), serverName);
            }

        } catch (Exception e) {
            logger.error("Error processing new events from file: {}", file.getName(), e);
            // Don't clear state on error to avoid losing track of processed events
        }
    }

    private List<RawEvent> parseRawEvents(File file) {
        try {
            // Handle both array and single event formats
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            if (content.trim().isEmpty() || content.trim().equals("[]")) {
                return new ArrayList<>();
            }

            if (content.trim().startsWith("[")) {
                // Array format
                return objectMapper.readValue(file, new TypeReference<List<RawEvent>>() {
                });
            } else {
                // Single event format
                RawEvent singleEvent = objectMapper.readValue(file, RawEvent.class);
                return Collections.singletonList(singleEvent);
            }
        } catch (Exception e) {
            logger.error("Failed to parse raw events from file: {}", file.getName(), e);
            return new ArrayList<>();
        }
    }

    // Clear file but maintain state of processed events
    private void clearFileAfterProcessing(File file, FileState state) {
        try {
            // Clear the physical file
            FileWriter writer = new FileWriter(file);
            writer.write("[]");
            writer.close();

            // Update file state
            state.lastModified = file.lastModified();
            state.fileSize = file.length();

            logger.debug("File cleared after processing: {}", file.getName());
        } catch (Exception e) {
            logger.error("Failed to clear file after processing: {}", file.getName(), e);
        }
    }

    private String generateEventHash(RawEvent event) {
        try {
            StringBuilder content = new StringBuilder();

            content.append(event.getEvent_type()).append(":");
            if (event.getSubtype() != null) {
                content.append(event.getSubtype()).append(":");
            }
            content.append(event.getTimestamp()).append(":");

            // Include relevant data fields in the hash to identify unique events
            if (event.getData() != null) {
                // For different event types, use different identifying fields
                switch (event.getEvent_type()) {
                    case "event_join":
                    case "event_leave":
                        if (event.getData().get("name") != null) {
                            content.append(event.getData().get("name"));
                        }
                        if (event.getData().get("id") != null) {
                            content.append(event.getData().get("id"));
                        }
                        break;
                    case "event_chat":
                        if (event.getData().get("name") != null) {
                            content.append(event.getData().get("name"));
                        }
                        if (event.getData().get("msg") != null) {
                            content.append(event.getData().get("msg"));
                        }
                        break;
                    case "event_death":
                        if (event.getData().get("victimName") != null) {
                            content.append(event.getData().get("victimName"));
                        }
                        if (event.getData().get("killerName") != null) {
                            content.append(event.getData().get("killerName"));
                        }
                        break;
                    case "event_score":
                        if (event.getData().get("name") != null) {
                            content.append(event.getData().get("name"));
                        }
                        if (event.getData().get("score") != null) {
                            content.append(event.getData().get("score"));
                        }
                        break;
                    default:
                        // For other events, include all data for uniqueness
                        for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                            content.append(entry.getKey()).append(":").append(entry.getValue()).append(":");
                        }
                }
            }

            // Use a consistent hash to ensure same events have same hash
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
            // Fallback to UUID with event type for basic uniqueness
            return event.getEvent_type() + "_" + UUID.randomUUID();
        }
    }

    public boolean isWatching() {
        return isWatching;
    }

    public interface EventListener {
        void onEventProcessed(RawEvent event, String serverName);
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