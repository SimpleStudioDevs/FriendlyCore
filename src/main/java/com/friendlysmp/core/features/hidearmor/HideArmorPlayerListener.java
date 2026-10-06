package com.friendlysmp.core.features.hidearmor;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

public final class HideArmorPlayerListener implements Listener {
    private final Plugin plugin;
    private final PlayerSettingsStore store;
    private final Map<Integer, UUID> playerEntityIds;
    private final HideArmorConditions conditions;

    public HideArmorPlayerListener(Plugin plugin, PlayerSettingsStore store, Map<Integer, UUID> playerEntityIds, HideArmorConditions conditions) {
        this.plugin = plugin;
        this.store = store;
        this.playerEntityIds = playerEntityIds;
        this.conditions = conditions;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(PlayerArmorChangeEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!conditions.hidesOwnArmor() || !store.isArmorHidden(uuid) || conditions.isForcedVisible(uuid)) return;
        if (e.getOldItem().getType() == e.getNewItem().getType()) return;

        player.getScheduler().run(plugin, task -> player.updateInventory(), null);
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
