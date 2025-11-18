package com.chalwk.file;

import com.chalwk.config.ConfigManager;
import com.chalwk.discord.DiscordBot;
import com.chalwk.discord.EventProcessor;
import com.chalwk.model.RawEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileWatcher {

    private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);

    // Default maximum file size (10MB)
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final ConfigManager configManager;
    private final DiscordBot discordBot;
    private final EventProcessor eventProcessor;
    private final ScheduledExecutorService scheduler;
    private final Map<String, FileState> fileStates;
    private boolean isWatching;
    private EventListener eventListener;

    public FileWatcher(ConfigManager configManager, DiscordBot discordBot, EventProcessor eventProcessor) {
        this.configManager = configManager;
        this.discordBot = discordBot;
        this.eventProcessor = eventProcessor;
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

        // Clear all existing text files to remove residual events
        clearExistingTextFiles(directory);

        int pollInterval = configManager.getConfig().getPollInterval();

        scheduler.scheduleAtFixedRate(this::checkForChanges, 0, pollInterval, TimeUnit.MILLISECONDS);
        isWatching = true;

        logger.info("Started watching directory: {}", watchDir);
        logger.info("Poll interval: {} ms", pollInterval);
        logger.info("Watching for .txt files (raw text format)");
        logger.info("Maximum file size: {} bytes ({} MB)", getMaxFileSize(), getMaxFileSize() / (1024 * 1024));
    }

    /**
     * Gets the maximum file size from config or uses default
     */
    private long getMaxFileSize() {
        return DEFAULT_MAX_FILE_SIZE;
    }

    /**
     * Clears all existing text files in the watch directory to remove residual events
     * from previous runs before starting the bot.
     */
    private void clearExistingTextFiles(File directory) {
        try {
            File[] textFiles = directory.listFiles((dir, name) -> name.endsWith(".txt"));

            if (textFiles == null || textFiles.length == 0) {
                logger.info("No text files found to clear in directory: {}", directory.getAbsolutePath());
                return;
            }

            int clearedCount = 0;
            for (File file : textFiles) {
                if (clearFileContent(file)) {
                    clearedCount++;
                    logger.info("Cleared residual events from file: {}", file.getName());
                } else {
                    logger.warn("Failed to clear file: {}", file.getName());
                }
            }

            logger.info("Successfully cleared {} text files to remove residual events", clearedCount);

        } catch (Exception e) {
            logger.error("Error clearing existing text files", e);
        }
    }

    /**
     * Clears the content of a file by overwriting it with an empty string.
     */
    private boolean clearFileContent(File file) {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("");
            return true;
        } catch (IOException e) {
            logger.error("Failed to clear file content: {}", file.getName(), e);
            return false;
        }
    }

    public void stopWatching() {
        if (!isWatching) return;

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
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".txt"));

            if (files == null) return;

            for (File file : files) processFileIfChanged(file);

        } catch (Exception e) {
            logger.error("Error checking for file changes", e);
        }
    }

    private void processFileIfChanged(File file) {
        try {
            String filePath = file.getAbsolutePath();
            long lastModified = file.lastModified();
            long fileSize = file.length();

            // Check if file is too large and clear it if needed
            if (fileSize > getMaxFileSize()) {
                logger.warn("File {} is too large ({} bytes > {} bytes). Clearing content.",
                        file.getName(), fileSize, getMaxFileSize());
                if (clearFileContent(file)) {
                    // Remove the file state so it will be treated as a new file
                    fileStates.remove(filePath);
                    logger.info("Successfully cleared oversized file: {}", file.getName());
                } else {
                    logger.error("Failed to clear oversized file: {}", file.getName());
                }
                return;
            }

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
            List<RawEvent> eventsToProcess = parseRawTextEvents(file, state);

            // No events to process
            if (eventsToProcess.isEmpty()) return;

            // Extract server name from filename (remove .txt extension)
            String serverName = file.getName().replace(".txt", "");

            // Ensure server channels exist in config
            configManager.getConfig().ensureServerChannels(serverName);

            // Process events
            for (RawEvent event : eventsToProcess) {
                String eventHash = generateEventHash(event);

                // Process the raw event with server name
                eventProcessor.processRawEvent(event, discordBot, serverName);

                // Mark as successfully processed
                state.processedEventHashes.add(eventHash);

                // Notify UI with server name
                if (eventListener != null) {
                    eventListener.onEventProcessed(event, serverName);
                }

                logger.debug("Successfully processed event: {} from server '{}'",
                        event.getEvent_type(), serverName);
            }

            logger.info("Processed {} events from server '{}'",
                    eventsToProcess.size(), serverName);

        } catch (Exception e) {
            logger.error("Error processing new events from file: {}", file.getName(), e);
        }
    }

    private List<RawEvent> parseRawTextEvents(File file, FileState state) {
        List<RawEvent> events = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                try {
                    RawEvent event = parseEventLine(line);
                    if (event != null) {
                        String eventHash = generateEventHash(event);

                        // Check if we've already processed this event
                        if (!state.processedEventHashes.contains(eventHash)) {
                            events.add(event);
                            state.processedEventHashes.add(eventHash);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse line {} in file {}: {}", lineNumber, file.getName(), line);
                }
            }

            logger.debug("Parsed {} new events from {}", events.size(), file.getName());

        } catch (Exception e) {
            logger.error("Failed to parse raw text events from file: {}", file.getName(), e);
        }

        return events;
    }

    private RawEvent parseEventLine(String line) {
        // Format: event_type|key1=value1|key2=value2|timestamp=123456789
        String[] parts = line.split("\\|");

        if (parts.length < 1) return null;

        RawEvent event = new RawEvent();
        Map<String, Object> data = new HashMap<>();

        // First part is always the event type
        event.setEvent_type(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int equalsIndex = part.indexOf('=');

            if (equalsIndex > 0) {
                String key = part.substring(0, equalsIndex);
                String value = unescapeValue(part.substring(equalsIndex + 1));

                switch (key) {
                    case "subtype":
                        event.setSubtype(value);
                        break;
                    case "timestamp":
                        try {
                            event.setTimestamp(Long.parseLong(value));
                        } catch (NumberFormatException e) {
                            event.setTimestamp(System.currentTimeMillis() / 1000);
                        }
                        break;
                    default:
                        data.put(key, value);
                        break;
                }
            }
        }

        event.setData(data);
        return event;
    }

    private String unescapeValue(String value) {
        if (value == null) return "";
        return value.replace("\\|", "|")
                .replace("\\n", "\n")
                .replace("\\r", "\r");
    }

    private String generateEventHash(RawEvent event) {
        try {
            StringBuilder content = new StringBuilder();

            content.append(event.getEvent_type()).append(":");
            if (event.getSubtype() != null) {
                content.append(event.getSubtype()).append(":");
            }
            content.append(event.getTimestamp()).append(":");

            // Include relevant data fields in the hash
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

            // Use consistent hash
            return Integer.toHexString(content.toString().hashCode());

        } catch (Exception e) {
            // Fallback to UUID with event type for basic uniqueness
            return event.getEvent_type() + "_" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    public interface EventListener {
        void onEventProcessed(RawEvent event, String serverName);
    }

    // Track state for each file
    private static class FileState {
        long lastModified;
        long fileSize;
        Set<String> processedEventHashes;

        FileState() {
            this.processedEventHashes = new HashSet<>();
        }
    }
}