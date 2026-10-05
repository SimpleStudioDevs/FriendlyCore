package com.friendlysmp.core.features.commandmaker.commands;

import com.friendlysmp.core.features.commandmaker.CommandFeature;
import com.friendlysmp.core.features.commandmaker.commands.subcommands.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.friendlysmp.core.features.commandmaker.util.FilterUtil.filter;

public class AdminCommand implements CommandExecutor, TabCompleter {
    public static final String PERMISSION = "friendlycore.admin";

    private final CommandFeature feature;

    public AdminCommand(CommandFeature feature) {
        this.feature = feature;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You do not have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("CommandMaker", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .appendNewline()
                    .append(Component.text("For a list of commands, use /cm help", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false)));
            return true;
        }

        // Defer each command task to their own methods
        switch (args[0].toLowerCase()) {
            case "reload" -> ReloadSubCommand.runCommand(sender, feature);
            case "create" -> CreateSubCommand.runCommand(sender, args, feature);
            case "delete" -> DeleteSubCommand.runCommand(sender, args, feature);
            case "edit" -> EditSubCommand.runCommand(sender, args, feature);
            case "enable" -> EnableSubCommand.runCommand(sender, args, feature);
            case "disable" -> DisableSubCommand.runCommand(sender, args, feature);
            case "argument" -> ArgumentSubCommand.runCommand(sender, args, feature);
            case "help" -> HelpSubCommand.runCommand(sender);
            case "list" -> ListSubCommand.runCommand(sender, feature);
            default -> sender.sendMessage(Component.text("Unknown command! Use /cm help", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(PERMISSION)) return List.of();

        if (args.length == 1) {
            return filter(args[0], List.of("disable", "argument", "help", "reload", "enable", "edit", "list", "delete", "create"));
        }

        switch (args[0].toLowerCase()) {
            case "disable" -> {
                if (args.length == 2) return filter(args[1], feature.getConfig().getStringList(CommandFeature.ENABLED_COMMANDS));
            }
            case "enable", "delete" -> {
                if (args.length == 2) return filter(args[1], feature.getCommandKeys());
            }
            case "argument" -> { return ArgumentSubCommand.tabComplete(args, feature); }
            case "edit" -> { return EditSubCommand.tabComplete(args, feature); }
        }
        return List.of();
    }
}
