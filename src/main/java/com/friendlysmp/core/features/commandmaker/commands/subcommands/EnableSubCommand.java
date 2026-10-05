package com.friendlysmp.core.features.commandmaker.commands.subcommands;

import com.friendlysmp.core.features.commandmaker.CommandFeature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;

import java.util.List;

public class EnableSubCommand {

    public static void runCommand(CommandSender sender, String[] args, CommandFeature feature) {
        // /cm enable <name>
        //      0        1

        if (args.length < 2) {
            sender.sendMessage(Component.text("Please specify an existing command", NamedTextColor.RED));
            return;
        }

        Configuration config = feature.getConfig();
        List<String> enabledCommands = config.getStringList(CommandFeature.ENABLED_COMMANDS);

        if (enabledCommands.contains(args[1])) {
            sender.sendMessage(Component.text("This command is already enabled!", NamedTextColor.YELLOW));
            return;
        }

        if (feature.getCommandKeys().contains(args[1])) {
            enabledCommands.add(args[1]);
            config.set(CommandFeature.ENABLED_COMMANDS, enabledCommands);
            feature.saveAndReload();

            sender.sendMessage(Component.text("Command " + args[1] + " has been enabled", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("This command does not exist!", NamedTextColor.RED));
        }
    }
}
