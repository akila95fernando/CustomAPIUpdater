package com.sample.utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigLoader {

    private static final Logger logger = Logger.getLogger(ConfigLoader.class.getName());
    private Properties properties;

    public ConfigLoader(String configFilePath) {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            properties.load(fis);
            logger.log(Level.INFO, "Configuration loaded successfully from: " + configFilePath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load configuration file: " + configFilePath, e);
            System.exit(1);
        }
    }

    public String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            logger.log(Level.WARNING, "Property not found: " + key);
            return "";
        }
        return value.trim();
    }

    public List<String> getListProperty(String key) {
        List<String> list = new ArrayList<>();
        String value = getProperty(key);

        if (value.isEmpty()) {
            return list;
        }

        // Remove brackets and split by comma
        value = value.replaceAll("[\\[\\]]", "").trim();
        if (!value.isEmpty()) {
            String[] items = value.split(",");
            for (String item : items) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }

        return list;
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public void printAllProperties() {
        logger.log(Level.INFO, "Current Configuration:");
        properties.forEach((key, value) -> logger.log(Level.INFO, key + " = " + value));
    }
}