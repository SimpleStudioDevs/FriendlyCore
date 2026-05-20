package com.friendlysmp.core.features.chatpatrol.checks;

import com.friendlysmp.core.features.chatpatrol.ChatPatrolFeature;
import com.friendlysmp.core.features.chatpatrol.managers.ConfigManager;
import com.friendlysmp.core.features.chatpatrol.managers.LogManager;
import com.friendlysmp.core.features.chatpatrol.managers.PunishmentManager;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;

public class BlacklistCheck {

    private final ConfigManager configManager;
    private final LogManager logManager;
    private final PunishmentManager punishmentManager;


    public BlacklistCheck(ChatPatrolFeature plugin) {
        this.configManager = plugin.getConfigManager();
        this.logManager = new LogManager(plugin);
        this.punishmentManager = new PunishmentManager(plugin);
    }

    public boolean handleChat(AsyncPlayerChatEvent event, Player player, String message) {
        for (String word : configManager.getBlacklistedWords()) {
            if (message.contains(word.toLowerCase())) {
                event.setCancelled(true);
                logManager.logPunishment(player, "Blacklisted Word", message);
                punishmentManager.executeCommand(configManager.getBlacklistedWordsCommand(), player);
                return true;
            }
        }
        return false;
    }

    public boolean handleBook(PlayerEditBookEvent event, Player player, String message) {
        for (String word : configManager.getBlacklistedWords()) {
            if (message.contains(word.toLowerCase())) {
                event.setCancelled(true);
                logManager.logPunishment(player, "Blacklisted Word In Book", message);
                punishmentManager.executeCommand(configManager.getBlacklistedWordsCommand(), player);
                return true;
            }
        }
        return false;
    }

    public boolean containsBlackListedWord(String text) {
        String lowered = text.toLowerCase();
        for (String word : configManager.getBlacklistedWords()) {
            if (lowered.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
