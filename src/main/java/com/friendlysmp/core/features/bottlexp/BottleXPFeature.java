package com.friendlysmp.core.features.bottlexp;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;

import java.util.List;

public class BottleXPFeature implements Feature {
    private final FriendlyCorePlugin plugin;

    public BottleXPFeature(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "bottle-xp";
    }

    @Override
    public void enable() {

        plugin.getServer().getPluginManager().registerEvents(new BottleListener(plugin), plugin);

        var bottleXPCommand = plugin.getCommand("bottlexp");

        if (bottleXPCommand != null) {
            bottleXPCommand.setExecutor(new BottleXPCommand(plugin));
            bottleXPCommand.setTabCompleter(new BottleXPCommand(plugin));
        }
    }

    @Override
    public void disable() {

    }

    @Override
    public void reload() {

    }



}
