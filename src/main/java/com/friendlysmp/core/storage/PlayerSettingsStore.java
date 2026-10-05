package com.friendlysmp.core.storage;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.friendlysmp.core.schedulers.Schedulers;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSettingsStore {

    private final JavaPlugin plugin;
    private final Schedulers schedulers;

    private final SqlManager sql;
    private final WitherSoundDao witherDao;
    private final HideArmorDao hideArmorDao;

    private final Map<UUID, Boolean> muteWitherDeath = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> hideArmor = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> saveQueued = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> loading = new ConcurrentHashMap<>();

    public PlayerSettingsStore(JavaPlugin plugin, Schedulers schedulers) {
        this.plugin = plugin;
        this.schedulers = schedulers;

        this.sql = new SqlManager(plugin);
        this.witherDao = new WitherSoundDao(sql.dataSource());
        this.hideArmorDao = new HideArmorDao(sql.dataSource());

        try {
            witherDao.init();
            hideArmorDao.init();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to init SQLite tables: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    public DataSource dataSource() {
        return sql.dataSource();
    }

    public boolean isWitherDeathMuted(UUID uuid) {
        return muteWitherDeath.getOrDefault(uuid, false);
    }

    public boolean isArmorHidden(UUID uuid) {
        return hideArmor.getOrDefault(uuid, false);
    }

    private boolean isLoaded(UUID uuid) {
        return muteWitherDeath.containsKey(uuid) && hideArmor.containsKey(uuid);
    }

    public void ensureLoadedAsync(UUID uuid) {
        if (isLoaded(uuid)) return;
        if (loading.putIfAbsent(uuid, true) != null) return;

        schedulers.async(() -> {
            try {
                loadNow(uuid);
            } finally {
                loading.remove(uuid);
            }
        });
    }

    public void loadBlocking(UUID uuid) {
        if (isLoaded(uuid)) return;
        loadNow(uuid);
    }

    private void loadNow(UUID uuid) {
        try {
            boolean muted = witherDao.load(uuid);

            Boolean before = muteWitherDeath.get(uuid);
            muteWitherDeath.putIfAbsent(uuid, muted);
            boolean after = muteWitherDeath.getOrDefault(uuid, false);

            plugin.getLogger().info("[WitherStore] LOADED " + uuid
                    + " dbMuted=" + muted
                    + " cacheBefore=" + before
                    + " cacheAfter=" + after);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load wither sound setting for " + uuid + ": " + e.getMessage());
        }

        try {
            hideArmor.putIfAbsent(uuid, hideArmorDao.load(uuid));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load hide armor setting for " + uuid + ": " + e.getMessage());
        }
    }

    public boolean setWitherDeathMuted(UUID uuid, boolean muted) {
        muteWitherDeath.put(uuid, muted);
        queueSave(uuid);
        return muted;
    }

    public boolean toggleWitherDeathMuted(UUID uuid) {
        boolean now = !isWitherDeathMuted(uuid);
        setWitherDeathMuted(uuid, now);
        return now;
    }

    public boolean setArmorHidden(UUID uuid, boolean hidden) {
        hideArmor.put(uuid, hidden);
        queueSave(uuid);
        return hidden;
    }

    public boolean toggleArmorHidden(UUID uuid) {
        boolean now = !isArmorHidden(uuid);
        setArmorHidden(uuid, now);
        return now;
    }

    private void queueSave(UUID uuid) {
        if (saveQueued.putIfAbsent(uuid, true) != null) return;
        schedulers.asyncLater(Duration.ofMillis(300), () -> saveNowAsync(uuid));
    }

    private void saveNowAsync(UUID uuid) {
        try {
            saveAll(uuid);
        } finally {
            saveQueued.remove(uuid);
        }
    }

    private void saveAll(UUID uuid) {
        try {
            witherDao.upsert(uuid, isWitherDeathMuted(uuid));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save wither sound setting for " + uuid + ": " + e.getMessage());
        }
        try {
            hideArmorDao.upsert(uuid, isArmorHidden(uuid));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save hide armor setting for " + uuid + ": " + e.getMessage());
        }
    }

    public void shutdown() {
        for (UUID uuid : saveQueued.keySet()) {
            saveAll(uuid);
        }
        saveQueued.clear();

        sql.shutdown();
    }
}