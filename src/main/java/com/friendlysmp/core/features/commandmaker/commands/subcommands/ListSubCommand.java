package com.friendlysmp.core.features.commandmaker.commands.subcommands;

import com.friendlysmp.core.features.commandmaker.CommandFeature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ListSubCommand {

    public static void runCommand(CommandSender sender, CommandFeature feature) {
        sender.sendMessage(Component.text("Custom Commands:", NamedTextColor.YELLOW, TextDecoration.BOLD));

        List<String> enabledCommands = feature.getConfig().getStringList(CommandFeature.ENABLED_COMMANDS);
        int i = 0;
        for (String s : feature.getCommandKeys()) {
            Component line = Component.text(" " + i + " - " + s);
            if (!enabledCommands.contains(s)) line = line.append(Component.text(" (disabled)", NamedTextColor.GRAY));
            sender.sendMessage(line);
            i++;
        }
    }
}
