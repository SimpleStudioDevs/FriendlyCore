package com.friendlysmp.core.features.chatpatrol;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.features.chatpatrol.checks.BlacklistCheck;
import com.friendlysmp.core.features.chatpatrol.listeners.AnvilListener;
import com.friendlysmp.core.features.chatpatrol.listeners.BookListener;
import com.friendlysmp.core.features.chatpatrol.listeners.ChatListener;
import com.friendlysmp.core.features.chatpatrol.listeners.SignListener;
import com.friendlysmp.core.features.chatpatrol.managers.ConfigManager;
import com.friendlysmp.core.schedulers.Schedulers;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.util.List;

import static org.bukkit.Bukkit.getServer;

public class ChatPatrolFeature extends Feature {
    private final Schedulers schedulers;

    private ConfigManager configManager;
    private BlacklistCheck blacklistCheck;

    public ChatPatrolFeature(FriendlyCorePlugin plugin, Schedulers schedulers) {
        super(plugin);
        this.schedulers = schedulers;
    }

    @Override
    public String id() {
        return "chat-patrol";
    }

    @Override
    public void enable() {
        if (configManager == null) configManager = new ConfigManager(plugin);
        configManager.reload();
        blacklistCheck = new BlacklistCheck(this);

        registerListener(new ChatListener(this));
        registerListener(new SignListener(this));
        registerListener(new AnvilListener(this));
        registerListener(new BookListener(this));

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
        blacklistCheck = null;
        plugin.getLogger().info("ChatPatrol disabled");
    }

    @Override
    public void reload() {
        configManager.reload();
        plugin.getLogger().info("ChatPatrol config reloaded");
    }


    public boolean punishIfBlacklisted(Player player, String message, String reason) {
        BlacklistCheck check = blacklistCheck;
        if (check == null || !configManager.isWordFilterEnabled()) return false;
        return check.handleText(player, message, reason);
    }

    public FriendlyCorePlugin getPlugin() { return plugin; }
    public ConfigManager getConfigManager() { return configManager; }

    public Schedulers getSchedulers() { return schedulers; }
}
