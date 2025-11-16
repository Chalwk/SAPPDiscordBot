package com.chalwk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class UpdateChecker {

    private static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);

    // GitHub API URLs
    private static final String SCRIPT_COMMITS_URL = "https://api.github.com/repos/Chalwk/HALO-SCRIPT-PROJECTS/commits?path=sapp/utility/discord.lua&per_page=1";
    private static final String APP_RELEASES_URL = "https://api.github.com/repos/Chalwk/SAPPDiscordBot/releases/latest";

    private static final String CURRENT_VERSION = "1.0.0";

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
            return response.contains("\"commit\"") && response.contains("\"author\"");

        } catch (IOException e) {
            logger.warn("Failed to check script updates", e);
            throw e;
        }
    }

    private static boolean checkAppUpdate() throws IOException {
        try {
            String response = makeApiCall(APP_RELEASES_URL);

            // Extract the latest version from the response
            // Look for "tag_name" in the JSON response
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

    private static String makeApiCall(String urlString) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Add User-Agent to comply with GitHub API requirements
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