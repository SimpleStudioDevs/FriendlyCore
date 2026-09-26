package com.friendlysmp.core.features.withersound;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class WitherSoundCommand implements CommandExecutor, TabCompleter {
    private final PlayerSettingsStore store;

    public WitherSoundCommand(PlayerSettingsStore store) {
        this.store = store;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        store.ensureLoadedAsync(player.getUniqueId());

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            boolean nowMuted = store.toggleWitherDeathMuted(player.getUniqueId());
            player.sendMessage("Boss global sounds: " + (nowMuted ? "§cMUTED" : "§aON"));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            store.setWitherDeathMuted(player.getUniqueId(), false);
            player.sendMessage("Boss global sounds: §aON");
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            store.setWitherDeathMuted(player.getUniqueId(), true);
            player.sendMessage("Boos global sounds: §cMUTED");
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            boolean muted = store.isWitherDeathMuted(player.getUniqueId());
            player.sendMessage("Boss global sounds: " + (muted ? "§cMUTED" : "§aON"));
            return true;
        }

        player.sendMessage("Usage: /withersound [toggle|on|off|status]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("toggle", "on", "off", "status");
        return List.of();
    }
}