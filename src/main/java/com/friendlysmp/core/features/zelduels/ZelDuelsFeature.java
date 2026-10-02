package com.friendlysmp.core.features.zelduels;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import com.zeltuv.zelduels.api.events.ArenaPreStartEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ZelDuelsFeature extends Feature implements Listener {

    public ZelDuelsFeature(FriendlyCorePlugin plugin) {
        super(plugin);
    }

    @Override
    public String id() {
        return "zelduels";
    }

    @Override
    public void enable() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ZelDuels")) {
            plugin.getLogger().warning("ZelDuels is not enabled!");
            return;
        }

        registerListener(this);

    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {

    }

    @EventHandler
    public void onArenaPreStart(ArenaPreStartEvent event) {
        Player player1 = event.getPlayer1();
        Player player2 = event.getPlayer2();

        if (!player1.getGameMode().equals(GameMode.SURVIVAL)) {
            player1.sendMessage(Component.text("Duel cancelled because you are not in survival mode!", NamedTextColor.RED));
            player2.sendMessage(Component.text("Duel cancelled because the other player is not in survival mode!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (!player2.getGameMode().equals(GameMode.SURVIVAL)) {
            player1.sendMessage(Component.text("Duel cancelled because the other player is not in survival mode!", NamedTextColor.RED));
            player2.sendMessage(Component.text("Duel cannceled becaused you are not in survival mode!",  NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
    }
}
