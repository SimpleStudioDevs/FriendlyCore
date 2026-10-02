package com.friendlysmp.core.features.playerbroadcast;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BroadcastFeature extends Feature {
    private Economy economy;
    private final Map<UUID, long[]> freeUseTracker = new HashMap<>();
    private final FeatureConfig config;

    public BroadcastFeature(FriendlyCorePlugin plugin) {
        super(plugin);
        this.config = new FeatureConfig(plugin, "FeatureConfigs/playerbroadcast.yml");
    }

    @Override
    public String id() {
        return "player-broadcast";
    }

    @Override
    public void enable() {
        config.load();
        MiniMessage miniMessage = MiniMessage.miniMessage();

        this.economy = plugin.getEconomy();

        MessageUtil.init(this, miniMessage);

        BroadcastCommand broadcastCommand = new BroadcastCommand(this);

        plugin.getCommand("pbroadcast").setExecutor(broadcastCommand);
        plugin.getCommand("pbroadcast").setTabCompleter(broadcastCommand);

        plugin.getCommand("pbreload").setExecutor(broadcastCommand);
        plugin.getCommand("pbreload").setTabCompleter(broadcastCommand);




    }

    @Override
    public void disable() {

    }

    @Override
    public void reload() {
        config.load();
        freeUseTracker.clear();
    }


    public Economy getEconomy() {
        return economy;
    }
    public Configuration getConfig() { return config.get(); }

    public int consumeFreeUse(UUID uuid, String group) {
        if (group == null) return 0;

        long resetMs = config.get().getLong("free-uses." + group + ".interval", 60) * 60_000L;
        int maxFree = config.get().getInt("free-uses." + group + ".count");

        long[] entry = freeUseTracker.get(uuid);
        long now = System.currentTimeMillis();

        if (entry == null || now - entry[1] >= resetMs) {
            freeUseTracker.put(uuid, new long[]{maxFree - 1, now});
            return maxFree;
        }

        if (entry[0] > 0) {
            entry[0]--;
            return (int) entry[0] + 1;
        }

        return 0;
    }

    public @Nullable String resolveGroup(Player player) {
        ConfigurationSection groups = config.get().getConfigurationSection("free-uses");
        if (groups == null) return null;

        for (String key : groups.getKeys(false)) {
            if (player.hasPermission("friendlycore.pbc.free-uses." + key)) return key;
        }
        return null;
    }

    public int remainingUses(Player player) {
        String group = resolveGroup(player);
        if (group == null) return 0;

        long resetMs = config.get().getLong("free-uses." + group + ".interval", 60) * 60_000L;
        int maxFree = config.get().getInt("free-uses." + group + ".count");

        long[] entry = freeUseTracker.get(player.getUniqueId());
        if (entry == null || System.currentTimeMillis() - entry[1] >= resetMs) return maxFree;

        return (int) entry[0];
    }

    public String remainingTimeFormatted(Player player) {
        String group = resolveGroup(player);
        if (group == null) return "0 seconds";

        long resetMs = config.get().getLong("free-uses." + group + ".interval", 60) * 60_000L;

        long[] entry = freeUseTracker.get(player.getUniqueId());
        if (entry == null) return "0 seconds";

        long remainingMs = Math.max(0, resetMs - (System.currentTimeMillis() - entry[1]));
        long seconds = remainingMs / 1000;

        if (seconds < 60) return seconds + " seconds";
        return (seconds / 60) + " minutes";
    }


}
