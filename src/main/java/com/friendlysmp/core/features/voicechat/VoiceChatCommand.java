package com.friendlysmp.core.features.voicechat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            commandSender.sendMessage(Component.text("You must be a player to use this command!", NamedTextColor.RED));
            return true;
        }

        if (strings.length == 0 || (strings.length == 1 && strings[0].equalsIgnoreCase("toggle"))) {
            if (feature.isToggled(player)) {
                feature.setToggled(player, false);
                player.sendMessage(Component.text("Your messages will now go to public chat.", NamedTextColor.GRAY));
                return true;
            }
            if (feature.getGroup(player) == null) {
                player.sendMessage(Component.text("You are not in a VoiceChat group!", NamedTextColor.RED));
                return true;
            }
            feature.setToggled(player, true);
            player.sendMessage(Component.text("Your messages will now go to your VoiceChat group.", NamedTextColor.GRAY));
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
