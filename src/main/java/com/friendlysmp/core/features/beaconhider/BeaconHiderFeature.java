package com.friendlysmp.core.features.beaconhider;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;



public final class BeaconHiderFeature implements Feature {

    private final FriendlyCorePlugin plugin;

    private ScheduledTask task;

    public BeaconHiderFeature(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "beacon-hider";
    }

    @Override
    public void enable() {
        int intervalTicks = 80;
        task = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, t -> tick(), intervalTicks, intervalTicks);
    }

    @Override
    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void reload() {
    }

    private void tick() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities(b -> b.getType() == Material.BEACON, false)) {
                    if (state instanceof Beacon beacon) {
                        applyHiddenBeacon(beacon);
                    }
                }
            }
        }
    }

    private void applyHiddenBeacon(Beacon beacon) {
        Block block = beacon.getBlock();
        if (block.getRelative(BlockFace.UP).getType() != Material.TINTED_GLASS) return;

        int tier = beacon.getTier();
        if (tier <= 0) return;

        PotionEffect primary = beacon.getPrimaryEffect();
        if (primary == null) return;
        PotionEffect secondary = beacon.getSecondaryEffect();

        int duration = (9 + tier * 2) * 20; 

        double range = beacon.getEffectRange();
        if (range <= 0) range = tier * 10 + 10;

        World world = block.getWorld();
        BoundingBox box = BoundingBox.of(block).expand(range);
        box.resize(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), world.getMaxHeight(), box.getMaxZ());

        PotionEffect primaryEffect = new PotionEffect(
                primary.getType(), duration, primary.getAmplifier(), true, true, true);
        PotionEffect secondaryEffect = secondary == null ? null : new PotionEffect(
                secondary.getType(), duration, secondary.getAmplifier(), true, true, true);

        for (Entity entity : world.getNearbyEntities(box, e -> e instanceof Player)) {
            Player player = (Player) entity;
            player.addPotionEffect(primaryEffect);
            if (secondaryEffect != null) player.addPotionEffect(secondaryEffect);
        }
    }
}
