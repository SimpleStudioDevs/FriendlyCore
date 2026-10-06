package com.friendlysmp.core.features.dupepatch;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class DupePatchFeature extends Feature implements Listener {
    public DupePatchFeature(FriendlyCorePlugin plugin) {
        super(plugin);
    }

    @Override
    public String id() {
        return "dupepatch";
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        unregisterListener(this);
    }

    @Override
    public void reload() {

    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Villager)) return;
        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        ItemStack offhand =  e.getPlayer().getInventory().getItemInOffHand();
        if (item.getType() == Material.SHEARS || offhand.getType() == Material.SHEARS) e.setCancelled(true);
    }
}
