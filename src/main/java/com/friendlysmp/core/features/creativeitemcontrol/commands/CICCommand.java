package com.friendlysmp.core.features.creativeitemcontrol.commands;

import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ConfigManager;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ExcludedItemManager;
import com.friendlysmp.core.features.creativeitemcontrol.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CICCommand implements CommandExecutor, TabCompleter {
    private final CreativeFeature feature;
    public CICCommand(CreativeFeature feature) {
        this.feature = feature;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        boolean isAdmin = sender.hasPermission("cic.admin");
        MessageManager mm = feature.getMessageManager();
        if (!isAdmin && !sender.hasPermission("cic.give")) {
            mm.send(sender, ConfigManager.ConfigKeys.CMD_NO_PERM);
            return true;
        }

        String prefix = "<gold>[<green>CIC<gold>] <reset>";
        if (args.length == 0) {
            mm.send(sender, prefix + "&eMaster Enabled&f: " + (feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.MAIN_ENABLED) ? "&atrue" : "&cfalse"));
            mm.send(sender, prefix + "&eAttributes&f: " + (feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.ATTRIBUTES_ENABLED) ? "&atrue" : "&cfalse"));
            mm.send(sender, prefix + "&eEnchantments&f: " + (feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.ENCHANT_ENABLED) ? "&atrue" : "&cfalse"));
            mm.send(sender, prefix + "&ePotions&f: " + (feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.POTIONS_ENABLED) ? "&atrue" : "&cfalse"));
            mm.send(sender, prefix + "&eComponents&f: " + (feature.getConfigManager().getBoolean(ConfigManager.ConfigKeys.COMPONENTS_ENABLED) ? "&atrue" : "&cfalse"));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            ExcludedItemManager eim = feature.getExcludedItemManager();
            switch (args.length) {
                case 1: {
                    mm.send(sender, "&cPlease specify an item to give | /cic give <itemID>");
                    return true;
                }
                case 2: {
                    if (!(sender instanceof Player p)) {
                        mm.send(sender, "&cPlease specify a player | /cic give <itemID> <player>");
                        return true;
                    }

                    if (feature.getConfigManager().isWorldDenied(p.getWorld())) {
                        mm.send(sender, ConfigManager.ConfigKeys.CMD_INVALID_WORLD);
                        return true;
                    }

                    if (!eim.excludedItemExists(args[1])) {
                        mm.send(sender, ConfigManager.ConfigKeys.CMD_GIVE_INVALID, "{id}",  args[1]);
                        return true;
                    }

                    if (!sender.hasPermission("cic.give." + args[1])) {
                        mm.send(sender, ConfigManager.ConfigKeys.CMD_NO_PERM);
                        return true;
                    }

                    if (!isAdmin && eim.isOnGiveCooldown(p.getUniqueId(), args[1])) {
                        mm.send(sender, ConfigManager.ConfigKeys.CMD_ON_COOLDOWN, "{time}", String.valueOf(eim.getGiveCooldownRemaining(p.getUniqueId(), args[1])));
                        return true;
                    }
                    eim.give(p, args[1]);
                    mm.send(sender, ConfigManager.ConfigKeys.CMD_GIVE, "{id}", args[1], "{player}", p.getName());
                    return true;
                }
                case 3: {
                    if (!isAdmin) {
                        mm.send(sender, ConfigManager.ConfigKeys.CMD_GIVE_OTHERS_FAIL);
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        mm.send(sender, ConfigManager.ConfigKeys.CMD_PLAYER_NOT_FOUND, "{player}", args[2]);
                        return true;
                    }

                    if (!eim.excludedItemExists(args[1])) {
                        mm.send(sender, ConfigManager.ConfigKeys.CMD_GIVE_INVALID, "{id}",  args[1]);
                        return true;
                    }

                    if (!eim.canFit(target.getInventory(), eim.getExcludedItem(args[1]))) {
                        mm.send(sender, "&cTarget's inventory is full!");
                        return true;
                    }

                    eim.give(target, args[1]);
                    mm.send(sender, ConfigManager.ConfigKeys.CMD_GIVE, "{id}", args[1], "{player}", target.getName());
                    return true;
                }
            }
        }

        // Exclude

        if (args[0].equalsIgnoreCase("exclude")) {
            if (!isAdmin) {
                mm.send(sender, ConfigManager.ConfigKeys.CMD_NO_PERM);
                return true;
            }
            if (!(sender instanceof Player p)) {
                mm.send(sender, "&cOnly players can use this command.");
                return true;
            }

            if (args.length == 1) {
                mm.send(sender, "&cPlease specify an item ID | /cic exclude <id>");
                return true;
            }

            ItemStack held = p.getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                mm.send(sender, "&cYou must be holding an item!");
                return true;
            }
            ExcludedItemManager eim =  feature.getExcludedItemManager();
            eim.storeExcludedItem(args[1], held);
            mm.send(sender, ConfigManager.ConfigKeys.CMD_EXCLUDE, "{id}", args[1], "{type}", String.valueOf(held.getType()));
            return true;

        }

        // Remove

        if (args[0].equalsIgnoreCase("remove")) {
            if (!isAdmin) {
                mm.send(sender, ConfigManager.ConfigKeys.CMD_NO_PERM);
                return true;
            }
            switch (args.length) {
                case 1: {
                    mm.send(sender, "&cPlease specify an item ID to remove");
                    return true;
                }
                case 2: {
                    ExcludedItemManager eim =  feature.getExcludedItemManager();
                    if (!eim.excludedItemExists(args[1])) {
                        mm.send(sender, "&cThis item does not exist!");
                        return true;
                    }

                    eim.removeExcludedItem(args[1]);
                    mm.send(sender,  ConfigManager.ConfigKeys.CMD_REMOVE, "{id}", args[1]);
                    return true;
                }
            }
        }

        if (args[0].equalsIgnoreCase("reload")) {
            feature.reload();
            mm.send(sender, "&aCIC config reloaded!");
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        boolean isAdmin = sender.hasPermission("cic.admin");
        if (!isAdmin && !sender.hasPermission("cic.give")) return List.of();
        ExcludedItemManager eim = feature.getExcludedItemManager();

        if (!isAdmin) {
            switch (args.length) {
                case 1: { return List.of("give"); }
                case 2: { return List.copyOf(eim.getExcludedItems().keySet()); }
            }
            return List.of();
        }

        switch (args.length) {
            case 1: { return List.of("reload", "exclude", "remove", "give");}
            case 2: {
                List<String> result = new ArrayList<>();
                if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("give")) {
                    for (String id : eim.getExcludedItems().keySet()) {
                        if (sender.hasPermission(eim.givePermissionNode(id))) {
                            result.add(id);
                        }
                    }
                    return result;
                }
                return List.of();
            }
            case 3: {
                if (args[0].equalsIgnoreCase("give")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                }
            }
        }

        return List.of();
    }
}
