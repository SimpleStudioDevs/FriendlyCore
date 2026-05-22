package com.friendlysmp.core.features.zeladdon;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.schedulers.Schedulers;
import it.pino.zelchat.api.ZelChatAPI;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ZelAddonFeature implements Feature {
    private StaffChatModule staffChatModule;
    private SwearWarnModule swearWarnModule;
    private final FriendlyCorePlugin plugin;
    private final Schedulers schedulers;

    public ZelAddonFeature(FriendlyCorePlugin plugin, Schedulers schedulers) {
        this.plugin = plugin;
        this.schedulers = schedulers;
    }

    @Override
    public String id() {
        return "zel-addon";
    }

    @Override
    public void enable() {
        swearWarnModule =  new SwearWarnModule(this);
        staffChatModule = new StaffChatModule(loadFormats());
    }

    @Override
    public void disable() {
        if (swearWarnModule != null) {
            ZelChatAPI.get().getModuleManager().unregister(plugin, swearWarnModule);
        }
        if (staffChatModule != null) {
            ZelChatAPI.get().getModuleManager().unregister(plugin, staffChatModule);
        }
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
    }

    private Map<String, String> loadFormats() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("zel-addon.FORMATS");
        Map<String, String> formats = new LinkedHashMap<>();

        if (section != null) {
            for (String key : section.getKeys(false)) {
                formats.put(key, section.getString(key, ""));
            }
            this.getLogger().info("Loaded formats: " + formats.keySet());
        } else {
            plugin.getLogger().info("No formats found!");
        }
        return formats;
    }

    public Schedulers getSchedulers() {
        return schedulers;
    }

    public final Configuration getConfig() {
        return plugin.getConfig();
    }

    public final Logger getLogger() {
        return plugin.getLogger();
    }

}
