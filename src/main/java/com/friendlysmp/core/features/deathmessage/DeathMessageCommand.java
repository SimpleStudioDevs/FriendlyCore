package com.friendlysmp.core.features.deathmessage;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class DeathMessageCommand implements CommandExecutor, TabCompleter {
    private final PlayerSettingsStore store;

    public DeathMessageCommand(PlayerSettingsStore store) {
        this.store = store;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        store.ensureLoadedAsync(uuid);

        String action = args.length == 0 ? "toggle" : args[0].toLowerCase();
        switch (action) {
            case "toggle" -> sendStatus(player, store.toggleDeathMessagesHidden(uuid));
            case "on" -> sendStatus(player, store.setDeathMessagesHidden(uuid, false));
            case "off" -> sendStatus(player, store.setDeathMessagesHidden(uuid, true));
            case "status" -> sendStatus(player, store.isDeathMessagesHidden(uuid));
            default -> player.sendMessage("Usage: /deathmessages [toggle|on|off|status]");
        }
        return true;
    }

    private void sendStatus(Player player, boolean hidden) {
        player.sendMessage("Death messages: " + (hidden ? "§cHIDDEN" : "§aVISIBLE"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("toggle", "on", "off", "status");
        return List.of();
    }
}
