package com.friendlysmp.core.features.homes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class GrantHomesCommand implements CommandExecutor, TabCompleter {
    private final HomesFeature feature;
    private final boolean revoke;

    public GrantHomesCommand(HomesFeature feature, boolean revoke) {
        this.feature = feature;
        this.revoke = revoke;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("friendlycore.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /" + command.getName() + " <user> <int>", NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if  (!target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("That player does not exist!", NamedTextColor.RED));
            return true;
        }

        UUID uuid = target.getUniqueId();

        int homes;
        try {
            homes = Integer.parseInt(args[1]);
        }  catch (NumberFormatException e) {
            sender.sendMessage(Component.text("That is not a number!", NamedTextColor.RED));
            return true;
        }

        if (homes < 1) {
            sender.sendMessage(Component.text("Homes must be greater than 0", NamedTextColor.RED));
            return true;
        }

        String name = target.getName() != null ? target.getName() : args[0];
        int delta = revoke ? -homes : homes;
        feature.changeBonusHomes(uuid, delta, change -> {
            if (!change.applied()) {
                int max = feature.getMaxBonusHomes();
                sender.sendMessage(Component.text(name + " has " + change.before() + " bonus homes. Granting " + homes
                        + " would exceed the maximum of " + max + " (can grant up to " + Math.max(0, max - change.before()) + ")", NamedTextColor.RED));
                return;
            }
            String verb = revoke ? "Revoked " + (change.before() - change.after()) + " bonus homes from " : "Granted " + homes + " bonus homes to ";
            sender.sendMessage(Component.text(verb + name + " (total bonus: " + change.after() + ")", NamedTextColor.GREEN));
        }, () -> sender.sendMessage(Component.text("Failed to update homes, check console for details.", NamedTextColor.RED)));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        return switch (strings.length) {
            case 1 -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            case 2 -> List.of("<int>");
            default -> List.of();
        };
    }
}
