package com.friendlysmp.core.features.creeperdisable;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class CreeperDisableFeature extends Feature implements Listener {
    public CreeperDisableFeature(FriendlyCorePlugin plugin) {
        super(plugin);
    }

    @Override
    public String id() {
        return "creeperdisable";
    }

    @Override
    public void enable() {
        registerListener(this);
    }

    @Override
    public void disable() {
        unregisterListener(this);
    }

    @Override
    public void reload() {

    }

    @EventHandler
    public void onCreeperExplode(EntityExplodeEvent event) {
        if ((event.getEntity() instanceof Creeper) && event.getEntity().getWorld().getName().equals("world")) {
            event.blockList().clear();
        }
    }
}
