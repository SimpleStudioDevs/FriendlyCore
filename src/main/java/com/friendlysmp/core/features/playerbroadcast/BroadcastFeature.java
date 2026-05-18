package com.friendlysmp.core.features.playerbroadcast;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BroadcastFeature implements Feature {
    private Economy economy;
    private final FriendlyCorePlugin plugin;
    private final Map<UUID, long[]> freeUseTracker = new HashMap<>();

    public BroadcastFeature(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "player-broadcast";
    }

    @Override
    public void enable() {
        MiniMessage miniMessage = MiniMessage.miniMessage();

        if (!setupEconomy()) {
            plugin.getLogger().severe("Vault or an economy provider was not found. PlayerBroadcast will not function");
            return;
        }

        MessageUtil.init(plugin, miniMessage);

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
        plugin.reloadConfig();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }
    public Configuration getConfig() {return plugin.getConfig(); }

    public int consumeFreeUse(UUID uuid, String group) {
        if (group == null) return 0;

        long resetMs = plugin.getConfig().getLong("player-broadcast.free-uses." + group + ".interval", 60) * 60_000L;
        int maxFree = plugin.getConfig().getInt("player-broadcast.free-uses." + group + ".count");

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
        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("player-broadcast.free-uses");

        if (groups == null) return null;

        for (String key : groups.getKeys(false)) {
            String perm = "friendlycore.pbc.free-uses." + key;
            if (player.hasPermission(perm)) return key;
        }
        return null;
    }


}
