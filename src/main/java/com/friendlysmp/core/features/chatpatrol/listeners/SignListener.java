package com.friendlysmp.core.features.chatpatrol.listeners;

import com.friendlysmp.core.features.chatpatrol.ChatPatrolFeature;
import com.friendlysmp.core.features.chatpatrol.checks.BlacklistCheck;
import com.friendlysmp.core.features.chatpatrol.managers.LogManager;
import com.friendlysmp.core.features.chatpatrol.managers.PunishmentManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class SignListener implements Listener {
    private final ChatPatrolFeature plugin;
    private final BlacklistCheck blacklistCheck;
    private final LogManager logManager;
    private final PunishmentManager punishmentManager;

    public SignListener(ChatPatrolFeature plugin) {
        this.plugin = plugin;
        this.blacklistCheck = new BlacklistCheck(plugin);
        this.logManager = new LogManager(plugin);
        this.punishmentManager = new PunishmentManager(plugin);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();

        for (String line : event.getLines()) {
            if (line != null && blacklistCheck.containsBlackListedWord(line)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Your sign contains inappropriate language and was not placed.", NamedTextColor.RED));
                logManager.logPunishment(player, "Sign Text", line);
                punishmentManager.executeCommand(
                        plugin.getConfigManager().getBlacklistedWordsCommand(),
                        player
                );
            }
        }
    }
}
