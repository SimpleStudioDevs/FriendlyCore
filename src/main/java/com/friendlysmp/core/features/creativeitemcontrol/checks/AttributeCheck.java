package com.friendlysmp.core.features.creativeitemcontrol.checks;

import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ConfigManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class AttributeCheck implements ItemCheck {

    private final CreativeFeature feature;


    public AttributeCheck(CreativeFeature feature) {
        this.feature = feature;
    }

    @Override
    public void check(ItemCheckContext ctx) {
        if (ctx.isCancelled()) return;
        if (!feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.ATTRIBUTES_ENABLED)) return;
        boolean playerNotNull = ctx.player != null;
        if (playerNotNull && ctx.player.hasPermission("cic.bypass.attributes")) return;

        boolean attributeIssue = ctx.meta.getAttributeModifiers() != null;

        if (!attributeIssue) {
            if (ctx.meta instanceof BundleMeta bm) {
                for (ItemStack item : bm.getItems()) {
                    ItemMeta meta =  item.getItemMeta();
                    if (meta.getAttributeModifiers() != null) {
                        ctx.cancel();
                        attributeIssue = true;
                        break;
                    }
                }
            }
        }

        if (attributeIssue) {
            if (feature.getConfigManager().getString(ConfigManager.ConfigKeys.ATTRIBUTES_ACTION).equalsIgnoreCase("REMOVE")) {
                ctx.meta.setAttributeModifiers(null);
            } else {
                ctx.cancel();
            }

            if (playerNotNull && feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.MAIN_PLAYER_ALERTS)) {
                feature.getMessageManager().sendAlert(ctx.player, ConfigManager.ConfigKeys.ALERTS_ATTRIBUTES);
            }
        }

}
}
