package com.friendlysmp.core.features.deathmessage;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.placeholder.FriendlyCoreExpansion;
import com.friendlysmp.core.placeholder.PlaceholderProvider;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class DeathMessageFeature extends Feature implements Listener, PlaceholderProvider {
    private final PlayerSettingsStore store;

    public DeathMessageFeature(FriendlyCorePlugin plugin, PlayerSettingsStore store) {
        super(plugin);
        this.store = store;
    }

    @Override
    public String id() {
        return "deathmessage";
    }

    @Override
    public void enable() {
        PluginCommand cmd = plugin.getCommand("deathmessages");
        if (cmd != null) {
            DeathMessageCommand exec = new DeathMessageCommand(store);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            store.ensureLoadedAsync(online.getUniqueId());
        }
        registerListener(this);
    }

    @Override
    public void disable() {
        unregisterListener(this);
    }

    @Override
    public void reload() {

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        store.ensureLoadedAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Component msg = event.deathMessage();
        if (msg == null) return;

        event.deathMessage(null);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!store.isDeathMessagesHidden(player.getUniqueId())) {
                player.sendMessage(msg);
            }
        }

        Bukkit.getConsoleSender().sendMessage(msg);
    }

    @Override
    public void registerPlaceholders(FriendlyCoreExpansion expansion) {
        expansion.registerHandler("deathmessages", (player, args) -> {
            boolean muted = store.isDeathMessagesHidden(player.getUniqueId());
            if (args.length == 0) return muted ? "OFF" : "ON";
            if (args[0].equalsIgnoreCase("colored")) return muted ? "§cOFF" : "§aON";
            return muted ? "OFF" : "ON";
        });
    }

}
