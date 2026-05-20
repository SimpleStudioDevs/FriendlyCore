package com.friendlysmp.core.features.chatpatrol.managers;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.features.chatpatrol.ChatPatrolFeature;

import java.util.List;

public class ConfigManager {
    private final FriendlyCorePlugin plugin;
    public ConfigManager(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
    }
    public void reload() {
        plugin.reloadConfig();
    }


    public boolean isWordFilterEnabled() {
        return plugin.getConfig().getBoolean("chat-patrol.ENABLE-WORD-FILTER", true);
    }

    public List<String> getBlacklistedWords() {
        return plugin.getConfig().getStringList("chat-patrol.BLACKLISTED-WORDS");
    }


    public String getBlacklistedWordsCommand() {
        return plugin.getConfig().getString(
                "chat-patrol.PUNISHMENTS.BLACKLISTED-WORDS-COMMAND",
                "ban {player} You were banned for using inappropriate language!"
        );
    }

}
