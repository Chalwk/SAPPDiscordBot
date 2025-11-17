package com.chalwk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "sapp_bot_config.json";

    private final ObjectMapper objectMapper;
    private AppConfig config;

    public ConfigManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        loadConfig();
    }

    public AppConfig getConfig() {
        return config;
    }

    public void saveConfig(AppConfig newConfig) throws IOException {
        this.config = newConfig;
        objectMapper.writeValue(new File(CONFIG_FILE), config);
        logger.info("Configuration saved to {}", CONFIG_FILE);
    }

    private void loadConfig() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                config = objectMapper.readValue(configFile, AppConfig.class);
                logger.info("Configuration loaded from {}", CONFIG_FILE);

                // Ensure backward compatibility - if serverEventConfigs is null, initialize it
                if (config.getServerEventConfigs() == null) {
                    config.setServerEventConfigs(new HashMap<>());
                }
            } else {
                config = new AppConfig();
                logger.info("No configuration file found, using defaults");
            }
        } catch (IOException e) {
            logger.warn("Error loading configuration, using defaults", e);
            config = new AppConfig();
        }
    }
}