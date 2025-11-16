package com.chalwk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;

public class UpdateChecker {

    private static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);

    // GitHub API URLs
    private static final String SCRIPT_COMMITS_URL = "https://api.github.com/repos/Chalwk/HALO-SCRIPT-PROJECTS/commits?path=sapp/utility/discord.lua&per_page=1";
    private static final String APP_RELEASES_URL = "https://api.github.com/repos/Chalwk/SAPPDiscordBot/releases/latest";

    private static final String CURRENT_VERSION = "1.0.0";
    private static final String UPDATE_CONFIG_FILE = "update_config.properties";

    public static void checkForUpdates(Component parent) {
        new Thread(() -> {
            try {
                boolean scriptUpdated = checkScriptUpdate();
                boolean appUpdated = checkAppUpdate();

                StringBuilder message = new StringBuilder();

                if (scriptUpdated) {
                    message.append("📝 The discord.lua script has been updated!\n")
                            .append("Please download the latest version from the GitHub repository.\n\n");
                } else {
                    message.append("📝 discord.lua script is up to date.\n\n");
                }

                if (appUpdated) {
                    message.append("🚀 A new version of SAPPDiscordBot is available!\n")
                            .append("Please download the latest release from the GitHub repository.");
                } else {
                    message.append("🚀 SAPPDiscordBot is up to date.");
                }

                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(parent,
                                message.toString(),
                                "Update Check",
                                JOptionPane.INFORMATION_MESSAGE)
                );

            } catch (Exception e) {
                logger.error("Error checking for updates", e);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(parent,
                                "Failed to check for updates: " + e.getMessage(),
                                "Update Check Error",
                                JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

    private static boolean checkScriptUpdate() throws IOException {
        try {
            String response = makeApiCall(SCRIPT_COMMITS_URL);

            // Extract the latest commit SHA from response
            String latestCommitSha = extractCommitShaFromResponse(response);
            if (latestCommitSha == null) {
                logger.warn("Could not extract commit SHA from response");
                return false;
            }

            // Load stored commit SHA
            String storedCommitSha = loadStoredCommitSha();

            if (storedCommitSha == null) {
                // First time running - store the current commit SHA but don't show as update
                storeCommitSha(latestCommitSha);
                return false;
            } else {
                // Compare with stored commit SHA
                boolean isUpdated = !latestCommitSha.equals(storedCommitSha);

                // Update stored commit SHA to the latest
                storeCommitSha(latestCommitSha);

                return isUpdated;
            }

        } catch (IOException e) {
            logger.warn("Failed to check script updates", e);
            throw e;
        }
    }

    private static boolean checkAppUpdate() throws IOException {
        try {
            String response = makeApiCall(APP_RELEASES_URL);

            // Extract the latest version from the response
            if (response.contains("\"tag_name\"")) {
                String versionTag = extractVersionFromResponse(response);
                return !versionTag.equals(CURRENT_VERSION);
            }

            return false;

        } catch (IOException e) {
            logger.warn("Failed to check app updates", e);
            throw e;
        }
    }

    private static String extractCommitShaFromResponse(String response) {
        try {
            // Look for "sha":"..." pattern in the commit response
            int start = response.indexOf("\"sha\":\"") + 7;
            if (start > 6) {
                int end = response.indexOf("\"", start);
                if (end > start) {
                    return response.substring(start, end);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract commit SHA from response", e);
        }
        return null;
    }

    private static String extractVersionFromResponse(String response) {
        try {
            // Simple extraction - look for "tag_name":"1.0.0" pattern
            int start = response.indexOf("\"tag_name\":\"") + 12;
            int end = response.indexOf("\"", start);
            if (start > 11 && end > start) {
                return response.substring(start, end);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract version from response", e);
        }
        return CURRENT_VERSION; // Fallback to current version
    }

    private static String loadStoredCommitSha() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(UPDATE_CONFIG_FILE)) {
            props.load(input);
            return props.getProperty("last_commit_sha");
        } catch (IOException e) {
            // File doesn't exist yet - this is normal for first run
            return null;
        }
    }

    private static void storeCommitSha(String commitSha) {
        Properties props = new Properties();
        props.setProperty("last_commit_sha", commitSha);

        try (OutputStream output = new FileOutputStream(UPDATE_CONFIG_FILE)) {
            props.store(output, "Update Checker Configuration - Last known commit SHA for discord.lua");
        } catch (IOException e) {
            logger.warn("Failed to store commit SHA", e);
        }
    }

    private static String makeApiCall(String urlString) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // User-Agent to comply with GitHub API requirements
            connection.setRequestProperty("User-Agent", "SAPPDiscordBot/" + CURRENT_VERSION);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            return response;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}