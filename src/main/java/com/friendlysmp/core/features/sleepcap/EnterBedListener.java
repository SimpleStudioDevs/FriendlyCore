package com.friendlysmp.core.features.sleepcap;

import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EnterBedListener implements Listener {
    private final SleepCapFeature feature;

    public EnterBedListener(SleepCapFeature feature) {
        this.feature = feature;
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!event.getPlayer().getWorld().getName().equals("world")) return;
        recalculate();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        recalculate();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        recalculate();
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        recalculate();
    }

    private void recalculate() {
        World world = Bukkit.getWorld("world");
        if (world == null) return;

        int playersInWorld = world.getPlayers().size();
        if (playersInWorld == 0) return;

        int basePercent = feature.getConfig().getInt("base-percentage", 25);
        int cap = feature.getConfig().getInt("limit", 5);

        int rawRequired = (int) Math.ceil(playersInWorld * basePercent / 100.0);
        int cappedRequired = Math.min(rawRequired, cap);
        int newPercent = cappedRequired * 100 / playersInWorld;

        world.setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, newPercent);
    }
}