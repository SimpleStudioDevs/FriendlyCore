package com.friendlysmp.core.features.commandmaker.commands.subcommands;

import com.friendlysmp.core.features.commandmaker.CommandFeature;
import com.friendlysmp.core.features.commandmaker.arguments.ArgVerification;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.friendlysmp.core.features.commandmaker.util.FilterUtil.filter;

public class EditSubCommand {

    public static void runCommand(CommandSender sender, String[] args, CommandFeature feature) {
        // /cm edit <name> <setting>
        //      0     1      2

        switch (args.length) {
            case 1 -> {
                sender.sendMessage(Component.text("Please specify a command name", NamedTextColor.RED));
                return;
            }
            case 2 -> {
                sender.sendMessage(Component.text("Please specify a setting to edit", NamedTextColor.RED));
                return;
            }
            default -> {}
        }

        if (!feature.getCommandKeys().contains(args[1])) {
            sender.sendMessage(Component.text("That command does not exist!", NamedTextColor.RED));
            return;
        }

        // Defer edit settings to their own methods
        switch (args[2].toLowerCase()) {
            case "action" -> editAction(sender, args, feature);
            case "permission" -> editPermission(sender, args, feature);
            case "alias" -> editAliases(sender, args, feature);
            default -> sender.sendMessage(Component.text("That setting does not exist! Options: action, permission, alias", NamedTextColor.RED));
        }
    }

    public static void editAction(CommandSender sender, String[] args, CommandFeature feature) {
        // /cm edit <command> action add/remove/list <(add)actiontype>
        //      0    1         2        3              4
        Configuration config = feature.getConfig();
        String path = CommandFeature.commandPath(args[1]) + ".actions";

        if (args.length == 3) {
            sender.sendMessage(Component.text("You must specify an action to perform on this setting", NamedTextColor.RED));
            return;
        }

        String option = args[3].toLowerCase();
        if (!Set.of("add", "remove", "list").contains(option)) {
            sender.sendMessage(Component.text("Unknown option. Specify either 'add', 'remove' or 'list'", NamedTextColor.RED));
            return;
        }

        List<String> actions = config.getStringList(path);

        if (option.equals("add")) {
            String actionPrefix = args.length < 5 ? null : switch (args[4].toLowerCase()) {
                case "message" -> "MESSAGE:";
                case "broadcast" -> "BROADCAST:";
                case "console" -> "CONSOLE:";
                case "player" -> "PLAYER:";
                case "sound" -> "SOUND:";
                case "soundall" -> "SOUNDALL:";
                default -> null;
            };

            if (actionPrefix == null) {
                sender.sendMessage(Component.text("Invalid action type! Options: message, broadcast, console, player, sound, soundall", NamedTextColor.RED));
                return;
            }

            String action = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
            if (action.isBlank() && !actionPrefix.equals("MESSAGE:") && !actionPrefix.equals("BROADCAST:")) {
                sender.sendMessage(Component.text("This action type MUST have text. Please include the command or sound", NamedTextColor.RED));
                return;
            }
            action = actionPrefix + action;

            actions.add(action);
            config.set(path, actions);
            feature.saveAndReload();

            sender.sendMessage(Component.text("Action '" + action + "' added to command " + args[1], NamedTextColor.GREEN));
        }

        if (option.equals("list")) {
            if (actions.isEmpty()) {
                sender.sendMessage(Component.text("There are no actions to list", NamedTextColor.YELLOW));
                return;
            }

            sender.sendMessage(Component.text("Actions for command " + args[1] + ":", NamedTextColor.YELLOW));
            for (int i = 0; i < actions.size(); i++) {
                sender.sendMessage(Component.text(i + " - " + actions.get(i)));
            }
        }

        if (option.equals("remove")) {
            if (args.length < 5 || !ArgVerification.isInteger(args[4])) {
                sender.sendMessage(Component.text("You must write the id to the action you wish to remove. Use /cm edit <name> action list to view IDs", NamedTextColor.RED));
                return;
            }

            int id = Integer.parseInt(args[4]);
            if (id < 0 || id >= actions.size()) {
                sender.sendMessage(Component.text("This action ID does not exist. Check /cm edit <name> action list", NamedTextColor.RED));
                return;
            }

            actions.remove(id);
            config.set(path, actions);
            feature.saveAndReload();
            sender.sendMessage(Component.text("Successfully removed action id " + id, NamedTextColor.GREEN));
        }
    }

