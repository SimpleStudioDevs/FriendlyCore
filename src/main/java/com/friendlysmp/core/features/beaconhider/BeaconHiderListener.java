package com.friendlysmp.core.features.beaconhider;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class BeaconHiderListener implements Listener {

    private final BeaconHiderFeature feature;

    public BeaconHiderListener(BeaconHiderFeature feature) {
        this.feature = feature;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        feature.registerBeaconsInChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        feature.unregisterBeaconsInChunk(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() == Material.BEACON) {
            feature.registerBeacon(block);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.BEACON) {
            feature.unregisterBeacon(event.getBlock());
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (block.getType() == Material.BEACON) {
                feature.unregisterBeacon(block);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (block.getType() == Material.BEACON) {
                feature.unregisterBeacon(block);
            }
        }
    }
}
