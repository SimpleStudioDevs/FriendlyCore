package com.friendlysmp.core.features.creativeitemcontrol.listeners;

import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.creativeitemcontrol.checks.*;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DispenserListener implements Listener {
    private final CreativeFeature feature;
    private final AttributeCheck attributeCheck;
    private final ComponentCheck componentCheck;
    private final EnchantmentCheck enchantmentCheck;
    private final PotionCheck potionCheck;


    public DispenserListener(CreativeFeature feature) {
        this.feature = feature;
        this.attributeCheck = new AttributeCheck(feature);
        this.componentCheck = new ComponentCheck(feature);
        this.enchantmentCheck = new EnchantmentCheck(feature);
        this.potionCheck = new PotionCheck(feature);
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent e) {
        if (!feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.MAIN_DISPENSERS_ENABLED)) return;
        if (feature.getConfigManager().isWorldDenied(e.getBlock().getWorld())) return;

        ItemStack item = e.getItem();
        if (item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        ItemMeta originalMeta = meta.clone();

        if (meta.equals(feature.getConfigManager().getDefaultItem(item.getType()))) return;

        if (feature.getExcludedItemManager().isExcluded(item)) return;

        ItemCheckContext ctx = new ItemCheckContext(null, item, 0);
        attributeCheck.check(ctx);
        componentCheck.check(ctx);
        enchantmentCheck.check(ctx);
        potionCheck.check(ctx);


        boolean wasModified = !ctx.meta.equals(originalMeta);
        if (wasModified || ctx.isCancelled()) {
            e.setCancelled(true);
        }
    }

}
