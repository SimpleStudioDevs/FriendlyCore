package com.friendlysmp.core.features.sleepcap;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import org.bukkit.configuration.Configuration;

public class SleepCapFeature extends Feature {
    private final FeatureConfig config;

    public SleepCapFeature(FriendlyCorePlugin plugin) {
        super(plugin);
        this.config = new FeatureConfig(plugin, "FeatureConfigs/sleepcap.yml");
    }

    @Override
    public String id() {
        return "sleep-cap";
    }

    @Override
    public void enable() {
        config.load();
        registerListener(new EnterBedListener(this));
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {
        config.load();
    }

    public Configuration getConfig() {
        return config.get();
    }
}
