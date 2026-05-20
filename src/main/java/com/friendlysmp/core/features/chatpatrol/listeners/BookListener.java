package com.friendlysmp.core.features.chatpatrol.listeners;

import com.friendlysmp.core.features.chatpatrol.ChatPatrolFeature;
import com.friendlysmp.core.features.chatpatrol.checks.BlacklistCheck;
import com.friendlysmp.core.features.chatpatrol.managers.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

public class BookListener implements Listener {
    private final ConfigManager configManager;
    private final BlacklistCheck blacklistCheck;

    public BookListener(ChatPatrolFeature plugin) {
        this.configManager = plugin.getConfigManager();
        this.blacklistCheck = new BlacklistCheck(plugin);
    }

    @EventHandler
    public void onBookEdit(PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        BookMeta meta = event.getNewBookMeta();

        StringBuilder fullText = new StringBuilder();

        if (meta.hasTitle()) {
            fullText.append(meta.getTitle()).append(" ");
        }
        if (meta.hasPages()) {
            for (String page : meta.getPages()) {
                fullText.append(page).append(" ");
            }
        }

        String originalMessage = fullText.toString();
        String loweredMessage = originalMessage.toLowerCase();

        if (originalMessage.isBlank()) return;

        if (configManager.isWordFilterEnabled()) {
            if (blacklistCheck.handleBook(event, player, loweredMessage)) { return; }
        }

    }
}
