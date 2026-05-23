package com.friendlysmp.core.features.bottlexp;

import com.friendlysmp.core.FriendlyCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class BottleXPCommand implements CommandExecutor, TabCompleter {
    private final NamespacedKey xpPointsKey;
    private final FriendlyCorePlugin plugin;

    public BottleXPCommand(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
        this.xpPointsKey = new NamespacedKey(plugin, "stored_xp_points");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players may use this command!", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        final boolean debug = plugin.getConfig().getBoolean("bottle-xp.debug");

        if (args.length == 0) {
            player.sendMessage(Component.text("/" + label + " <amount|all> [bottles]", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(Component.text("BottleXP Config has been reloaded!", NamedTextColor.GREEN));
            return true;
        }

        final boolean withdrawAll = args[0].equalsIgnoreCase("all");

        // Total XP the player currently has (points, not levels)
        final double currentXP = getTotalExperience(player);
        if (currentXP <= 0.0D) {
            player.sendMessage(Component.text("You don't have any xp to store.", NamedTextColor.RED));
            return true;
        }

        final int maxPoints = plugin.getConfig().getInt(
                "bottle-xp.max-xp-points-bottle", 100000
        );


        // --------------------------
        // /bottlexp all
        // --------------------------

        if (withdrawAll) {
            if (maxPoints > 0 && currentXP > maxPoints) {
                int fullBottles = (int) (currentXP / maxPoints);
                double remainder = currentXP % maxPoints;
                int bottlesNeeded = fullBottles + (remainder > 0.0D ? 1 : 0);

                int empty = countEmptySlots(player);
                if (empty < bottlesNeeded) {
                    player.sendMessage(
                            ChatColor.RED + "Your inventory is full! You need at least "
                                    + ChatColor.YELLOW + bottlesNeeded
                                    + ChatColor.RED + " free slot"
                                    + (bottlesNeeded == 1 ? "" : "s")
                                    + " to store all of your XP."
                    );
                    return true;
                }

                setTotalExperience(player, 0.0D);

                int bottleCount = 0;
                for (int i = 0; i < fullBottles; i++) {
                    giveBottle(player, maxPoints, plugin, debug, false);
                    bottleCount++;
                }
                if (remainder > 0.0D) {
                    giveBottle(player, remainder, plugin, debug, false);
                    bottleCount++;
                }

                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.0F, 0.7F);
                player.sendMessage(
                        ChatColor.GOLD + "Stored " + ChatColor.WHITE + formatXP(currentXP)
                                + " XP Points " + ChatColor.GOLD + "into "
                                + ChatColor.YELLOW + bottleCount
                                + ChatColor.GOLD + " bottle" + (bottleCount == 1 ? "" : "s") + "."
                );
                return true;
            } else {
                if (countEmptySlots(player) < 1) {
                    player.sendMessage(
                            ChatColor.RED + "Your inventory is full! You need at least "
                                    + ChatColor.YELLOW + "1"
                                    + ChatColor.RED + " free slot to store your XP."
                    );
                    return true;
                }

                double amount = currentXP;
                setTotalExperience(player, 0.0D);
                giveBottle(player, amount, plugin, debug, true);
                return true;
            }


        }
        // --------------------------
        // /bottlexp <amount> <#bottles?>
        // --------------------------

        final double amountPerBottle;
        try {
            amountPerBottle = Double.parseDouble(args[0]);
            if (amountPerBottle <= 0.0D) {
                player.sendMessage(ChatColor.RED + "Amount must be greater than 0.");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number: " + ChatColor.YELLOW + args[0]);
            return true;
        }

        int bottlesCount = 1;
        if (args.length >= 2) {
            try {
                bottlesCount = Integer.parseInt(args[1]);
                if (bottlesCount <= 0) {
                    player.sendMessage(ChatColor.RED + "Number of bottles must be greater than 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid bottle count: " + ChatColor.YELLOW + args[1]);
                return true;
            }
        }

        if (maxPoints > 0 && amountPerBottle > maxPoints) {
            player.sendMessage(ChatColor.RED + "You cannot store more than "
                    + ChatColor.YELLOW + maxPoints + ChatColor.RED + " points in one bottle!");
            return true;
        }

        double totalRequired = amountPerBottle * bottlesCount;

        if (currentXP < totalRequired) {
            player.sendMessage(ChatColor.RED + "You don't have enough XP points! You need "
                    + ChatColor.YELLOW + formatXP(totalRequired) + ChatColor.RED + " XP.");
            return true;
        }

        if (!canFitBottles(player, amountPerBottle, bottlesCount)) {
            player.sendMessage(
                    ChatColor.RED + "Your inventory does not have enough space for that many XP bottles."
            );
            return true;
        }

        setTotalExperience(player, currentXP - totalRequired);

        for (int i = 0; i < bottlesCount; i++) {
            giveBottle(player, amountPerBottle, plugin, debug, false);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.0F, 0.7F);
        player.sendMessage(
                ChatColor.GOLD + "Stored " + ChatColor.WHITE + formatXP(totalRequired)
                        + " XP Points " + ChatColor.GOLD + "into "
                        + ChatColor.YELLOW + bottlesCount
                        + ChatColor.GOLD + " bottle" + (bottlesCount == 1 ? "" : "s") + "."
        );
        return true;


    }

    private void giveBottle(Player player, double amount, Plugin plugin, boolean debug, boolean sendMessage) {
        final Material itemType = Material.EXPERIENCE_BOTTLE;
        final String primary = ChatColor.GOLD.toString();
        final String accent = ChatColor.GRAY.toString();
        final String reset = ChatColor.RESET.toString();

        ItemStack item = new ItemStack(itemType, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String itemName = primary + "XP Bottle" + reset
                    + accent + " (Points " + formatXP(amount) + ")";
            meta.setDisplayName(itemName);
            meta.getPersistentDataContainer().set(this.xpPointsKey, PersistentDataType.DOUBLE, amount);
            item.setItemMeta(meta);
        }

        if (debug) {
            plugin.getLogger().info("(DEBUG) Created XP Bottle with " + amount + " points for " + player.getName());
        }

        player.getInventory().addItem(item);

        if (sendMessage) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.0F, 0.7F);
            player.sendMessage(
                    primary + "Stored " + ChatColor.WHITE + formatXP(amount)
                            + " XP Point" + (Double.compare(amount, 1.0D) == 0 ? "" : "s")
                            + reset
            );
        }
    }

    private boolean canFitBottles(Player player, double amountPerBottle, int bottlesCount) {
        PlayerInventory inv = player.getInventory();

        final Material itemType = Material.EXPERIENCE_BOTTLE;
        final String primary = ChatColor.GOLD.toString();
        final String accent = ChatColor.GRAY.toString();
        final String reset = ChatColor.RESET.toString();

        ItemStack prototype = new ItemStack(itemType, 1);
        ItemMeta meta = prototype.getItemMeta();
        if (meta != null) {
            String itemName = primary + "XP Bottle" + reset
                    + accent + " (Points " + formatXP(amountPerBottle) + ")";
            meta.setDisplayName(itemName);
            meta.getPersistentDataContainer().set(this.xpPointsKey, PersistentDataType.DOUBLE, amountPerBottle);
            prototype.setItemMeta(meta);
        }

        int maxStack = prototype.getMaxStackSize();
        int remaining = bottlesCount;

        ItemStack[] contents = inv.getStorageContents();

        for (ItemStack stack : contents) {
            if (remaining <= 0) break;
            if (stack == null || stack.getType() == Material.AIR) continue;
            if (stack.isSimilar(prototype)) {
                int canAdd = maxStack - stack.getAmount();
                if (canAdd > 0) {
                    remaining -= Math.min(canAdd, remaining);
                }
            }
        }

        if (remaining <= 0) {
            return true;
        }

        for (ItemStack stack : contents) {
            if (remaining <= 0) break;
            if (stack == null || stack.getType() == Material.AIR) {
                int canAdd = maxStack;
                remaining -= Math.min(canAdd, remaining);
            }
        }

        return remaining <= 0;
    }

    private int countEmptySlots(Player player) {
        PlayerInventory inv = player.getInventory();
        int empty = 0;
        ItemStack[] contents = inv.getStorageContents();
        for (ItemStack stack : contents) {
            if (stack == null || stack.getType() == Material.AIR) {
                empty++;
            }
        }
        return empty;
    }

    private String formatXP(double value) {
        return (value % 1.0D == 0.0D)
                ? String.valueOf((int) value)
                : String.format(Locale.ROOT, "%.2f", value);
    }

    private double getTotalExperience(Player player) {
        double total = 0.0D;
        for (int i = 0; i < player.getLevel(); i++) {
            total += getExpAtLevel(i);
        }
        total += (player.getExp() * getExpAtLevel(player.getLevel()));
        return total;
    }

    private void setTotalExperience(Player player, double amount) {
        player.setExp(0.0F);
        player.setLevel(0);
        player.setTotalExperience(0);

        double remaining = Math.max(0.0D, amount);
        int level = 0;
        while (remaining >= getExpAtLevel(level)) {
            remaining -= getExpAtLevel(level);
            level++;
        }
        player.setLevel(level);
        player.setExp(getExpAtLevel(level) > 0 ? (float) (remaining / getExpAtLevel(level)) : 0.0F);
    }

    private int getExpAtLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return List.of("all");
        }

        return List.of();
    }
}
