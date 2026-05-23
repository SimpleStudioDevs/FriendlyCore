package com.friendlysmp.core.features.achievementguard;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class AchievementGuardFeature implements Feature, CommandExecutor {
    private final FriendlyCorePlugin plugin;
    private Set<String> blockedWorlds;

    public AchievementGuardFeature(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "achievement-guard";
    }

    @Override
    public void enable() {
        reloadBlockedWorlds();

        plugin.getServer().getPluginManager().registerEvents(new AchievementListener(this), plugin);

        if (plugin.getCommand("achievementguard") != null) {
            plugin.getCommand("achievementguard").setExecutor(this);
        }
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(plugin);
    }

    @Override
    public void reload() {
        reloadBlockedWorlds();
    }

    public void reloadBlockedWorlds() {
        plugin.reloadConfig();
        List<String> list = plugin.getConfig().getStringList("achievement-guard.BLOCKED-WORLDS");

        blockedWorlds = list.stream()
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
    }

    public boolean isWorldBlocked(String worldName) {
        if (worldName == null) return false;
        return blockedWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("friendlycore.admin")) {
            sender.sendMessage(Component.text("You do not have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadBlockedWorlds();
            sender.sendMessage(Component.text("Successfully reloaded!", NamedTextColor.GREEN));
        }


        return false;
    }

}
