package com.friendlysmp.core.features.commandmaker.commands.subcommands;

import com.friendlysmp.core.features.commandmaker.CommandFeature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class CreateSubCommand {

    public static void runCommand(CommandSender sender, String[] args, CommandFeature feature) {
        // /cm create <name>

        if (args.length > 2) {
            sender.sendMessage(Component.text("Too many arguments! /cm create <name>", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Please specify a name for this command", NamedTextColor.RED));
            return;
        }

        String name = args[1].toLowerCase();
        if (feature.getCommandKeys().contains(name)) {
            sender.sendMessage(Component.text("This command already exists!", NamedTextColor.RED));
            return;
        }

        feature.getConfig().createSection(CommandFeature.commandPath(name));
        feature.saveAndReload();
        sender.sendMessage(Component.text("Added command \"" + name + "\"", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Use '/cm edit " + name + "' to edit the command", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Use '/cm enable " + name + "' to enable the command", NamedTextColor.GREEN));
    }
}
