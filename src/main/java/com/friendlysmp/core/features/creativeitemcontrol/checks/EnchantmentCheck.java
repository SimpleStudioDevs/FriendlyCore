package com.friendlysmp.core.features.creativeitemcontrol.checks;

import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ConfigManager;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnchantmentCheck implements ItemCheck {
    private final CreativeFeature feature;

    public EnchantmentCheck(CreativeFeature feature) {
        this.feature = feature;
    }


    @Override
    public void check(ItemCheckContext ctx) {
        if (ctx.isCancelled()) return;
        if (!feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.ENCHANT_ENABLED)) return;
        boolean playerNotNull = ctx.player != null;
        if (playerNotNull && ctx.player.hasPermission("cic.bypass.enchantments")) return;

        Map<Enchantment, Integer> enchants = ctx.meta.getEnchants();
        if (enchants.isEmpty()) return;
        Set<Enchantment> seen = new HashSet<>();

        boolean found = false;

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();

            if (impossibleLevel(ctx, enchantment, level)) found = true;
            if (incompatibleEnchantment(ctx, enchantment, seen)) found = true;
            if (incompatibleItem(ctx, enchantment)) found = true;

            seen.add(enchantment);
        }

        if (!ctx.isCancelled() && !found) {
            if (ctx.meta instanceof BundleMeta bm) {
                for (ItemStack item : bm.getItems()) {
                    ItemMeta meta = item.getItemMeta();
                    Map<Enchantment, Integer> bundleEnchants = meta.getEnchants();
                    if (bundleEnchants.isEmpty()) continue;
                    Set<Enchantment> bundleSeen = new HashSet<>();

                    boolean bundleFound = false;
                    for (Map.Entry<Enchantment, Integer> entry : bundleEnchants.entrySet()) {
                        Enchantment enchantment = entry.getKey();
                        int level = entry.getValue();

                        if (impossibleLevel(ctx, enchantment, level)) bundleFound = true;
                        if (incompatibleEnchantment(ctx, enchantment, bundleSeen)) bundleFound = true;
                        if (incompatibleItem(ctx, enchantment)) bundleFound = true;

                        bundleSeen.add(enchantment);
                    }

                    if (bundleFound) {
                        ctx.cancel();
                        found = true;
                        break;
                    }

                }
            }
        }

        if (playerNotNull && found && feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.MAIN_PLAYER_ALERTS)) {
            feature.getMessageManager().sendAlert(ctx.player, ConfigManager.ConfigKeys.ALERTS_ENCHANTMENTS);
        }
    }

    private boolean impossibleLevel(ItemCheckContext ctx, Enchantment enchantment, int level) {
        if (level > enchantment.getMaxLevel()) {
            switch (feature.getConfigManager().getString(ConfigManager.ConfigKeys.ENCHANT_ACTION)) {
                case "LOWER" -> ctx.meta.addEnchant(enchantment, enchantment.getMaxLevel(), true);
                case "REMOVE" -> ctx.meta.removeEnchant(enchantment);
                case "DELETE" -> ctx.cancel();
                case null, default -> {
                }
            }
            return true;
        } else {
            return false;
        }

    }

    private boolean incompatibleEnchantment(ItemCheckContext ctx, Enchantment enchantment, Set<Enchantment> seen) {
        if (feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.ENCHANT_ALLOW_INCOMPATIBLE)) return false;

        for (Enchantment e : seen) {
            if (enchantment.conflictsWith(e)) {
                switch (feature.getConfigManager().getString(ConfigManager.ConfigKeys.ENCHANT_ACTION)) {
                    case "LOWER", "REMOVE" -> ctx.meta.removeEnchant(enchantment);
                    case "DELETE" -> ctx.cancel();
                    case null, default -> {
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean incompatibleItem(ItemCheckContext ctx, Enchantment enchantment) {
        if (!enchantment.canEnchantItem(ctx.item)) {
            switch (feature.getConfigManager().getString(ConfigManager.ConfigKeys.ENCHANT_ACTION)) {
                case "LOWER", "REMOVE" -> ctx.meta.removeEnchant(enchantment);
                case "DELETE" -> ctx.cancel();
                case null, default -> {
                }
            }
            return true;
        } else {
            return false;
        }

    }
}