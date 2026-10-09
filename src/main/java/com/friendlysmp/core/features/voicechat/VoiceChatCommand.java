package com.friendlysmp.core.features.voicechat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VoiceChatCommand implements CommandExecutor, TabCompleter {
    private final VoiceChatFeature feature;

    public VoiceChatCommand(VoiceChatFeature feature) {
        this.feature = feature;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(feature.message("players_only"));
            return true;
        }

        if (strings.length == 0 || (strings.length == 1 && strings[0].equalsIgnoreCase("toggle"))) {
            if (feature.isToggled(player)) {
                feature.setToggled(player, false);
                player.sendMessage(feature.message("toggle_off"));
                return true;
            }
            if (feature.getGroup(player) == null) {
                player.sendMessage(feature.message("not_in_group"));
                return true;
            }
            feature.setToggled(player, true);
            player.sendMessage(feature.message("toggle_on"));
            return true;
        }

        feature.sendGroupMessage(player, String.join(" ", strings), true);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (strings.length == 1 && "toggle".startsWith(strings[0].toLowerCase())) return List.of("toggle");
        return List.of();
    }
}
