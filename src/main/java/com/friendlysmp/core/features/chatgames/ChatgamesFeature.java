package com.friendlysmp.core.features.chatgames;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.schedulers.Schedulers;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.Configuration;

public class ChatgamesFeature extends Feature {
    private final Schedulers schedulers;
    private final FeatureConfig config;

    public ChatgamesFeature(FriendlyCorePlugin plugin, Schedulers schedulers) {
        super(plugin);
        this.schedulers = schedulers;
        this.config = new FeatureConfig(plugin, "FeatureConfigs/chatgames.yml");
    }

    @Override
    public String id() {
        return "chatgames";
    }

    @Override
    public void enable() {
        config.load();
        var command = plugin.getCommand("pcg");
        if (command != null) {
            ChatgamesCommand c = new ChatgamesCommand(this);
            command.setExecutor(c);
            command.setTabCompleter(c);
        }
    }

    @Override
    public void disable() {

    }

    @Override
    public void reload() {
        config.load();
    }

    public Economy getEconomy() {
        return plugin.getEconomy();
    }

    public Configuration getConfig() {
        return config.get();
    }

    public FriendlyCorePlugin getPlugin() {
        return plugin;
    }

    public Schedulers getSchedulers() {
        return schedulers;
    }


}
