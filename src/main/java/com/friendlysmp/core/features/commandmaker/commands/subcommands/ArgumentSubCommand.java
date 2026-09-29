package com.friendlysmp.core.features.commandmaker.commands.subcommands;

import com.friendlysmp.core.features.commandmaker.CommandFeature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

import static com.friendlysmp.core.features.commandmaker.util.FilterUtil.filter;

public class ArgumentSubCommand {
    public static void runCommand(CommandSender sender, String[] args, CommandFeature feature) {
        // /cm argument <name> add <argName> <argType> [(STRING) option1 option2 option 3 | (PLAYER) true/false]
        //       0          1   2     3           4
        // /cm argument <name> remove <argName>
        // /cm argument <name> list

        switch (args.length) {
            case 1 -> {
                sender.sendMessage(Component.text("Please specify the name of the command to edit", NamedTextColor.RED));
                return;
            }
            case 2 -> {
                sender.sendMessage(Component.text("You must specify an action. Options: add, list, remove", NamedTextColor.RED));
                return;
            }
            case 3 -> {
                if (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove")) {
                    sender.sendMessage(Component.text("You must specify an argument name", NamedTextColor.RED));
                    return;
                }
            }
        }

        String option = args[2].toLowerCase();
        if (!Set.of("add", "remove", "list").contains(option)) {
            sender.sendMessage(Component.text("Unknown option! Select add, remove, or list", NamedTextColor.RED));
            return;
        }

        String name = args[1].toLowerCase();
        if (!feature.getCommandKeys().contains(name)) {
            sender.sendMessage(Component.text("This command does not exist!", NamedTextColor.RED));
            return;
        }

        Configuration config = feature.getConfig();
        String argsPath = CommandFeature.commandPath(name) + ".args";

        // /cm argument <name> add <argName> <argType> [options]
        if (option.equals("add")) {
            Set<String> availableTypes = Set.of("string", "player", "int", "float");
            if (args.length < 5 || !availableTypes.contains(args[4].toLowerCase())) {
                sender.sendMessage(Component.text("Please specify a valid argument type. Options: string, player, int, float", NamedTextColor.RED));
                return;
            }

            String type = args[4].toUpperCase();
            String argPath = argsPath + "." + args[3];

            config.set(argPath + ".type", type);
            if (type.equals("STRING") && args.length > 5) {
                config.set(argPath + ".options", new ArrayList<>(Arrays.asList(Arrays.copyOfRange(args, 5, args.length))));
            }

            if (type.equals("PLAYER") && args.length > 5) {
                config.set(argPath + ".placeholder", Boolean.parseBoolean(args[5]));
            }
            feature.saveAndReload();
            sender.sendMessage(Component.text("Argument " + args[3] + " added to command " + name, NamedTextColor.GREEN));
        }

        ConfigurationSection argsSection = config.getConfigurationSection(argsPath);
        Set<String> availableArguments = argsSection == null ? Set.of() : argsSection.getKeys(false);

        // /cm argument <name> list
        if (option.equals("list")) {
            if (availableArguments.isEmpty()) {
                sender.sendMessage(Component.text("There are no arguments to list", NamedTextColor.YELLOW));
                return;
            }

            List<String> argList = new ArrayList<>(availableArguments);
            sender.sendMessage(Component.text("Arguments for command " + name + ":", NamedTextColor.YELLOW));
            for (int i = 0; i < argList.size(); i++) {
                String argPath = argList.get(i);
                String type = argsSection.getString(argPath + ".type", "STRING");
                sender.sendMessage(Component.text(i + " - " + argList.get(i) + " (" + type.toUpperCase() + ")"));

                if (type.equalsIgnoreCase("string")) {
                    List<String> options = argsSection.getStringList(argPath + ".options");
                    if (!options.isEmpty()) {
                        sender.sendMessage(Component.text("  Options:", NamedTextColor.YELLOW));
                        for (String stringOption : options) {
                            sender.sendMessage(Component.text("    - " + stringOption));
                        }
                    }
                }
            }
        }

        // /cm argument <cmdName> remove <argName>
        if (option.equals("remove")) {
            String argName = args[3];
            if (!availableArguments.contains(argName)) {
                sender.sendMessage(Component.text("This argument does not exist. Use /cm argument <name> list to view options", NamedTextColor.RED));
                return;
            }

            config.set(argsPath + "." + argName, null);
            feature.saveAndReload();
            sender.sendMessage(Component.text("Argument '" + argName + "' has been removed from command '" + name + "'", NamedTextColor.GREEN));
        }
    }

    public static List<String> tabComplete(String[] args, CommandFeature feature) {
        if (args.length == 2) {
            return filter(args[1], feature.getCommandKeys());
        }

        if (args.length == 3) {
            return filter(args[2], List.of("add", "list", "remove"));
        }

        if (args[2].equalsIgnoreCase("add")) {
            if (args.length == 4) return List.of("<argName>");
            else if (args.length == 5) return filter(args[4], List.of("STRING", "PLAYER", "INT", "FLOAT"));
            else if (args[4].equalsIgnoreCase("string")) return List.of("[options]");
            else if (args[4].equalsIgnoreCase("player") && args.length == 6) return filter(args[5], List.of("true", "false"));
            else return List.of();
        }

        if (args[2].equalsIgnoreCase("remove") && args.length == 4) {
            ConfigurationSection argsSection = feature.getConfig().getConfigurationSection(CommandFeature.commandPath(args[1].toLowerCase()) + ".args");
            if (argsSection != null) return filter(args[3], argsSection.getKeys(false));
        }
        return List.of();
    }
}
