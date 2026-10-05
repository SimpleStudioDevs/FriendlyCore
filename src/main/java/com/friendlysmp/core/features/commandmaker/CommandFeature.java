package com.friendlysmp.core.features.commandmaker;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.features.commandmaker.arguments.ArgsDefinition;
import com.friendlysmp.core.features.commandmaker.commandcreation.CustomCommand;
import com.friendlysmp.core.features.commandmaker.commands.AdminCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class CommandFeature extends Feature {
    public static final String FILE_NAME = "FeatureConfigs/commandmaker.yml";
    public static final String ENABLED_COMMANDS = "enabled-commands";
    public static final String COMMANDS = "commands";
    public static final List<String> ACTION_PREFIXES = List.of("BROADCAST:", "MESSAGE:", "PLAYER:", "CONSOLE:", "SOUND:", "SOUNDALL:");

    private final List<CustomCommand> registeredCommands = new ArrayList<>();
    private final FeatureConfig config;
    private boolean papi;

    public CommandFeature(FriendlyCorePlugin plugin) {
        super(plugin);
        this.config = new FeatureConfig(plugin, FILE_NAME, false);
    }

    @Override
    public String id() {
        return "command-maker";
    }

    @Override
    public void enable() {
        papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (!papi) {
            plugin.getLogger().info("PlaceholderAPI not detected, CommandMaker PAPI placeholders will be in plain-text!");
        }

        config.load();
        registerCommands();

        PluginCommand adminCommand = Objects.requireNonNull(plugin.getCommand("commandmaker"));
        AdminCommand executor = new AdminCommand(this);
        adminCommand.setExecutor(executor);
        adminCommand.setTabCompleter(executor);
    }

    @Override
    public void disable() {
        unregisterCommands();
    }

    @Override
    public void reload() {
        unregisterCommands();
        config.load();
        registerCommands();
    }

    public void saveAndReload() {
        config.save();
        reload();
    }

    public FriendlyCorePlugin getPlugin() {
        return plugin;
    }

    public FileConfiguration getConfig() {
        return config.get();
    }

    public boolean isPapiEnabled() {
        return papi;
    }

    public Set<String> getCommandKeys() {
        ConfigurationSection section = getConfig().getConfigurationSection(COMMANDS);
        return section == null ? Set.of() : section.getKeys(false);
    }

    public static String commandPath(String name) {
        return COMMANDS + "." + name;
    }

    private void registerCommands() {
        CommandMap commandMap = Bukkit.getServer().getCommandMap();
        Map<String, Command> knownCommands = commandMap.getKnownCommands();
        for (String cmdName : getConfig().getStringList(ENABLED_COMMANDS)) {
            // Define command information and details
            String path = commandPath(cmdName);
            List<String> aliases = getConfig().getStringList(path + ".aliases");
            String permission = getConfig().getString(path + ".permission");
            List<String> actions = getConfig().getStringList(path + ".actions");

            actions.removeIf(action -> {
                int colonIndex = action.indexOf(":");
                if (colonIndex == -1 || !ACTION_PREFIXES.contains(action.substring(0, colonIndex + 1))) {
                    plugin.getLogger().warning("Incorrectly formatted action! Failed to parse: " + action + " This action will be disabled");
                    return true;
                }
                return false;
            });

            ConfigurationSection argsSection = getConfig().getConfigurationSection(path + ".args");
            List<ArgsDefinition> argDefs = new ArrayList<>();
            if (argsSection != null) {
                for (String argName : argsSection.getKeys(false)) {
                    // Define argument information, add each to argDefs array
                    String type = argsSection.getString(argName + ".type", "STRING");
                    boolean papi = argsSection.getBoolean(argName + ".placeholder", false);
                    List<String> options = argsSection.getStringList(argName + ".options");
                    argDefs.add(new ArgsDefinition(argName, type, papi, options));
                }
            }

            // Register built command to the server
            CustomCommand cmd = new CustomCommand(cmdName, aliases, actions, this, permission, argDefs);
            commandMap.register(plugin.getName(), cmd);

            // Force custom commands to take highest priority
            knownCommands.put(cmdName.toLowerCase(), cmd);
            for (String alias : aliases) {
                knownCommands.put(alias.toLowerCase(), cmd);
            }
            registeredCommands.add(cmd);
        }
        plugin.getLogger().info("CommandMaker registered " + registeredCommands.size() + " commands to the server");
    }

    private void unregisterCommands() {
        CommandMap commandMap = Bukkit.getServer().getCommandMap();
        Map<String, Command> knownCommands = commandMap.getKnownCommands();
        String prefix = plugin.getName().toLowerCase() + ":";
        for (CustomCommand cmd : registeredCommands) {
            cmd.unregister(commandMap);
            knownCommands.remove(cmd.getName());
            knownCommands.remove(prefix + cmd.getName());
            for (String alias : cmd.getAliases()) {
                knownCommands.remove(alias);
                knownCommands.remove(prefix + alias);
            }
        }
        registeredCommands.clear();
    }

}
