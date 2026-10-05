package com.friendlysmp.core.features.commandmaker.commandcreation;

import com.friendlysmp.core.features.commandmaker.CommandFeature;
import com.friendlysmp.core.features.commandmaker.arguments.ArgVerification;
import com.friendlysmp.core.features.commandmaker.arguments.ArgsDefinition;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static com.friendlysmp.core.features.commandmaker.util.FilterUtil.filter;

public class CustomCommand extends Command {
    private final List<String> actions;
    private final CommandFeature feature;
    private final String permission;
    private final List<ArgsDefinition> argDefs;
    private final ExecuteActions executeActions;

    public CustomCommand(String name, List<String> aliases, List<String> actions, CommandFeature feature, String permission, List<ArgsDefinition> argDefs) {
        super(name);
        this.feature = feature;
        setAliases(aliases);
        this.actions = actions;
        this.permission = permission;
        this.argDefs = argDefs;
        this.executeActions = new ExecuteActions(feature.getPlugin());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String @NotNull [] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("You do not have permission to run this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < argDefs.size()) {
            sender.sendMessage(Component.text("Not enough arguments!", NamedTextColor.RED));
            return true;
        }

        // A trailing STRING argument takes the rest of the input
        if (!argDefs.isEmpty() && argDefs.getLast().type().equalsIgnoreCase("STRING") && args.length > argDefs.size()) {
            String[] condensed = Arrays.copyOf(args, argDefs.size());
            condensed[argDefs.size() - 1] = String.join(" ", Arrays.copyOfRange(args, argDefs.size() - 1, args.length));
            args = condensed;
        }

        Player placeholderTarget = (sender instanceof Player p) ? p : null;

        // Verify args
        for (int i = 0; i < argDefs.size(); i++) {
            ArgsDefinition def = argDefs.get(i);
            String arg = args[i];
            boolean valid = switch (def.type().toUpperCase()) {
                case "INT" -> ArgVerification.isInteger(arg);
                case "FLOAT" -> ArgVerification.isFloat(arg);
                case "STRING" -> ArgVerification.validString(arg, def.options());
                case "PLAYER" -> ArgVerification.validPlayer(arg);
                default -> true;
            };

            // Send messages if invalid args
            if (!valid) {
                switch (def.type().toUpperCase()) {
                    case "INT" -> sender.sendMessage(Component.text("Argument '" + def.name() + "' must be an integer", NamedTextColor.RED));
                    case "FLOAT" -> sender.sendMessage(Component.text("Argument '" + def.name() + "' must be a number", NamedTextColor.RED));
                    case "STRING" -> sender.sendMessage(Component.text("Invalid string argument! Options: " + def.options(), NamedTextColor.RED));
                    case "PLAYER" -> sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                }
                return true;
            }

            // Set papi target to player argument if set in config
            if (def.type().equalsIgnoreCase("PLAYER") && def.papi()) {
                placeholderTarget = Bukkit.getPlayerExact(arg);
            }
        }

        for (String string : actions) {
            int colonIndex = string.indexOf(":");
            String prefix = string.substring(0, colonIndex + 1);
            String action = parsePlaceholders(string.substring(colonIndex + 1).trim(), sender, args, placeholderTarget);

            // Send action out to methods
            switch (prefix) {
                case "MESSAGE:" -> executeActions.sendMessage(sender, action, false);
                case "BROADCAST:" -> executeActions.sendMessage(sender, action, true);
                case "CONSOLE:" -> executeActions.runCommand(sender, action, true);
                case "PLAYER:" -> executeActions.runCommand(sender, action, false);
                case "SOUND:" -> executeActions.playSound(sender, action, false);
                case "SOUNDALL:" -> executeActions.playSound(sender, action, true);
                default -> feature.getPlugin().getLogger().warning("Incorrectly formatted action! Failed to parse: " + string);
            }
        }

        return true;
    }

    private String parsePlaceholders(String action, CommandSender sender, String[] args, Player placeholderTarget) {
        // Args
        for (int i = 0; i < argDefs.size() && i < args.length; i++) {
            action = action.replace("{" + argDefs.get(i).name() + "}", args[i]);
        }

        // Server
        action = action.replace("{onlineplayers}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        action = action.replace("{maxplayers}", String.valueOf(Bukkit.getMaxPlayers()));

        String senderName = (sender instanceof Player p) ? p.getName() : "Console";
        action = action.replace("{player}", senderName);
        action = action.replace("{sender}", senderName);

        // Placeholders for player argument
        if (placeholderTarget != null) {
            action = action.replace("{target}", placeholderTarget.getName());
            action = action.replace("{x}", String.format("%.2f", placeholderTarget.getX()));
            action = action.replace("{y}", String.format("%.2f", placeholderTarget.getY()));
            action = action.replace("{z}", String.format("%.2f", placeholderTarget.getZ()));
            action = action.replace("{world}", placeholderTarget.getWorld().getName());
            action = action.replace("{displayname}", PlainTextComponentSerializer.plainText().serialize(placeholderTarget.displayName()));
        }

        if (feature.isPapiEnabled()) {
            action = PlaceholderAPI.setPlaceholders(placeholderTarget, action);
        }
        return action;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String @NotNull [] args) {
        int index = args.length - 1;

        if (index < argDefs.size()) {
            ArgsDefinition def = argDefs.get(index);
            switch (def.type().toUpperCase()) {
                case "PLAYER" -> {
                    return filter(args[index], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                }
                case "INT" -> { return List.of("<whole number>"); }
                case "FLOAT" -> { return List.of("<number>"); }
                case "STRING" -> {
                    if (def.options().isEmpty()) return List.of("<" + def.name() + ">");
                    return filter(args[index], def.options());
                }
            }
        }

        return List.of();
    }
}
