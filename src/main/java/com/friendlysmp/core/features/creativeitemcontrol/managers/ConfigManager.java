package com.friendlysmp.core.features.creativeitemcontrol.managers;

import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ConfigManager {
    private static final String RESOURCE_PATH = "FeatureConfigs/CreativeItemControl/config.yml";

    public enum ConfigKeys {
        // Main Config Options
        MAIN_ENABLED("config.enabled", true),
        MAIN_PLAYER_ALERTS("config.player-alerts", true),
        MAIN_ALERT_COOLDOWN("config.alert-cooldown", 1000),
        // Worlds handled separately, lists are weird
        MAIN_BLACKLIST("config.blacklist", false),
        MAIN_GIVE_COOLDOWN("config.give-cooldown", 0),
        MAIN_DISPENSERS_ENABLED("config.check-dispensers", true),

        // Enchantments Settings
        ENCHANT_ENABLED("enchantments.enabled", true),
        ENCHANT_ACTION("enchantments.action", "LOWER"),
        ENCHANT_ALLOW_INCOMPATIBLE("enchantments.allow-incompatible", false),

        // Attribute Settings
        ATTRIBUTES_ENABLED("attributes.enabled", true),
        ATTRIBUTES_ACTION("attributes.action", "REMOVE"),

        // Potion Settings
        POTIONS_ENABLED("potions.enabled", true),

        // Component Settings
        COMPONENTS_ENABLED("components.enabled", true),
        COMPONENTS_BLOCKED("components.blocked", new ArrayList<String>()),

        // Messages
        ALERTS_COMPONENTS("messages.alerts.components", "<b><red>Items with custom components are not allowed here!"),
        ALERTS_ATTRIBUTES("messages.alerts.attributes", "<b><red>Items with attribute modifiers are not allowed here!"),
        ALERTS_ENCHANTMENTS("messages.alerts.enchantments", "<b><red>Items with impossible enchantments are not allowed here!"),
        ALERTS_POTIONS("messages.alerts.potions", "<b><red>Custom potions are not allowed here!"),

        CMD_EXCLUDE("messages.commands.exclude", "&aStored {type} as \"{id}\"."),
        CMD_REMOVE("messages.commands.remove", "&aRemoved excluded item \"{id}\"."),
        CMD_LIST_EMPTY("messages.commands.listempty", "&eNo excluded items stored."),
        CMD_GIVE("messages.commands.give", "&aGave \"{id}\" to &f&a{player}."),
        CMD_GIVE_INVALID("messages.commands.giveinvalid", "&cNo excluded item found with id \"{id}\"."),
        CMD_GIVE_OTHERS_FAIL("messages.commands.giveothersfail", "&cYou do not have permission to give to other players!"),
        CMD_PLAYER_NOT_FOUND("messages.commands.playernotfound", "&cPlayer \"{player}\" not found."),
        CMD_SPECIFY_PLAYER("messages.commands.specifyplayer", "&cSpecify a player: /cic give <id> <player>"),
        CMD_INVALID_WORLD("messages.commands.invalidworld", "&cYou cannot use this in this world!"),
        CMD_ON_COOLDOWN("messages.commands.oncooldown", "&cYou must wait {time}s before receiving this item again."),
        CMD_NO_PERM("messages.commands.noperm", "&cYou do not have permission!");


        public final String path;
        public final Object defaultValue;

        ConfigKeys(String path, Object defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
    }

    public final Map<ConfigKeys, Object> cachedConfig = new HashMap<>();
    public List<World> worlds = new ArrayList<>();
    public List<DataComponentType> resolvedComponents = new ArrayList<>();
    private final Map<Material, ItemStack> defaultItemCache = new EnumMap<>(Material.class);
    private final FeatureConfig featureConfig;
    private YamlConfiguration config = new YamlConfiguration();

    public ConfigManager(CreativeFeature feature) {
        this.featureConfig = new FeatureConfig(feature.getPlugin(), RESOURCE_PATH);
    }

    public void load() {
        featureConfig.load();
        config = featureConfig.get();

        cachedConfig.clear();
        worlds.clear();

        for (ConfigKeys key : ConfigKeys.values()) {
            cachedConfig.put(key, config.get(key.path, key.defaultValue));
        }

        List<String> worldStrings = config.getStringList("config.worlds");

        if (!worldStrings.isEmpty()) {
            for (String worldName : worldStrings) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) worlds.add(world);
            }

        }

        String attributesAction = (String) cachedConfig.get(ConfigKeys.ATTRIBUTES_ACTION);
        if (!attributesAction.equalsIgnoreCase("remove") && !attributesAction.equalsIgnoreCase("delete")) {
            cachedConfig.replace(ConfigKeys.ATTRIBUTES_ACTION, ConfigKeys.ATTRIBUTES_ACTION.defaultValue);
        }

        String enchantmentsAction = (String) cachedConfig.get(ConfigKeys.ENCHANT_ACTION);
        if (!enchantmentsAction.equalsIgnoreCase("lower") && !enchantmentsAction.equalsIgnoreCase("remove") && !enchantmentsAction.equalsIgnoreCase("delete")) {
            cachedConfig.replace(ConfigKeys.ENCHANT_ACTION, ConfigKeys.ENCHANT_ACTION.defaultValue);
        }

        resolvedComponents.clear();
        List<String> components = (List<String>) cachedConfig.get(ConfigKeys.COMPONENTS_BLOCKED);
        for (String name : components) {
            NamespacedKey key = NamespacedKey.fromString(name);
            if (key == null) continue;
            DataComponentType type = Registry.DATA_COMPONENT_TYPE.get(key);
            if (type == null) continue;
            resolvedComponents.add(type);
        }



    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public boolean getBoolean(ConfigKeys key) {
        Object value = cachedConfig.get(key);
        return (value instanceof Boolean) ? (boolean) value : (boolean) key.defaultValue;
    }

    public int getInt(ConfigKeys key) {
        Object value = cachedConfig.get(key);
        return (value instanceof Integer) ? (int) value : (int) key.defaultValue;
    }

    public String getString(ConfigKeys key) {
        Object value = cachedConfig.get(key);
        return (value instanceof String) ? (String) value : (String) key.defaultValue;
    }

    public List<String> getStringList(ConfigKeys key) {
        Object value = cachedConfig.get(key);
        return  (value instanceof List) ? (List<String>) value : (List<String>) key.defaultValue;
    }

    public boolean isWorldDenied(World world) {
        return (boolean) cachedConfig.get(ConfigKeys.MAIN_BLACKLIST) == worlds.contains(world);
    }

    public List<DataComponentType> getResolvedComponents() {
        return resolvedComponents;
    }

    public ItemStack getDefaultItem(Material type) {
        return defaultItemCache.computeIfAbsent(type, t -> new ItemStack(t, 1));
    }



}

