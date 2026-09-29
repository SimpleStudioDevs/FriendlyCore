package com.friendlysmp.core.features.commandmaker.commands.subcommands;

import com.friendlysmp.core.features.commandmaker.CommandFeature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;

import java.util.List;

public class DisableSubCommand {

    public static void runCommand(CommandSender sender, String[] args, CommandFeature feature) {
        // /cm disable <name>
        //      0        1

        if (args.length < 2) {
            sender.sendMessage(Component.text("Please specify an existing command", NamedTextColor.RED));
            return;
        }

        Configuration config = feature.getConfig();
        List<String> enabledCommands = config.getStringList(CommandFeature.ENABLED_COMMANDS);

        if (enabledCommands.remove(args[1])) {
            config.set(CommandFeature.ENABLED_COMMANDS, enabledCommands);
            feature.saveAndReload();

            sender.sendMessage(Component.text("Command " + args[1] + " has been disabled", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("This command is not enabled or does not exist", NamedTextColor.RED));
        }
    }
}
