package com.friendlysmp.core.features.achievementguard;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AchievementListener implements Listener {
    private final AchievementGuardFeature plugin;

    public AchievementListener(AchievementGuardFeature plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdvancementCriterion(PlayerAdvancementCriterionGrantEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        if (plugin.isWorldBlocked(worldName)) {
            event.setCancelled(true);
        }
    }
}
