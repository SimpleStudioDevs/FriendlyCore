package com.friendlysmp.core.features.hidearmor;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class HideArmorCommand implements CommandExecutor, TabCompleter {
    private final PlayerSettingsStore store;
    private final Consumer<Player> refreshViewers;
    private final HideArmorConditions conditions;

    public HideArmorCommand(PlayerSettingsStore store, Consumer<Player> refreshViewers, HideArmorConditions conditions) {
        this.store = store;
        this.refreshViewers = refreshViewers;
        this.conditions = conditions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        store.ensureLoadedAsync(player.getUniqueId());

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            boolean nowHidden = store.toggleArmorHidden(player.getUniqueId());
            refreshViewers.accept(player);
            sendStatus(player, nowHidden);
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            store.setArmorHidden(player.getUniqueId(), true);
            refreshViewers.accept(player);
            sendStatus(player, true);
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            store.setArmorHidden(player.getUniqueId(), false);
            refreshViewers.accept(player);
            sendStatus(player, false);
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            sendStatus(player, store.isArmorHidden(player.getUniqueId()));
            return true;
        }

        player.sendMessage("Usage: /hidearmor [toggle|on|off|status]");
        return true;
    }

    private void sendStatus(Player player, boolean hidden) {
        player.sendMessage("Armor visible to others: " + (hidden ? "§cHIDDEN" : "§aVISIBLE"));
        if (hidden) {
            player.sendMessage("§7Other players will not be able to see your armor");
        }
        if (!hidden) return;

        UUID uuid = player.getUniqueId();
        if (conditions.isInDisabledWorld(uuid)) {
            player.sendMessage("§7Your armor stays visible in this world.");
        } else if (conditions.isInCombat(uuid)) {
            player.sendMessage("§7Your armor will hide once you leave combat.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("toggle", "on", "off", "status");
        return List.of();
    }
}
