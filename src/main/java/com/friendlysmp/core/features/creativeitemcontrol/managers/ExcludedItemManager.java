package com.friendlysmp.core.features.creativeitemcontrol.managers;

import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ExcludedItemManager {
    private final File file;
    private final YamlConfiguration yaml;
    private final CreativeFeature feature;
    private Map<String, ItemStack> excludedItems;
    private Map<String, Long> giveCooldowns = new HashMap<>();

    public ExcludedItemManager(CreativeFeature feature) {
        this.feature = feature;
        feature.getDataFolder().mkdirs();
        this.file = new File(feature.getDataFolder(), "cic_items.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public Map<String, ItemStack> loadAll() {
        Map<String, ItemStack> result = new LinkedHashMap<>();
        for (String key : yaml.getKeys(false)) {
            ItemStack item = yaml.getItemStack(key);
            if (item != null) { result.put(key, item); }
        }
        excludedItems = result;
        for (String id : excludedItems.keySet()) {
            registerGivePermission(id);
        }
        return result;
    }

    public String givePermissionNode(String id) {
        return "cic.give." + id.toLowerCase(Locale.ROOT);
    }

    public void registerGivePermission(String id) {
        PluginManager pm = Bukkit.getPluginManager();
        String node = givePermissionNode(id);
        if (pm.getPermission(node) != null) return;
        pm.addPermission(new Permission(node, "Allows giving the excluded item '" + id + "'", PermissionDefault.OP, Map.of("cic.give", true)));
    }

    public void unregisterGivePermission(String id) {
        Bukkit.getPluginManager().removePermission(givePermissionNode(id));
    }


    public void flush() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save cic_items.yml", e);
        }
    }

    public void storeExcludedItem(String id, ItemStack item) {
        excludedItems.put(id, item);
        yaml.set(id,  item);
        flush();
        registerGivePermission(id);
    }

    public void removeExcludedItem(String id) {
        excludedItems.remove(id);
        yaml.set(id, null);
        flush();
        unregisterGivePermission(id);
    }

    public ItemStack getExcludedItem(String id) {
        return excludedItems.get(id);
    }

    public boolean excludedItemExists(String id) {
        return excludedItems.containsKey(id);
    }

    public Map<String, ItemStack> getExcludedItems() {
        return Collections.unmodifiableMap(excludedItems);
    }

    public boolean isExcluded(ItemStack item) {
        return excludedItems.values().stream().anyMatch(e -> e.isSimilar(item));
    }

    public boolean isOnGiveCooldown(UUID targetId, String itemId) {
        String key = targetId + ":" + itemId;
        Long last = giveCooldowns.get(key);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < feature.getConfigManager().getInt(ConfigManager.ConfigKeys.MAIN_GIVE_COOLDOWN);
    }

    public long getGiveCooldownRemaining(UUID targetId, String itemId) {
        String key = targetId + ":" + itemId;
        Long last = giveCooldowns.get(key);
        return feature.getConfigManager().getInt(ConfigManager.ConfigKeys.MAIN_GIVE_COOLDOWN) - (System.currentTimeMillis() - last) / 1000L;
    }

    public void recordGive(UUID targetId, String itemId) {
        giveCooldowns.put(targetId + ":" + itemId, System.currentTimeMillis());
    }

    public void give(Player player, String itemId) {
        ItemStack item =  getExcludedItem(itemId);
        if (item == null) return;


        player.give(item);
        recordGive(player.getUniqueId(), itemId);
    }

    public boolean canFit(Inventory inventory, ItemStack item) {
        int remaining = item.getAmount();
        int maxStack = item.getMaxStackSize();

        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                remaining -= maxStack;
            } else if (slot.isSimilar(item)) {
                int space = maxStack - slot.getAmount();
                if (space > 0) remaining -= space;
            }
            if (remaining <= 0) return true;
        }
        return remaining <= 0;
    }


}
