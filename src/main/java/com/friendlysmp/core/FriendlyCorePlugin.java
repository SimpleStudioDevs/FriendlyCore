package com.friendlysmp.core;

import com.friendlysmp.core.command.FriendlyCoreCommand;
import com.friendlysmp.core.feature.FeatureManager;
import com.friendlysmp.core.features.beaconhider.BeaconHiderFeature;
import com.friendlysmp.core.features.bottlexp.BottleXPFeature;
import com.friendlysmp.core.features.chatgames.ChatgamesFeature;
import com.friendlysmp.core.features.chatpatrol.ChatPatrolFeature;
import com.friendlysmp.core.features.commandmaker.CommandFeature;
import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.creeperdisable.CreeperDisableFeature;
import com.friendlysmp.core.features.deathmessage.DeathMessageFeature;
import com.friendlysmp.core.features.dupepatch.DupePatchFeature;
import com.friendlysmp.core.features.geysercombatlog.GeyserCombatLogFeature;
import com.friendlysmp.core.features.hidearmor.HideArmorFeature;
import com.friendlysmp.core.features.homes.HomesFeature;
import com.friendlysmp.core.features.playerbroadcast.BroadcastFeature;
import com.friendlysmp.core.features.sleepcap.SleepCapFeature;
import com.friendlysmp.core.features.tokens.TokenFeature;
import com.friendlysmp.core.features.voicechat.VoiceChatFeature;
import com.friendlysmp.core.features.voidguard.VoidGuardFeature;
import com.friendlysmp.core.features.withersound.WitherSoundFeature;
import com.friendlysmp.core.features.zeladdon.ZelAddonFeature;
import com.friendlysmp.core.features.zelduels.ZelDuelsFeature;
import com.friendlysmp.core.placeholder.PlaceholderProvider;
import com.friendlysmp.core.placeholder.PlaceholderRegistrar;
import com.friendlysmp.core.schedulers.Schedulers;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import com.friendlysmp.core.util.ConfirmationMenuListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class FriendlyCorePlugin extends JavaPlugin {
    private Schedulers schedulers;
    private PlayerSettingsStore playerSettings;
    private FeatureManager featureManager;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.schedulers = new Schedulers(this);

        this.playerSettings = new PlayerSettingsStore(this, schedulers);

        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new ConfirmationMenuListener(), this);

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
        ChatPatrolFeature chatPatrol = new ChatPatrolFeature(this, schedulers);
        ZelAddonFeature zelAddon = new ZelAddonFeature(this, schedulers);
        featureManager.register(chatPatrol);
        featureManager.register(zelAddon);
        featureManager.register(new BottleXPFeature(this));
        featureManager.register(new SleepCapFeature(this));
        featureManager.register(new VoidGuardFeature(this));
        featureManager.register(new GeyserCombatLogFeature(this));
        featureManager.register(new BeaconHiderFeature(this));
        featureManager.register(new ZelDuelsFeature(this));
        featureManager.register(new HideArmorFeature(this, playerSettings));
        featureManager.register(new DupePatchFeature(this));
        featureManager.register(new HomesFeature(this, playerSettings, schedulers));
        if (pm.getPlugin("voicechat") != null) {
            featureManager.register(new VoiceChatFeature(this, schedulers, playerSettings, chatPatrol, zelAddon));
        }
        featureManager.register(new CreeperDisableFeature(this));
        featureManager.register(new DeathMessageFeature(this, playerSettings));

        // Vault-required features
        if (setupEconomy()) {
            featureManager.register(new BroadcastFeature(this));
            featureManager.register(new ChatgamesFeature(this, schedulers));
        } else {
            getLogger().severe("Vault or an economy provider was not found. Vault-required features will not function");
        }

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

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }
}