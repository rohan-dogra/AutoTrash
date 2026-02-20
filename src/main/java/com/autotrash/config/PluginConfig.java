package com.autotrash.config;

import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    private final int mostTrashedLimit;

    public PluginConfig(FileConfiguration config) {
        this.mostTrashedLimit = config.getInt("most-trashed-limit", 45);
    }

    public int getMostTrashedLimit() {
        return mostTrashedLimit;
    }
}
