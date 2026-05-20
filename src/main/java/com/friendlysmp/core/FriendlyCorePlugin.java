package com.friendlysmp.core;

import com.friendlysmp.core.command.FriendlyCoreCommand;
import com.friendlysmp.core.feature.FeatureManager;
import com.friendlysmp.core.features.chatpatrol.ChatPatrolFeature;
import com.friendlysmp.core.features.commandmaker.CommandFeature;
import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.playerbroadcast.BroadcastFeature;
import com.friendlysmp.core.features.tokens.TokenFeature;
import com.friendlysmp.core.features.withersound.WitherSoundFeature;
import com.friendlysmp.core.placeholder.PlaceholderProvider;
import com.friendlysmp.core.placeholder.PlaceholderRegistrar;
import com.friendlysmp.core.schedulers.Schedulers;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class FriendlyCorePlugin extends JavaPlugin {
    private Schedulers schedulers;
    private PlayerSettingsStore playerSettings;
    private FeatureManager featureManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.schedulers = new Schedulers(this);

        this.playerSettings = new PlayerSettingsStore(this, schedulers);

        var pm = Bukkit.getPluginManager();

        var pe = pm.getPlugin("PacketEvents");
        if (pe == null) pe = pm.getPlugin("packetevents");
        if (pe == null) pe = pm.getPlugin("Packetevents");
        if (pe == null) pe = pm.getPlugin("PACKETEVENTS");

        if (pe == null) {
            getLogger().severe("PacketEvents is required for FriendlyCore. I couldn't find a plugin named PacketEvents/packetevents.");
            getLogger().severe("Loaded plugins: " + String.join(", ",
                    java.util.Arrays.stream(pm.getPlugins()).map(p -> p.getName()).toList()));
            pm.disablePlugin(this);
            return;
        }

        if (!pe.isEnabled()) {
            getLogger().severe("PacketEvents was found (" + pe.getName() + ") but it is NOT enabled. Check startup errors for PacketEvents.");
            pm.disablePlugin(this);
            return;
        }

        getLogger().info("Hooked into PacketEvents: " + pe.getName() + " v" + pe.getDescription().getVersion());

        this.featureManager = new FeatureManager(this);
        featureManager.register(new WitherSoundFeature(this, playerSettings));
        featureManager.register(new TokenFeature(this, playerSettings));
        featureManager.register(new CreativeFeature(this));
        featureManager.register(new CommandFeature(this));
        featureManager.register(new BroadcastFeature(this));
        featureManager.register(new ChatPatrolFeature(this, schedulers));

        var cmd = getCommand("friendlycore");
        if (cmd != null) cmd.setExecutor(new FriendlyCoreCommand(this));

        featureManager.enableConfigured();

        var expansion = PlaceholderRegistrar.register(this);
        if (expansion != null) {
            for (var feature : featureManager.getFeatures()) {
                if (feature instanceof PlaceholderProvider provider) {
                    provider.registerPlaceholders(expansion);
                }
            }
        }

        getLogger().info("FriendlyCore enabled.");
    }

    @Override
    public void onDisable() {
        if (featureManager != null) featureManager.disableAll();
        if (playerSettings != null) playerSettings.shutdown();
    }

    public void reloadFriendlyCore() {
        reloadConfig();
        if (featureManager != null) featureManager.reloadConfigured();
    }
}