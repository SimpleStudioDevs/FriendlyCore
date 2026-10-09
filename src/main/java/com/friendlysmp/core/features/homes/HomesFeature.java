package com.friendlysmp.core.features.homes;

import com.earth2me.essentials.IEssentials;
import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.schedulers.Schedulers;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class HomesFeature extends Feature implements Listener {

    private final PlayerSettingsStore playerSettings;
    private final Schedulers schedulers;
    private final FeatureConfig config;
    private final Map<UUID, Integer> bonusHomes = new ConcurrentHashMap<>();
    private final Object writeLock = new Object();

    private IEssentials essentials;
    private HomesDao dao;

    public HomesFeature(FriendlyCorePlugin plugin, PlayerSettingsStore playerSettings, Schedulers schedulers) {
        super(plugin);
        this.playerSettings = playerSettings;
        this.schedulers = schedulers;
        this.config = new FeatureConfig(plugin, "FeatureConfigs/homes.yml");
    }

    public record BonusChange(boolean applied, int before, int after) {}

    @Override
    public String id() {
        return "homes";
    }

    @Override
    public void enable() {
        config.load();
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Essentials")) {
            plugin.getLogger().info("[Homes] EssentialsX is not enabled!");
            disable();
            return;
        }

        try {
            dao = new HomesDao(playerSettings.dataSource());
            dao.init();
        } catch (Exception e) {
            plugin.getLogger().severe("[Homes] Failed to init bonus homes table: " + e.getMessage());
            return;
        }

        essentials = (IEssentials) plugin.getServer().getPluginManager().getPlugin("Essentials");
        registerListener(this);
        registerListener(new SetHomeListener(this, essentials));

        PluginCommand cmd = plugin.getCommand("granthomes");
        if (cmd != null) {
            GrantHomesCommand exec = new GrantHomesCommand(this, false);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }

        PluginCommand revokeCmd = plugin.getCommand("revokehomes");
        if (revokeCmd != null) {
            GrantHomesCommand exec = new GrantHomesCommand(this, true);
            revokeCmd.setExecutor(exec);
            revokeCmd.setTabCompleter(exec);
        }

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            loadAsync(online.getUniqueId());
        }
    }

    @Override
    public void disable() {
        bonusHomes.clear();
    }

    @Override
    public void reload() {
        config.load();
    }

    public int getMaxBonusHomes() {
        return Math.max(0, config.get().getInt("max-bonus-homes", 10));
    }

    public int getBonusHomes(UUID uuid) {
        Integer cached = bonusHomes.get(uuid);
        if (cached != null) return Math.min(cached, getMaxBonusHomes());
        try {
            int amount = dao.getBonusHomes(uuid);
            bonusHomes.put(uuid, amount);
            return Math.min(amount, getMaxBonusHomes());
        } catch (Exception e) {
            plugin.getLogger().warning("[Homes] Failed to load bonus homes for " + uuid + ": " + e.getMessage());
            return 0;
        }
    }

    public void changeBonusHomes(UUID uuid, int delta, Consumer<BonusChange> onDone, Runnable onFailure) {
        schedulers.async(() -> {
            try {
                BonusChange change;
                synchronized (writeLock) {
                    int before = dao.getBonusHomes(uuid);
                    int after = Math.max(0, before + delta);
                    if (delta > 0 && after > getMaxBonusHomes()) {
                        change = new BonusChange(false, before, before);
                    } else {
                        dao.setBonusHomes(uuid, after);
                        change = new BonusChange(true, before, after);
                    }
                }
                if (change.applied() && plugin.getServer().getPlayer(uuid) != null) bonusHomes.put(uuid, change.after());
                onDone.accept(change);
            } catch (Exception e) {
                plugin.getLogger().warning("[Homes] Failed to change bonus homes for " + uuid + ": " + e.getMessage());
                onFailure.run();
            }
        });
    }

    private void loadAsync(UUID uuid) {
        schedulers.async(() -> {
            try {
                bonusHomes.put(uuid, dao.getBonusHomes(uuid));
            } catch (Exception e) {
                plugin.getLogger().warning("[Homes] Failed to load bonus homes for " + uuid + ": " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bonusHomes.remove(event.getPlayer().getUniqueId());
    }
}
