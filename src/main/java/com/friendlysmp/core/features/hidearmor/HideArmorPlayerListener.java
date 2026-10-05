package com.friendlysmp.core.features.hidearmor;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

public final class HideArmorPlayerListener implements Listener {
    private final PlayerSettingsStore store;
    private final Map<Integer, UUID> playerEntityIds;

    public HideArmorPlayerListener(PlayerSettingsStore store, Map<Integer, UUID> playerEntityIds) {
        this.store = store;
        this.playerEntityIds = playerEntityIds;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        store.loadBlocking(e.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        playerEntityIds.put(e.getPlayer().getEntityId(), e.getPlayer().getUniqueId());
        store.ensureLoadedAsync(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        playerEntityIds.remove(e.getPlayer().getEntityId());
    }
}
