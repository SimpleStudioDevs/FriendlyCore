package com.friendlysmp.core.features.homes;

import com.earth2me.essentials.IEssentials;
import net.essentialsx.api.v2.events.HomeModifyEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SetHomeListener implements Listener {
    private final com.earth2me.essentials.IEssentials essentials;
    private final HomesFeature feature;


    public SetHomeListener(HomesFeature feature, IEssentials essentials) {
        this.essentials = essentials;
        this.feature = feature;
    }

    @EventHandler
    public void onSetHome(HomeModifyEvent event) {
        if (event.getCause() != HomeModifyEvent.HomeModifyCause.CREATE) return;

        Player player = event.getUser().getBase();
        if (player == null) return;

        if (player.isOp()) return;

        int currentHomesCount = event.getUser().getHomes().size();
        int baseRankLimit = getBaseRankLimit(player);
        int purchasedBonusHomes = feature.getBonusHomes(player.getUniqueId());
        int maxAllowedHomes = baseRankLimit + purchasedBonusHomes;

        if (currentHomesCount >= maxAllowedHomes) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You have reached your limit of " + maxAllowedHomes + " homes!", NamedTextColor.RED));
        }

    }

    private int getBaseRankLimit(Player player) {
        if (player.hasPermission("essentials.sethome.multiple.legend")) return 20;
        if (player.hasPermission("essentials.sethome.multiple.supporter")) return 15;
        if (player.hasPermission("essentials.sethome.multiple.staff")) return 10;
        if (player.hasPermission("essentials.sethome.multiple.vip")) return 6;
        return 3;
    }

}
