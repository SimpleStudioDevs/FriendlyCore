package com.friendlysmp.core.features.chatpatrol.managers;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;

import java.util.List;

public class ConfigManager {
    private static final String RESOURCE_PATH = "FeatureConfigs/ChatPatrol/config.yml";

    private final FeatureConfig config;

    public ConfigManager(FriendlyCorePlugin plugin) {
        this.config = new FeatureConfig(plugin, RESOURCE_PATH);
    }

    public void reload() {
        config.load();
    }


    public boolean isWordFilterEnabled() {
        return config.get().getBoolean("ENABLE-WORD-FILTER", true);
    }

    public List<String> getBlacklistedWords() {
        return config.get().getStringList("BLACKLISTED-WORDS");
    }


    public String getBlacklistedWordsCommand() {
        return config.get().getString(
                "PUNISHMENTS.BLACKLISTED-WORDS-COMMAND",
                "ban {player} You were banned for using inappropriate language!"
        );
    }

}
