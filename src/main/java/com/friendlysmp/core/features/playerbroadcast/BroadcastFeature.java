package com.friendlysmp.core.features.playerbroadcast;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.RegisteredServiceProvider;

public class BroadcastFeature implements Feature {
    private Economy economy;
    private final FriendlyCorePlugin plugin;

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


}
