package com.friendlysmp.core.features.bottlexp;

import com.friendlysmp.core.FriendlyCorePlugin;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class BottleListener implements Listener {

    private final FriendlyCorePlugin plugin;
    private final NamespacedKey xpPointsKey;
    private final NamespacedKey oldPointsKey = new NamespacedKey("stowablexp", "stored_xp_points");

    public BottleListener(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
        this.xpPointsKey = new NamespacedKey(plugin, "stored_xp_points");
    }

    @EventHandler
    public void onPlayerUseXPBottle(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // We want to handle BOTH hands; Bukkit fires separate events per hand.
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        ItemStack item = getItemInHand(player, hand);
        if (item == null || item.getType() == Material.AIR) return;


        if (item.getType() != Material.EXPERIENCE_BOTTLE && item.getType() != Material.KNOWLEDGE_BOOK) return;
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(oldPointsKey, PersistentDataType.DOUBLE)) { // migrate old points keys
            double xp = pdc.get(oldPointsKey, PersistentDataType.DOUBLE);

            pdc.set(xpPointsKey, PersistentDataType.DOUBLE, xp);
            pdc.remove(oldPointsKey);

            item.setItemMeta(meta);

        }


        boolean isSXP =
                item.getItemMeta().getPersistentDataContainer().has(this.xpPointsKey, PersistentDataType.DOUBLE);
        if (!isSXP) return;

        // Check if redeeming is blocked in this world
        if (isRedeemBlockedInWorld(player.getWorld().getName())) {
            event.setCancelled(true); // stop vanilla throw/use
            player.sendMessage(ChatColor.RED + "You cannot redeem XP items in this world.");
            return;
        }

        // It IS one of ours – take over the interaction completely
        event.setCancelled(true);

        double amountToAdd = 0.0D;
        if (meta != null) {
            amountToAdd = meta.getPersistentDataContainer().get(this.xpPointsKey, PersistentDataType.DOUBLE);
        }

        if (amountToAdd <= 0.0D) {
            player.sendMessage("XP item is empty!");
            return;
        }

        // Consume from the correct hand
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            setItemInHand(player, hand, item);
        } else {
            setItemInHand(player, hand, null); // clear that hand
        }

        boolean debug = plugin.getConfig().getBoolean("debug-mode", false);

        addPointsWithMending(player, amountToAdd);

        if (debug)
            plugin.getLogger().info("(DEBUG) " + player.getName() + " redeemed " + amountToAdd + " points from " + ((item.getType() == Material.KNOWLEDGE_BOOK) ? "book" : "bottle") + " (" + hand + ")");
        player.sendMessage("You have redeemed " + formatXP(amountToAdd) + " XP point" + ((amountToAdd == 1.0D) ? "" : "s") + "!");


        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.2F, 0.2F);
        player.updateInventory();
    }


    private void addPoints(Player player, double points) {
        double currentXP = getTotalExperience(player);
        setTotalExperience(player, currentXP + points);
    }

    private void addPointsWithMending(Player player, double xp) {
        double remaining = repairWithMending(player, xp);
        if (remaining > 0.0D) addPoints(player, remaining);
    }

    private double repairWithMending(Player player, double xp) {
        boolean debug = plugin.getConfig().getBoolean("features.bottle-xp.debug-mode", false);
        List<ItemStack> mendable = new ArrayList<>();
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.containsEnchantment(Enchantment.MENDING)) mendable.add(armor);
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.containsEnchantment(Enchantment.MENDING)) mendable.add(mainHand);
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.containsEnchantment(Enchantment.MENDING)) mendable.add(offHand);

        if (mendable.isEmpty()) return xp;

        double xpPerItem = xp / mendable.size();
        double leftover = 0.0D;
        for (ItemStack item : mendable) {
            double after = repairItem(item, xpPerItem);
            leftover += after;
        }

        if (debug)
            plugin.getLogger().info("(DEBUG) Distributed " + xp + " XP to " + mendable.size() + " mending items. Leftover: " + leftover);
        return leftover;
    }

    private double repairItem(ItemStack item, double xp) {
        if (item == null) return xp;
        if (!item.containsEnchantment(Enchantment.MENDING)) return xp;

        short maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) return xp;

        short currentDamage = item.getDurability();
        if (currentDamage <= 0) return xp;

        int repairAmount = Math.min(currentDamage, (int) (xp * 2.0D));
        short newDamage = (short) (currentDamage - repairAmount);
        if (newDamage < 0) newDamage = 0;

        item.setDurability(newDamage);
        double xpUsed = repairAmount / 2.0D;

        if (plugin.getConfig().getBoolean("features.bottle-xp.debug-mode", false))
            plugin.getLogger().info(
                    "(DEBUG) Repairing item " + String.valueOf(item.getType())
                            + ": damage " + currentDamage + " -> " + newDamage
                            + ", XP used: " + xpUsed + ", XP left: " + (xp - xpUsed)
            );
        return xp - xpUsed;
    }

    private String formatXP(double value) {
        return (value % 1.0D == 0.0D) ? String.valueOf((int) value) : String.format("%.2f", value);
    }

    private double getTotalExperience(Player player) {
        double total = 0.0D;
        for (int i = 0; i < player.getLevel(); i++) total += getExpAtLevel(i);
        total += (player.getExp() * getExpAtLevel(player.getLevel()));
        return total;
    }

    private void setTotalExperience(Player player, double amount) {
        player.setExp(0.0F);
        player.setLevel(0);
        player.setTotalExperience(0);
        double remaining = amount;
        int level = 0;
        while (remaining > getExpAtLevel(level)) {
            remaining -= getExpAtLevel(level);
            level++;
        }
        player.setLevel(level);
        player.setExp((float) (remaining / getExpAtLevel(level)));
    }

    private int getExpAtLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    private ItemStack getItemInHand(Player p, EquipmentSlot hand) {
        return (hand == EquipmentSlot.HAND) ? p.getInventory().getItemInMainHand()
                : p.getInventory().getItemInOffHand();
    }

    private void setItemInHand(Player p, EquipmentSlot hand, ItemStack stack) {
        if (hand == EquipmentSlot.HAND) p.getInventory().setItemInMainHand(stack);
        else p.getInventory().setItemInOffHand(stack);
    }

    public boolean isRedeemBlockedInWorld(String worldname) {
        if (worldname == null) return false;

        List<String> blocked = plugin.getConfig().getStringList( "bottle-xp.blocked-redeem-worlds");
        if (blocked == null || blocked.isEmpty()) return false;

        for (String w : blocked) {
            if (w != null && w.equalsIgnoreCase(worldname)) return true;
        }

        return false;
    }
}