    public static void editPermission(CommandSender sender, String[] args, CommandFeature feature) {
        // /cm edit <name> permission [permission]
        //       0    1        2           3

        Configuration config = feature.getConfig();
        String path = CommandFeature.commandPath(args[1]) + ".permission";

        if (args.length < 4) {
            config.set(path, null);
            sender.sendMessage(Component.text("Permission has been removed from this command. Any player will be able to run it", NamedTextColor.GREEN));
        } else {
            config.set(path, args[3]);
            sender.sendMessage(Component.text("Permission " + args[3] + " set for command " + args[1], NamedTextColor.GREEN));
        }
        feature.saveAndReload();
    }

    public static void editAliases(CommandSender sender, String[] args, CommandFeature feature) {
        // /cm edit <name> alias add/remove/list
        //        0     1      2         3

        Configuration config = feature.getConfig();
        String name = args[1];
        String path = CommandFeature.commandPath(name) + ".aliases";
        List<String> aliases = config.getStringList(path);

        if (args.length == 3) {
            sender.sendMessage(Component.text("You must specify an action to perform on this setting", NamedTextColor.RED));
            return;
        }

        String option = args[3].toLowerCase();
        if (!Set.of("add", "remove", "list").contains(option)) {
            sender.sendMessage(Component.text("Unknown option! Available options: add, remove, list", NamedTextColor.RED));
            return;
        }

        if (option.equals("add")) {
            if (args.length < 5) {
                sender.sendMessage(Component.text("Please specify a command alias", NamedTextColor.RED));
                return;
            }

            String alias = args[4].toLowerCase();
            if (aliases.contains(alias)) {
                sender.sendMessage(Component.text("This command already has that alias!", NamedTextColor.RED));
                return;
            }

            aliases.add(alias);
            config.set(path, aliases);
            feature.saveAndReload();
            sender.sendMessage(Component.text("Alias '" + alias + "' added to command " + name, NamedTextColor.GREEN));
        }

        if (option.equals("list")) {
            if (aliases.isEmpty()) {
                sender.sendMessage(Component.text("There are no aliases to list", NamedTextColor.YELLOW));
                return;
            }

            sender.sendMessage(Component.text("Aliases for command " + name + ":", NamedTextColor.YELLOW));
            for (int i = 0; i < aliases.size(); i++) {
                sender.sendMessage(Component.text(i + " - " + aliases.get(i)));
            }
        }

        if (option.equals("remove")) {
            if (args.length < 5) {
                sender.sendMessage(Component.text("Please specify a command alias", NamedTextColor.RED));
                return;
            }

            String alias = args[4].toLowerCase();
            if (!aliases.remove(alias)) {
                sender.sendMessage(Component.text("This command alias does not exist!", NamedTextColor.RED));
                return;
            }

            config.set(path, aliases);
            feature.saveAndReload();
            sender.sendMessage(Component.text("Successfully removed alias " + alias, NamedTextColor.GREEN));
        }
    }

    public static List<String> tabComplete(String[] args, CommandFeature feature) {
        if (args.length == 2) {
            return filter(args[1], feature.getCommandKeys());
        }

        if (args.length == 3) {
            return filter(args[2], List.of("permission", "action", "alias"));
        }

        String setting = args[2].toLowerCase();

        if (setting.equals("permission")) {
            if (args.length == 4) return List.of("<permissionNode>");
        }

        if (setting.equals("action")) {
            if (args.length == 4) {
                return filter(args[3], List.of("add", "list", "remove"));
            }
            if (args[3].equalsIgnoreCase("add")) {
                if (args.length == 5) return filter(args[4], List.of("console", "player", "message", "broadcast", "sound", "soundall"));
                if (args.length == 6) {
                    if (args[4].equalsIgnoreCase("sound") || args[4].equalsIgnoreCase("soundall")) {
                        return filter(args[5], Registry.SOUNDS.stream().map(s -> s.getKey().toString()).toList());
                    } else {
                        return List.of("<action>");
                    }
                }
            }
            if (args[3].equalsIgnoreCase("remove")) {
                if (args.length == 5) return List.of("<actionID>");
            }
        }

        if (setting.equals("alias")) {
            if (args.length == 4) {
                return filter(args[3], List.of("add", "list", "remove"));
            }
            if (args[3].equalsIgnoreCase("add")) {
                if (args.length == 5) return List.of("<alias>");
            }
            if (args[3].equalsIgnoreCase("remove")) {
                if (args.length == 5) return filter(args[4], feature.getConfig().getStringList(CommandFeature.commandPath(args[1]) + ".aliases"));
            }
        }

        return List.of();
    }
}
