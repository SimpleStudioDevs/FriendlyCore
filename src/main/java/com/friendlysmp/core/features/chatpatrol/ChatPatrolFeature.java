package com.friendlysmp.core.features.chatpatrol;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.features.chatpatrol.listeners.AnvilListener;
import com.friendlysmp.core.features.chatpatrol.listeners.BookListener;
import com.friendlysmp.core.features.chatpatrol.listeners.ChatListener;
import com.friendlysmp.core.features.chatpatrol.listeners.SignListener;
import com.friendlysmp.core.features.chatpatrol.managers.ConfigManager;
import com.friendlysmp.core.schedulers.Schedulers;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;

import java.util.List;

import static org.bukkit.Bukkit.getServer;

public class ChatPatrolFeature implements Feature {
    public final FriendlyCorePlugin plugin;
    private final Schedulers schedulers;

    private ConfigManager configManager;
    public ChatPatrolFeature(FriendlyCorePlugin plugin, Schedulers schedulers) {
        this.plugin = plugin;
        this.schedulers = schedulers;
    }

    @Override
    public String id() {
        return "chat-patrol";
    }

    @Override
    public void enable() {
        this.configManager = new ConfigManager(plugin);


        getServer().getPluginManager().registerEvents(new ChatListener(this), plugin);
        getServer().getPluginManager().registerEvents(new SignListener(this), plugin);
        getServer().getPluginManager().registerEvents(new AnvilListener(this), plugin);
        getServer().getPluginManager().registerEvents(new BookListener(this), plugin);

        PluginCommand command = plugin.getCommand("chatpatrol");
        if (command != null) {
            command.setExecutor(new ChatPatrolCommand(this));
        } else {
            plugin.getLogger().warning("Command 'chatpatrol' is not defined in plugin.yml");
        }

        plugin.getLogger().info("ChatPatrol enabled");
    }

    @Override
    public void disable() {
        plugin.getLogger().info("ChatPatrol disabled");
    }

    @Override
    public void reload() {
        configManager.reload();
        plugin.getLogger().info("ChatPatrol config reloaded");
    }


    public Configuration getConfig() {
        return plugin.getConfig();
    }
    public ConfigManager getConfigManager() { return configManager; }

    public Schedulers getSchedulers() { return schedulers; }
}
