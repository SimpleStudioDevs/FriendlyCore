package com.friendlysmp.core.features.geysercombatlog;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class GeyserCombatLogFeature extends Feature implements Listener {

    public GeyserCombatLogFeature(FriendlyCorePlugin plugin) {
        super(plugin);
    }

    @Override
    public String id() {
        return "geyser-combat-log";
    }

    @Override
    public void enable() {
        registerListener(this);
    }

    @Override
    public void disable() {

    }

    @Override
    public void reload() {

    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().isDead()) event.getPlayer().spigot().respawn();
    }

}
