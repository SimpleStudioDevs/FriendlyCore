package com.friendlysmp.core.feature;

import com.friendlysmp.core.FriendlyCorePlugin;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public abstract class Feature {
    protected final FriendlyCorePlugin plugin;
    private final List<Listener> listeners = new ArrayList<>();

    protected Feature(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
    }

    public abstract String id();
    public abstract void enable();
    public abstract void disable();
    public abstract void reload();

    protected void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
    }

    protected void unregisterListener(Listener listener) {
        HandlerList.unregisterAll(listener);
        listeners.remove(listener);
    }

    final void unregisterListeners() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
    }
}
