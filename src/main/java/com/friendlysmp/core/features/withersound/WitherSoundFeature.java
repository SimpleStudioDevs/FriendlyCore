package com.friendlysmp.core.features.withersound;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.placeholder.FriendlyCoreExpansion;
import com.friendlysmp.core.placeholder.PlaceholderProvider;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import org.bukkit.command.PluginCommand;

public final class WitherSoundFeature extends Feature implements PlaceholderProvider {

    private final PlayerSettingsStore store;
    private final FeatureConfig config;

    private PacketListenerAbstract registered; // only one listener now

    public WitherSoundFeature(FriendlyCorePlugin plugin, PlayerSettingsStore store) {
        super(plugin);
        this.store = store;
        this.config = new FeatureConfig(plugin, "FeatureConfigs/withersound.yml");
    }

    @Override public String id() { return "wither-sound"; }

    @Override
    public void enable() {
        config.load();
        PluginCommand cmd = plugin.getCommand("withersound");
        if (cmd != null) {
            WitherSoundCommand exec = new WitherSoundCommand(store);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }
        registerListener(new WitherSoundJoinListener(store));
        boolean debug = config.get().getBoolean("debug", false);

        registered = new WitherEffectPacketListener(store, debug);
        PacketEvents.getAPI().getEventManager().registerListener(registered);
    }

    @Override
    public void registerPlaceholders(FriendlyCoreExpansion expansion) {
        expansion.registerHandler("withersound", (player, args) -> {
            boolean muted = store.isWitherDeathMuted(player.getUniqueId());
            if (args.length == 0) return muted ? "OFF" : "ON";
            if (args[0].equalsIgnoreCase("colored")) return muted ? "§cOFF" : "§aON";
            return muted ? "OFF" : "ON";
        });
    }

    @Override
    public void disable() {
        if (registered != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(registered);
            registered = null;
        }
    }

    @Override
    public void reload() { config.load(); }
}