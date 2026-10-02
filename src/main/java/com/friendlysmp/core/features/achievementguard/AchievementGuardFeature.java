package com.friendlysmp.core.features.achievementguard;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class AchievementGuardFeature extends Feature implements CommandExecutor {
    private final FeatureConfig config;
    private Set<String> blockedWorlds;

    public AchievementGuardFeature(FriendlyCorePlugin plugin) {
        super(plugin);
        this.config = new FeatureConfig(plugin, "FeatureConfigs/achievementguard.yml");
    }

    @Override
    public String id() {
        return "achievement-guard";
    }

    @Override
    public void enable() {
        reloadBlockedWorlds();

        registerListener(new AchievementListener(this));

        if (plugin.getCommand("achievementguard") != null) {
            plugin.getCommand("achievementguard").setExecutor(this);
        }
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {
        reloadBlockedWorlds();
    }

    public void reloadBlockedWorlds() {
        config.load();
        List<String> list = config.get().getStringList("BLOCKED-WORLDS");

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
