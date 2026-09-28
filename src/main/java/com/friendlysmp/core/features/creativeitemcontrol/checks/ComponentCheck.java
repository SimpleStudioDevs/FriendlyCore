package com.friendlysmp.core.features.creativeitemcontrol.checks;

import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ConfigManager;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;

import java.util.Objects;

public class ComponentCheck implements ItemCheck{
    private final CreativeFeature feature;
    public ComponentCheck(CreativeFeature feature) {
        this.feature = feature;
    }


    @Override
    public void check(ItemCheckContext ctx) {
        if (ctx.isCancelled()) return;
        if (!feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.COMPONENTS_ENABLED)) return;
        boolean playerNotNull = ctx.player != null;
        if (playerNotNull && ctx.player.hasPermission("cic.bypass.components")) return;

        for (DataComponentType type : feature.getConfigManager().getResolvedComponents()) {

            if (ctx.item.hasData(type)) {
                ItemStack defaultItem = feature.getConfigManager().getDefaultItem(ctx.item.getType());
                if (type instanceof DataComponentType.Valued<?> valued) {
                    if (Objects.equals(defaultItem.getData(valued), ctx.item.getData(valued))) continue;
                } else {
                    if (defaultItem.hasData(type)) continue;
                }
                ctx.cancel();
            }
        }

        // Bundle handling
        if (!ctx.isCancelled()) {
            if (ctx.meta instanceof BundleMeta bm) {
                for (ItemStack item : bm.getItems()) {
                    for (DataComponentType type : feature.getConfigManager().getResolvedComponents()) {
                        ItemStack defaultItem = feature.getConfigManager().getDefaultItem(item.getType());
                        if (item.hasData(type)) {
                            if (type instanceof  DataComponentType.Valued<?> valued) {
                                if (Objects.equals(defaultItem.getData(valued), item.getData(valued))) continue;
                            } else {
                                if (defaultItem.hasData(type)) continue;
                            }
                        }
                        ctx.cancel();
                    }
                }
            }
        }



        if (playerNotNull && ctx.isCancelled() && feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.MAIN_PLAYER_ALERTS)) {
            feature.getMessageManager().sendAlert(ctx.player, ConfigManager.ConfigKeys.ALERTS_COMPONENTS);
        }
    }
}
