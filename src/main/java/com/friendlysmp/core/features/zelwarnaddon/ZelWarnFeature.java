package com.friendlysmp.core.features.zelwarnaddon;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.schedulers.Schedulers;
import it.pino.zelchat.api.ZelChatAPI;
import org.bukkit.configuration.Configuration;

import java.util.logging.Logger;

public class ZelWarnFeature implements Feature {
    private final FriendlyCorePlugin plugin;
    private final Schedulers schedulers;
    private SwearWarnModule module;

    public ZelWarnFeature(FriendlyCorePlugin plugin, Schedulers schedulers) {
        this.plugin = plugin;
        this.schedulers = schedulers;
    }

    @Override
    public String id() {
        return "zel-warn-addon";
    }

    @Override
    public void enable() {
        module = new SwearWarnModule(this);

        getLogger().info("About to register module...");
        ZelChatAPI.get().getModuleManager().register(plugin, module);
        getLogger().info("Module register call finished.");
    }

    @Override
    public void disable() {
        if (module != null) {
            ZelChatAPI.get().getModuleManager().unregister(plugin, module);
        }
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
    }

    public final Configuration getConfig() {
        return plugin.getConfig();
    }

    public final Logger getLogger() {
        return plugin.getLogger();
    }

    public final Schedulers getSchedulers() {
        return schedulers;
    }
}
