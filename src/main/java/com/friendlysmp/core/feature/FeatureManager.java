package com.friendlysmp.core.feature;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FeatureManager {
    private final JavaPlugin plugin;
    private final Map<String, Feature> features = new LinkedHashMap<>();
    private final Map<String, Boolean> enabledState = new LinkedHashMap<>();

    public FeatureManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Feature feature) {
        String id = feature.id().toLowerCase();
        features.put(id, feature);
        enabledState.put(id, false);
    }
    
    public java.util.Collection<Feature> getFeatures() {
        return java.util.Collections.unmodifiableCollection(features.values());
    }

    public void enableConfigured() {
        for (Feature f : features.values()) {
            syncFeatureToConfig(f);
        }
    }

    public void reloadConfigured() {
        for (Feature f : features.values()) {
            syncFeatureToConfig(f);
        }
    }

    public void disableAll() {
        for (Feature f : features.values()) {
            if (enabledState.getOrDefault(f.id().toLowerCase(), false)) {
                try { disableFeature(f); } catch (Exception ignored) {}
            }
        }
    }

    private void syncFeatureToConfig(Feature f) {
        String id = f.id().toLowerCase();
        boolean shouldEnable = plugin.getConfig().getBoolean("features." + id + ".enabled", true);
        boolean isEnabled = enabledState.getOrDefault(id, false);

        if (shouldEnable && !isEnabled) {
            plugin.getLogger().info("Enabling feature: " + id);
            f.enable();
            enabledState.put(id, true);
            return;
        }

        if (!shouldEnable && isEnabled) {
            plugin.getLogger().info("Disabling feature: " + id);
            disableFeature(f);
            enabledState.put(id, false);
            return;
        }


        if (shouldEnable) {
            f.reload();
        }
    }

    private void disableFeature(Feature f) {
        try {
            f.disable();
        } finally {
            f.unregisterListeners();
        }
    }
}