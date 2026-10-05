package com.friendlysmp.core.features.creativeitemcontrol;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.features.creativeitemcontrol.commands.CICCommand;
import com.friendlysmp.core.features.creativeitemcontrol.listeners.DispenserListener;
import com.friendlysmp.core.features.creativeitemcontrol.listeners.InventoryListener;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ConfigManager;
import com.friendlysmp.core.features.creativeitemcontrol.managers.ExcludedItemManager;
import com.friendlysmp.core.features.creativeitemcontrol.managers.MessageManager;

import java.io.File;

public class CreativeFeature extends Feature {
    private ConfigManager configManager;
    private ExcludedItemManager excludedItemManager;
    private MessageManager messageManager;
    private InventoryListener inventoryListener;
    private DispenserListener dispenserListener;

    public CreativeFeature(FriendlyCorePlugin plugin) {
        super(plugin);
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

        registerListener(inventoryListener);
        registerListener(dispenserListener);

        var cmd = plugin.getCommand("cic");
        if (cmd != null) {
            CICCommand cicCommand = new CICCommand(this);
            cmd.setExecutor(cicCommand);
            cmd.setTabCompleter(cicCommand);
        }
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {
        configManager.load();
        excludedItemManager.loadAll();
    }

    public FriendlyCorePlugin getPlugin() { return plugin; }
    public File getDataFolder() { return new File(plugin.getDataFolder(), "FeatureConfigs/CreativeItemControl"); }
    public ConfigManager getConfigManager() { return configManager; }
    public ExcludedItemManager getExcludedItemManager() { return excludedItemManager; }
    public MessageManager getMessageManager() { return messageManager; }
}
