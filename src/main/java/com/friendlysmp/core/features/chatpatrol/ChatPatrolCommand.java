package com.friendlysmp.core.features.chatpatrol;

import com.friendlysmp.core.features.chatpatrol.managers.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ChatPatrolCommand implements CommandExecutor {
    private final ChatPatrolFeature plugin;

    public  ChatPatrolCommand(ChatPatrolFeature plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        ConfigManager configManager = plugin.getConfigManager();

        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "ChatPatrol Plugin is active!");
            sender.sendMessage(ChatColor.GOLD + "Word Filter: "
                    + (configManager.isWordFilterEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
            return true;
        }

        if (args[0].equals("reload")) {
            if (!sender.hasPermission("friendlycore.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            plugin.reload();
            sender.sendMessage(ChatColor.GREEN + "ChatPatrol config reloaded!");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " [reload]");
        return true;

    }
}
