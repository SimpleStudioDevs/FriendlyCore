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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

public class AnvilListener implements Listener {
    private final ChatPatrolFeature plugin;
    private final BlacklistCheck blacklistCheck;
    private final LogManager logManager;
    private final PunishmentManager punishmentManager;

    public AnvilListener(ChatPatrolFeature plugin) {
        this.plugin = plugin;
        this.blacklistCheck = new BlacklistCheck(plugin);
        this.logManager = new LogManager(plugin);
        this.punishmentManager = new PunishmentManager(plugin);
    }

    @EventHandler
    public void onAnvilRename(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();;
        if (result == null || !result.hasItemMeta() || !result.getItemMeta().hasDisplayName()) return;

        String displayName =  result.getItemMeta().getDisplayName();

        if (blacklistCheck.containsBlackListedWord(displayName)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot name items with inappropriate words.", NamedTextColor.RED));
            logManager.logPunishment(player, "Anvil Rename", displayName);
            punishmentManager.executeCommand(
                    plugin.getConfigManager().getBlacklistedWordsCommand(),
                    player
            );
        }
    }
}
