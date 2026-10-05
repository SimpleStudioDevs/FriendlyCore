package com.friendlysmp.core.features.zeladdon;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.placeholder.FriendlyCoreExpansion;
import com.friendlysmp.core.placeholder.PlaceholderProvider;
import com.friendlysmp.core.schedulers.Schedulers;
import it.pino.zelchat.api.ZelChatAPI;
import org.bukkit.Statistic;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ZelAddonFeature extends Feature implements PlaceholderProvider {
    private StaffChatModule staffChatModule;
    private SwearWarnModule swearWarnModule;
    private final Schedulers schedulers;
    private final FeatureConfig config;
    private SpyMsgPersist spyMsgPersist;

    public ZelAddonFeature(FriendlyCorePlugin plugin, Schedulers schedulers) {
        super(plugin);
        this.schedulers = schedulers;
        this.config = new FeatureConfig(plugin, "FeatureConfigs/zeladdon.yml");
    }

    @Override
    public String id() {
        return "zel-addon";
    }

    @Override
    public void enable() {
        config.load();

        swearWarnModule =  new SwearWarnModule(this);
        staffChatModule = new StaffChatModule(loadFormats());

        this.getLogger().info("About to register module...");
        ZelChatAPI.get().getModuleManager().register(this.plugin, this.swearWarnModule);
        ZelChatAPI.get().getModuleManager().register(this.plugin, this.staffChatModule);
        this.getLogger().info("Module register call finished.");

        // SpyMsg Listener
        syncSpyMsgListener();
    }

    @Override
    public void disable() {
        if (swearWarnModule != null) {
            ZelChatAPI.get().getModuleManager().unregister(plugin, swearWarnModule);
        }
        if (staffChatModule != null) {
            ZelChatAPI.get().getModuleManager().unregister(plugin, staffChatModule);
        }
        spyMsgPersist = null;
    }

    @Override
    public void reload() {
        config.load();
        syncSpyMsgListener();
    }

    private void syncSpyMsgListener() {
        boolean enabled = config.get().getBoolean("spy-msg");
        if (enabled && spyMsgPersist == null) {
            spyMsgPersist = new SpyMsgPersist(this, plugin);
            registerListener(spyMsgPersist);
        } else if (!enabled && spyMsgPersist != null) {
            unregisterListener(spyMsgPersist);
            spyMsgPersist = null;
        }
    }

    private Map<String, String> loadFormats() {
        ConfigurationSection section = config.get().getConfigurationSection("FORMATS");
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
        return config.get();
    }

    public final Logger getLogger() {
        return plugin.getLogger();
    }


    @Override
    public void registerPlaceholders(FriendlyCoreExpansion expansion) {
        expansion.registerHandler("zel", (player, args) -> {
            if (args.length == 0) return "";
            if (args[0].equalsIgnoreCase("playtime")) return formatPlaytime(player.getStatistic(Statistic.PLAY_ONE_MINUTE));
            return "";
        });
    }

    private static final long[] UNIT_MINUTES = {365L * 24 * 60, 30L * 24 * 60, 24 * 60, 60};
    private static final String[] UNIT_NAMES = {"year", "month", "day", "hour"};

    public String formatPlaytime(int playtime)  {
        long minutes = playtime / 20L / 60L;
        if (minutes < 60) return minutes + (minutes == 1 ? " minute" : " minutes");

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < UNIT_MINUTES.length; i++) {
            long amount = minutes / UNIT_MINUTES[i];
            if (amount == 0) continue;
            minutes %= UNIT_MINUTES[i];

            if (!out.isEmpty()) out.append(", ");
            out.append(amount).append(' ').append(UNIT_NAMES[i]);
            if (amount != 1) out.append('s');
        }
        return out.toString();
    }
}
