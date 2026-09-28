package com.friendlysmp.core.features.creativeitemcontrol;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.features.creativeitemcontrol.commands.CICCommand;
import com.friendlysmp.core.features.creativeitemcontrol.listeners.DispenserListener;
import com.friendlysmp.core.features.creativeitemcontrol.listeners.InventoryListener;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ConfigManager;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ExcludedItemManager;
import com.friendlysmp.core.features.creativeitemcontrol.managers.MessageManager;
import org.bukkit.event.HandlerList;

import java.io.File;

public class CreativeFeature implements Feature {
    private final FriendlyCorePlugin plugin;
    private ConfigManager configManager;
    private ExcludedItemManager excludedItemManager;
    private MessageManager messageManager;
    private InventoryListener inventoryListener;
    private DispenserListener dispenserListener;

    public CreativeFeature(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "creativeitemcontrol";
    }

    @Override
    public void enable() {
        configManager = new ConfigManager(this);
        configManager.load();
        excludedItemManager = new ExcludedItemManager(this);
        excludedItemManager.loadAll();
        messageManager = new MessageManager(this);
        dispenserListener = new DispenserListener(this);
        inventoryListener = new InventoryListener(this);

        plugin.getServer().getPluginManager().registerEvents(inventoryListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(dispenserListener, plugin);

        var cmd = plugin.getCommand("cic");
        if (cmd != null) {
            CICCommand cicCommand = new CICCommand(this);
            cmd.setExecutor(cicCommand);
            cmd.setTabCompleter(cicCommand);
        }
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(dispenserListener);
        HandlerList.unregisterAll(inventoryListener);
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
        configManager.load();
        excludedItemManager.loadAll();
    }

    public FriendlyCorePlugin getPlugin() { return plugin; }
    public File getDataFolder() { return new File(plugin.getDataFolder(), "CreativeItemControl"); }
    public ConfigManager getConfigManager() { return configManager; }
    public ExcludedItemManager getExcludedItemManager() { return excludedItemManager; }
    public MessageManager getMessageManager() { return messageManager; }
}
