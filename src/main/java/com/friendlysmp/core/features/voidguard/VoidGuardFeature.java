package com.friendlysmp.core.features.voidguard;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class VoidGuardFeature extends Feature {
    private final FeatureConfig config;
    public Set<String> guardedWorlds = new HashSet<>();

    public VoidGuardFeature(FriendlyCorePlugin plugin) {
        super(plugin);
        this.config = new FeatureConfig(plugin, "FeatureConfigs/voidguard.yml");
    }

    @Override
    public String id() {
        return "void-guard";
    }

    @Override
    public void enable() {
        config.load();
        registerListener(new VoidGuardListener(this));
        reloadGuardedWorlds();

        if (plugin.getCommand("voidguard") != null) {
            VoidGuardCommand command = new VoidGuardCommand(this);
            plugin.getCommand("voidguard").setExecutor(command);
            plugin.getCommand("voidguard").setTabCompleter(command);
        }


    }

    public void reloadGuardedWorlds() {
        List<String> list = config.get().getStringList("guarded-worlds");
        guardedWorlds.clear();
        guardedWorlds.addAll(list);
    }

    @Override
    public void disable() {

    }

    @Override
    public void reload() {
        config.load();
        reloadGuardedWorlds();
    }

    public void saveVoidLocation(World world, Location loc) {
        String path = "locations." + world.getName();
        Configuration yaml = config.get();

        yaml.set(path + ".world", world.getName());
        yaml.set(path + ".x", loc.getX());
        yaml.set(path + ".y", loc.getY());
        yaml.set(path + ".z", loc.getZ());
        yaml.set(path + ".yaw", loc.getYaw());
        yaml.set(path + ".pitch", loc.getPitch());

        if (!guardedWorlds.contains(world.getName())) {
            guardedWorlds.add(world.getName());
            yaml.set("guarded-worlds", new ArrayList<>(guardedWorlds));
        }

        config.save();

    }

    public Location getVoidLocation(World world) {
        String path = "locations." + world.getName();
        ConfigurationSection section = config.get().getConfigurationSection(path);

        if (section == null) {
            return world.getSpawnLocation();
        }

        double x = section.getDouble( "x", world.getSpawnLocation().getX() );
        double y = section.getDouble( "y", world.getSpawnLocation().getY() );
        double z = section.getDouble( "z", world.getSpawnLocation().getZ() );
        float yaw =  (float) section.getDouble( "yaw", world.getSpawnLocation().getYaw() );
        float pitch  = (float) section.getDouble( "pitch", world.getSpawnLocation().getPitch() );

        return new Location(world, x, y, z, yaw, pitch);
    }

    public boolean isGuardedWorld(World world) { return guardedWorlds.contains(world.getName()); }

    public Logger  getLogger() { return plugin.getLogger(); }

}
