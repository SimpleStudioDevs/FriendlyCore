package com.friendlysmp.core.features.hidearmor;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class HideArmorConditions implements Listener {
    private final Plugin plugin;
    private final PlayerSettingsStore store;
    private final Consumer<Player> refreshViewers;

    private final Map<UUID, Long> combatUntil = new ConcurrentHashMap<>();
    private final Set<UUID> inDisabledWorld = ConcurrentHashMap.newKeySet();

    private volatile long combatMillis;
    private volatile Set<String> disabledWorlds = Set.of();

    public HideArmorConditions(Plugin plugin, PlayerSettingsStore store, Consumer<Player> refreshViewers) {
        this.plugin = plugin;
        this.store = store;
        this.refreshViewers = refreshViewers;
    }

    public void configure(long combatSeconds, Collection<String> worlds) {
        combatMillis = Math.max(0L, combatSeconds) * 1000L;
        disabledWorlds = worlds.stream()
                .map(w -> w.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            online.getScheduler().run(plugin, task -> updateWorld(online), null);
        }
    }

    public boolean isForcedVisible(UUID uuid) {
        return isInDisabledWorld(uuid) || isInCombat(uuid);
    }

    public boolean isInCombat(UUID uuid) {
        Long until = combatUntil.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    public boolean isInDisabledWorld(UUID uuid) {
        return inDisabledWorld.contains(uuid);
    }

    private void updateWorld(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasForced = isForcedVisible(uuid);

        if (disabledWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
            inDisabledWorld.add(uuid);
        } else {
            inDisabledWorld.remove(uuid);
        }

        if (wasForced != isForcedVisible(uuid) && store.isArmorHidden(uuid)) {
            refreshViewers.accept(player);
        }
    }

    private void tag(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasForced = isForcedVisible(uuid);
        long until = System.currentTimeMillis() + combatMillis;
        combatUntil.put(uuid, until);

        if (!wasForced && store.isArmorHidden(uuid)) {
            player.getScheduler().run(plugin, task -> refreshViewers.accept(player), null);
        }
        player.getScheduler().runDelayed(plugin, task -> expire(player, until), null, combatMillis / 50L + 1L);
    }

    private void expire(Player player, long until) {
        UUID uuid = player.getUniqueId();
        if (!combatUntil.remove(uuid, until)) return;
        if (!isForcedVisible(uuid) && store.isArmorHidden(uuid)) {
            refreshViewers.accept(player);
        }
    }

    private static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) return shooter;
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (combatMillis <= 0L) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(e.getDamager());
        if (attacker == null || attacker.equals(victim)) return;

        tag(victim);
        tag(attacker);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        updateWorld(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent e) {
        updateWorld(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        combatUntil.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        combatUntil.remove(uuid);
        inDisabledWorld.remove(uuid);
    }
}
