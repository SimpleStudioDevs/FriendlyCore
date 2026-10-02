package com.friendlysmp.core.features.playerbroadcast;

import com.friendlysmp.core.util.ConfirmationMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BroadcastCommand implements CommandExecutor, TabCompleter {
    private final BroadcastFeature plugin;
    private final Economy economy;

    public BroadcastCommand(BroadcastFeature plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        String name = command.getName().toLowerCase();

        if (name.equals("pbreload")) {
            if (!sender.hasPermission("friendlycore.pbc.admin")) {
                MessageUtil.send(sender, MessageUtil.mmConfig("no_permission"));
                return true;
            }
            plugin.reload();
            MessageUtil.send(sender, MessageUtil.mmConfig("reloaded"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("friendlycore.pbc.use")) {
            MessageUtil.send(player, MessageUtil.mmConfig("no_permission"));
            return true;
        }

        if (economy == null) {
            MessageUtil.send(player, MessageUtil.mmConfig("no_economy"));
            return true;
        }

        if (args.length == 0) {
            Component component = MessageUtil.mmConfig(
                    "usage",
                    Placeholder.unparsed("uses", String.valueOf(plugin.remainingUses(player))),
                    Placeholder.unparsed("time", plugin.remainingTimeFormatted(player)));
            MessageUtil.send(player, component);
            return true;
        }

        String message = String.join(" ", args);

        double cost = plugin.getConfig().getDouble("economy.cost", 150.0);
        boolean usedFreeUse;

        if (!(plugin.remainingUses(player) > 0)) { // Check free uses before charging

            double balance = economy.getBalance(player);
            if (balance < cost) {
                Component notEnough = MessageUtil.mmConfig(
                        "not_enough_money",
                        Placeholder.unparsed("cost", String.valueOf(cost))
                );
                MessageUtil.send(player, notEnough);
                return true;
            }


            usedFreeUse = false;
        } else {
            usedFreeUse = true;
            cost = 0;
        }



        Component broadcast = MessageUtil.mmConfig(
                "broadcast",
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("message", message)
        ).decoration(TextDecoration.ITALIC, false);

        broadcast = broadcast.hoverEvent(HoverEvent.showText(MessageUtil.mmConfig(
                "broadcast_hover",
                Placeholder.unparsed("player", player.getName())
        )));

        List<Component> preview = new ArrayList<>();
        preview.add(broadcast);

        double finalCost = cost;
        Component finalBroadcast = broadcast;
        ConfirmationMenu menu = new ConfirmationMenu((int) cost, preview, () -> {
            if (usedFreeUse) {
                plugin.consumeFreeUse(player.getUniqueId(), plugin.resolveGroup(player));
            } else {
                economy.withdrawPlayer(player, finalCost);
            }

            Bukkit.getServer().sendMessage(finalBroadcast);

            Component success = MessageUtil.mmConfig(
                    usedFreeUse ?  "success_free" : "success",
                    Placeholder.unparsed("cost", String.valueOf(finalCost))
            );
            MessageUtil.send(player, success);
        }, () -> player.sendMessage(Component.text("Cancelled broadcast.", NamedTextColor.RED)));

        menu.openMenu(player);

        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("pbreload")) {
            return new ArrayList<>();
        }
        if (args.length == 1) {
            return List.of("<message>");
        }
        return new ArrayList<>();
    }
}
