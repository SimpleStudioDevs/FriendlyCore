package com.friendlysmp.core.features.commandmaker.commands.subcommands;

import com.friendlysmp.core.features.commandmaker.CommandFeature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;

import java.util.List;

public class DeleteSubCommand {

    public static void runCommand(CommandSender sender, String[] args, CommandFeature feature) {
        // /cm delete <cmdName> confirm
        if (args.length == 1) {
            sender.sendMessage(Component.text("Please specify a command to delete", NamedTextColor.RED));
            return;
        }

        String name = args[1].toLowerCase();
        if (!feature.getCommandKeys().contains(name)) {
            sender.sendMessage(Component.text("This command does not exist", NamedTextColor.RED));
            return;
        }

        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage(Component.text("Are you sure you want to delete '" + name + "'? This CANNOT be undone.", NamedTextColor.RED, TextDecoration.BOLD));
            sender.sendMessage(Component.text("If you're sure, use /cm delete " + name + " confirm", NamedTextColor.DARK_RED, TextDecoration.BOLD));
            return;
        }

        Configuration config = feature.getConfig();
        config.set(CommandFeature.commandPath(name), null);
        List<String> enabledCommands = config.getStringList(CommandFeature.ENABLED_COMMANDS);
        enabledCommands.remove(name);
        config.set(CommandFeature.ENABLED_COMMANDS, enabledCommands);
        feature.saveAndReload();

        sender.sendMessage(Component.text("Command '" + name + "' has been deleted successfully", NamedTextColor.GREEN));
    }
}
