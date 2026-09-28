package com.friendlysmp.core.features.creativeitemcontrol.listeners;

import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.creativeitemcontrol.checks.*;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ConfigManager;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryListener implements Listener {
    private final CreativeFeature feature;
    private final AttributeCheck attributeCheck;
    private final ComponentCheck componentCheck;
    private final EnchantmentCheck enchantmentCheck;
    private final PotionCheck potionCheck;

    public InventoryListener(CreativeFeature feature) {
        this.feature = feature;
        this.attributeCheck = new AttributeCheck(feature);
        this.componentCheck = new ComponentCheck(feature);
        this.enchantmentCheck = new EnchantmentCheck(feature);
        this.potionCheck = new PotionCheck(feature);
    }

    @EventHandler
    public void onCreativeInventory(InventoryCreativeEvent e) {
        if (!feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.MAIN_ENABLED)) return;
        if (feature.getConfigManager().isWorldDenied(e.getWhoClicked().getWorld())) return;
        if (e.getWhoClicked().hasPermission("cic.bypass")) return;

        // Setup Item Information
        boolean isDrop = e.getSlot() < 0;

        ItemStack item = e.getCursor();
        if (item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        ItemMeta originalMeta = meta.clone();

        Player p = (Player) e.getWhoClicked();

        if (meta.equals(Bukkit.getItemFactory().getItemMeta(item.getType()))) return;

        if (feature.getExcludedItemManager().isExcluded(item)) return;

        ItemCheckContext ctx = new ItemCheckContext(p, item, e.getSlot());

        attributeCheck.check(ctx);
        componentCheck.check(ctx);
        enchantmentCheck.check(ctx);
        potionCheck.check(ctx);

        boolean wasModified = !ctx.meta.equals(originalMeta);

        if (ctx.isCancelled()) {
            e.setCancelled(true);
        } else if (isDrop && wasModified) {
            e.setCancelled(true);
        } else {
            item.setItemMeta(ctx.getNewMeta());
            if (!isDrop && e.getSlot() < p.getInventory().getSize()) {
                p.getInventory().setItem(e.getSlot(), item);
            }
            if (wasModified) e.setCancelled(true);

        }

        if (ctx.isCancelled()) {
            e.setCancelled(true);
            return;
        }

        if (!wasModified) return;

        item.setItemMeta(ctx.getNewMeta());
        if (!isDrop && e.getSlot() < p.getInventory().getStorageContents().length) {
            p.getInventory().setItem(e.getSlot(), item);
        }
        if (isDrop) {
            e.setCancelled(true);
            return;
        }

        if (e.getSlot() == 40) {
            p.getInventory().setItemInOffHand(item);
            return;
        }


    }

    @EventHandler
    public void onInventorySlotChange(PlayerInventorySlotChangeEvent e) {
        if (!feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.MAIN_ENABLED)) return;
        if (e.getSlot() < 0) return;
        if (!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) return;
        if (feature.getConfigManager().isWorldDenied(e.getPlayer().getWorld())) return;

        if (e.getPlayer().hasPermission("cic.bypass")) return;

        ItemStack item = e.getNewItemStack();
        if (item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Player p = e.getPlayer();

        if (meta.equals(Bukkit.getItemFactory().getItemMeta(item.getType()))) return;

        if (feature.getExcludedItemManager().isExcluded(item)) return;

        ItemCheckContext ctx = new ItemCheckContext(p, item, e.getSlot());

        attributeCheck.check(ctx);
        componentCheck.check(ctx);
        enchantmentCheck.check(ctx);
        potionCheck.check(ctx);

        // getSlot() is relative to whatever inventory is currently open (including chests)
        // gotta check a different way so it doesn't cook itself
        boolean isOffHand = e.getSlot() == 40 || item.equals(p.getInventory().getItemInOffHand());

        if (ctx.isCancelled()) {
            if (isOffHand) {
                p.getInventory().setItemInOffHand(null);
            } else {
                p.getInventory().setItem(e.getSlot(), null);
            }
            p.updateInventory();
            return;
        }
        item.setItemMeta(ctx.getNewMeta());
        if (isOffHand) {
            p.getInventory().setItemInOffHand(item);
        } else {
            p.getInventory().setItem(e.getSlot(), item);
        }
        p.updateInventory();


    }

}
