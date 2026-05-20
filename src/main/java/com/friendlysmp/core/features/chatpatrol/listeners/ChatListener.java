package com.friendlysmp.core.features.chatpatrol.listeners;

import com.friendlysmp.core.features.chatpatrol.ChatPatrolFeature;
import com.friendlysmp.core.features.chatpatrol.checks.BlacklistCheck;
import com.friendlysmp.core.features.chatpatrol.managers.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final ConfigManager configManager;
    private final BlacklistCheck blacklistCheck;

    public ChatListener(ChatPatrolFeature plugin) {
        this.configManager = plugin.getConfigManager();
        this.blacklistCheck = new BlacklistCheck(plugin);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String originalMessage = event.getMessage();
        String loweredMessage = originalMessage.toLowerCase();

        if (configManager.isWordFilterEnabled()) {
            blacklistCheck.handleChat(event, player, loweredMessage);
        }

    }
}
