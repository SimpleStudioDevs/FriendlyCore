package com.friendlysmp.core.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public final class FeatureConfig {
    private final JavaPlugin plugin;
    private final String resourcePath;
    private final File file;
    private final boolean applyDefaults;
    private YamlConfiguration yaml = new YamlConfiguration();

    public FeatureConfig(JavaPlugin plugin, String resourcePath) {
        this(plugin, resourcePath, true);
    }

    public FeatureConfig(JavaPlugin plugin, String resourcePath, boolean applyDefaults) {
        this.plugin = plugin;
        this.resourcePath = resourcePath;
        this.file = new File(plugin.getDataFolder(), resourcePath);
        this.applyDefaults = applyDefaults;
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }

        yaml = YamlConfiguration.loadConfiguration(file);
        if (!applyDefaults) return;

        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in != null) {
                yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read bundled defaults for " + resourcePath + ": " + e.getMessage());
        }
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save " + resourcePath + ": " + e.getMessage());
        }
    }

    public YamlConfiguration get() {
        return yaml;
    }

    public File getFile() {
        return file;
    }
}
