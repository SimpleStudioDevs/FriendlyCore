package com.friendlysmp.core.features.tokens;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class TokenFeature extends Feature implements Listener {
    private final PlayerSettingsStore playerSettings;
    private final FeatureConfig config;

    private TokenDao dao;
    private TokenService service;
    private TokenCommand command;

    public TokenFeature(FriendlyCorePlugin plugin, PlayerSettingsStore playerSettings) {
        super(plugin);
        this.playerSettings = playerSettings;
        this.config = new FeatureConfig(plugin, "FeatureConfigs/tokens.yml");
    }

    @Override
    public String id() {
        return "tokens";
    }

    @Override
    public void enable() {
        config.load();
        try {
            this.dao = new TokenDao(playerSettings.dataSource());
            this.dao.init();
            this.service = new TokenService(plugin, config, dao);
            this.command = new TokenCommand(plugin, service);

            registerListener(this);

            PluginCommand token = plugin.getCommand("token");
            if (token != null) {
                token.setExecutor(command);
                token.setTabCompleter(command);
            }

            plugin.getLogger().info("Token feature enabled.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to enable token feature: " + e.getMessage());
        }
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {
        config.load();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("[TOKENS] TokenFeature saw join for " + player.getName());
        service.handleMonthlyJoin(player);
        service.handleOfflineTokens(player);
    }
}