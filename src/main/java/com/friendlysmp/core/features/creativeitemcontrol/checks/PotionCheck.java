package com.friendlysmp.core.features.creativeitemcontrol.checks;

import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ConfigManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.PotionMeta;

public class PotionCheck implements ItemCheck{
    private final CreativeFeature feature;

    public PotionCheck(CreativeFeature feature) {
        this.feature = feature;
    }

    @Override
    public void check(ItemCheckContext ctx) {
        if (ctx.isCancelled()) return;
        if (!feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.POTIONS_ENABLED)) return;
        boolean playerNotNull = ctx.player != null;
        if (playerNotNull && ctx.player.hasPermission("cic.bypass.potions")) return;

        if (ctx.meta instanceof BundleMeta bm) {
            for (ItemStack item : bm.getItems()) {
                if (item.getItemMeta() instanceof PotionMeta pm) {
                    if (pm.hasCustomEffects()) {
                        ctx.cancel();
                        if (playerNotNull && feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.MAIN_PLAYER_ALERTS)) {
                            feature.getMessageManager().sendAlert(ctx.player, ConfigManager.ConfigKeys.ALERTS_POTIONS);
                        }
                        return;
                    }
                }
            }
            return;
        }

        switch (ctx.item.getType()) {
            case POTION, LINGERING_POTION, SPLASH_POTION -> {}
            default -> { return; }

        }
        PotionMeta potionMeta = (PotionMeta)  ctx.item.getItemMeta();

        if (potionMeta.hasCustomEffects()) {
            if (playerNotNull && feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.MAIN_PLAYER_ALERTS))  {
                feature.getMessageManager().sendAlert(ctx.player, ConfigManager.ConfigKeys.ALERTS_POTIONS);
            }
            ctx.cancel();
        }

    }
}
