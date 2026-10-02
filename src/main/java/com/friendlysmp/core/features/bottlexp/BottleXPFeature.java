package com.friendlysmp.core.features.bottlexp;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import org.bukkit.configuration.Configuration;

public class BottleXPFeature extends Feature {
    private final FeatureConfig config;

    public BottleXPFeature(FriendlyCorePlugin plugin) {
        super(plugin);
        this.config = new FeatureConfig(plugin, "FeatureConfigs/bottlexp.yml");
    }

    @Override
    public String id() {
        return "bottle-xp";
    }

    @Override
    public void enable() {
        config.load();

        registerListener(new BottleListener(this));

        var bottleXPCommand = plugin.getCommand("bottlexp");

        if (bottleXPCommand != null) {
            BottleXPCommand command = new BottleXPCommand(this);
            bottleXPCommand.setExecutor(command);
            bottleXPCommand.setTabCompleter(command);
        }
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

    public FriendlyCorePlugin getPlugin() {
        return plugin;
    }
}
