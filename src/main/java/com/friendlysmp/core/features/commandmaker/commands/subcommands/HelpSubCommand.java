package com.friendlysmp.core.features.commandmaker.commands.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

public class HelpSubCommand {

    public static void runCommand(CommandSender sender) {
        sender.sendMessage(Component.text("CommandMaker Help", NamedTextColor.YELLOW, TextDecoration.UNDERLINED, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Commands:", NamedTextColor.GREEN, TextDecoration.BOLD));
        sender.sendMessage(Component.text(" /cm reload | Reload CommandMaker commands", NamedTextColor.WHITE));
        sender.sendMessage(Component.text(" /cm create <command name> | Create a new custom command", NamedTextColor.WHITE));
        sender.sendMessage(Component.text(" /cm enable <command name> | Enable a command", NamedTextColor.WHITE));
        sender.sendMessage(Component.text(" /cm disable <command name> | Disable a command", NamedTextColor.WHITE));
        sender.sendMessage(Component.text(" /cm list | List all custom commands", NamedTextColor.WHITE));
        sender.sendMessage(Component.text(" /cm delete <command> | Delete a custom command", NamedTextColor.WHITE));
        sender.sendMessage("");
        sender.sendMessage(Component.text("Edit Commands:", NamedTextColor.GREEN, TextDecoration.BOLD));
        sender.sendMessage(Component.text(" /cm edit <command> permission [permission node] | Set a permission for a command. Leave blank to remove", NamedTextColor.WHITE));
        sender.sendMessage(Component.text(" /cm edit <command> action <add/list/remove> | Edit command actions", NamedTextColor.WHITE));
        sender.sendMessage(Component.text(" /cm edit <command> alias <add/list/remove> | Edit command aliases", NamedTextColor.WHITE));
        sender.sendMessage("");
        sender.sendMessage(Component.text("Argument Commands:", NamedTextColor.GREEN, TextDecoration.BOLD));
        sender.sendMessage(Component.text(" /cm argument <command> add <argName> <argType> [options]", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  The [options] section is only for the string argument type. Every string written after will be an option for the command. Leave blank for no options/any string", NamedTextColor.WHITE));
        sender.sendMessage(Component.text(" /cm argument <command> list | List arguments for a command", NamedTextColor.WHITE));
        sender.sendMessage(Component.text(" /cm argument <command> remove <argName> | Remove an argument from the command", NamedTextColor.WHITE));
    }
}
