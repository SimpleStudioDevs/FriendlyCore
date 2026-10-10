package com.friendlysmp.core.features.voicechat;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.features.chatpatrol.ChatPatrolFeature;
import com.friendlysmp.core.features.zeladdon.ZelAddonFeature;
import com.friendlysmp.core.schedulers.Schedulers;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class VoiceChatFeature extends Feature implements VoicechatPlugin, Listener {

    private final Schedulers schedulers;
    private final PlayerSettingsStore playerSettings;
    private final ChatPatrolFeature chatPatrol;
    private final ZelAddonFeature zelAddon;
    private final FeatureConfig config;

    private volatile VoicechatServerApi serverApi;
    private volatile boolean enabled;
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final List<String> SUFFIXES = List.of("s", "es", "ed", "er", "ing");

    private volatile Set<String> swearSingles = Set.of();
    private volatile List<String> swearPhrases = List.of();
    private final Set<UUID> toggled = ConcurrentHashMap.newKeySet();
    private boolean registered;
    private volatile Method groupImplGetGroup;
    private volatile Method serverGroupGetPassword;

    public VoiceChatFeature(FriendlyCorePlugin plugin, Schedulers schedulers, PlayerSettingsStore playerSettings, ChatPatrolFeature chatPatrol, ZelAddonFeature zelAddon) {
        super(plugin);
        this.schedulers = schedulers;
        this.playerSettings = playerSettings;
        this.chatPatrol = chatPatrol;
        this.zelAddon = zelAddon;
        this.config = new FeatureConfig(plugin, "FeatureConfigs/voicechat.yml");
    }

    @Override
    public String id() {
        return "voicechat";
    }

    @Override
    public void enable() {
        config.load();
        if (!plugin.getServer().getPluginManager().isPluginEnabled("voicechat")) {
            plugin.getLogger().info("[VoiceChat] Simple Voice Chat is not enabled!");
            return;
        }

        if (!registered) {
            BukkitVoicechatService service = plugin.getServer().getServicesManager().load(BukkitVoicechatService.class);
            if (service == null) {
                plugin.getLogger().warning("[VoiceChat] BukkitVoicechatService is not available.");
                return;
            }
            service.registerPlugin(this);
            registered = true;
        }

        loadSwearWords();
        enabled = true;

        PluginCommand vcCommand = plugin.getCommand("voicechat");
        if (vcCommand != null) {
            VoiceChatCommand exec = new VoiceChatCommand(this);
            vcCommand.setExecutor(exec);
            vcCommand.setTabCompleter(exec);
        }

        PluginCommand spyCommand = plugin.getCommand("vcspy");
        if (spyCommand != null) {
            VoiceChatSpyCommand exec = new VoiceChatSpyCommand(this, playerSettings);
            spyCommand.setExecutor(exec);
            spyCommand.setTabCompleter(exec);
        }

        PluginCommand adminCommand = plugin.getCommand("vcadmin");
        if (adminCommand != null) {
            VoiceChatAdminCommand exec = new VoiceChatAdminCommand(this);
            adminCommand.setExecutor(exec);
            adminCommand.setTabCompleter(exec);
        }

        registerListener(this);
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            playerSettings.ensureLoadedAsync(online.getUniqueId());
        }
    }

    @Override
    public void disable() {
        enabled = false;
        toggled.clear();
    }

    @Override
    public void reload() {
        config.load();
        loadSwearWords();
    }

    private void loadSwearWords() {
        File file = new File(plugin.getDataFolder().getParentFile(), "ZelChat/filter/filter-rules.yml");
        if (!file.exists()) {
            swearSingles = Set.of();
            swearPhrases = List.of();
            plugin.getLogger().warning("[VoiceChat] ZelChat filter file not found at " + file.getPath());
            return;
        }

        Set<String> singles = new HashSet<>();
        List<String> phrases = new ArrayList<>();
        for (String word : YamlConfiguration.loadConfiguration(file).getStringList("rules.swearing.words")) {
            String normalized = normalize(word);
            if (normalized.isEmpty()) continue;
            if (normalized.contains(" ")) phrases.add(normalized);
            else singles.add(normalized);
        }
        swearSingles = Set.copyOf(singles);
        swearPhrases = List.copyOf(phrases);
        plugin.getLogger().info("[VoiceChat] Loaded " + (singles.size() + phrases.size()) + " swear words from ZelChat.");
    }

    public boolean moderate(Player player, String message, boolean checkChatPatrol) {
        if (player.hasPermission("zelchat.staff")) return true;
        if (checkChatPatrol && chatPatrol.punishIfBlacklisted(player, message, "Blacklisted Word In VoiceChat")) return false;

        if (!containsSwear(message)) return true;

        plugin.getLogger().info("Swear: " + message);
        return false;
    }

    private boolean containsSwear(String message) {
        String normalized = normalize(message);
        if (normalized.isEmpty()) return false;

        Set<String> singles = swearSingles;
        for (String token : normalized.split(" ")) {
            if (singles.contains(token)) return true;
            for (String suffix : SUFFIXES) {
                if (token.length() > suffix.length() + 1 && token.endsWith(suffix)
                        && singles.contains(token.substring(0, token.length() - suffix.length()))) {
                    return true;
                }
            }
        }

        String padded = " " + normalized + " ";
        for (String phrase : swearPhrases) {
            if (padded.contains(" " + phrase + " ")) return true;
        }
        return false;
    }

    private static String normalize(String text) {
        return NON_ALPHANUMERIC.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    @Override
    public String getPluginId() {
        return plugin.getName();
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi server) serverApi = server;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> serverApi = event.getVoicechat());
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> serverApi = null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerSettings.ensureLoadedAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        toggled.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!toggled.contains(player.getUniqueId())) return;
        event.setCancelled(true);
        sendGroupMessage(player, event.getMessage(), false);
    }

    public Component message(String key, TagResolver... resolvers) {
        return MiniMessage.miniMessage().deserialize(config.get().getString("messages." + key, ""), resolvers);
    }

    public boolean isToggled(Player player) {
        return toggled.contains(player.getUniqueId());
    }

    public void setToggled(Player player, boolean value) {
        if (value) toggled.add(player.getUniqueId());
        else toggled.remove(player.getUniqueId());
    }

    public void sendGroupMessage(Player player, String text, boolean checkChatPatrol) {
        Group group = getGroup(player);
        if (group == null) {
            if (toggled.remove(player.getUniqueId())) {
                player.sendMessage(message("toggle_group_left"));
            } else {
                player.sendMessage(message("not_in_group"));
            }
            return;
        }

        if (!moderate(player, text, checkChatPatrol)) {
            player.sendMessage(message("blocked"));
            return;
        }

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed("group", group.getName()),
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("message", text));
        Component groupMessage = message("format", placeholders);

        List<Player> members = getPlayersInGroup(group.getId());
        for (Player p : members) {
            p.sendMessage(groupMessage);
        }

        Component spyMessage = message("spy_format", placeholders);

        if (!player.hasPermission("zelchat.staff")) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (isSpying(p) && !members.contains(p)) p.sendMessage(spyMessage);
            }
            plugin.getServer().getConsoleSender().sendMessage(spyMessage);
        }

    }

    public boolean isSpying(Player player) {
        return player.hasPermission(VoiceChatSpyCommand.PERMISSION) && !playerSettings.isVcSpyDisabled(player.getUniqueId());
    }

    public boolean isAvailable() {
        return enabled && serverApi != null;
    }

    @Nullable
    public Group getGroup(Player player) {
        return getGroup(player.getUniqueId());
    }

    @Nullable
    public Group getGroup(UUID playerId) {
        VoicechatServerApi api = serverApi;
        if (!enabled || api == null) return null;
        VoicechatConnection connection = api.getConnectionOf(playerId);
        return connection == null ? null : connection.getGroup();
    }

    public List<Player> getPlayersInGroup(UUID groupId) {
        List<Player> members = new ArrayList<>();
        VoicechatServerApi api = serverApi;
        if (!enabled || api == null) return members;
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            VoicechatConnection connection = api.getConnectionOf(online.getUniqueId());
            if (connection == null) continue;
            Group group = connection.getGroup();
            if (group != null && group.getId().equals(groupId)) members.add(online);
        }
        return members;
    }

    @Nullable
    public String getGroupPassword(Group group) {
        try {
            Method getGroup = groupImplGetGroup;
            if (getGroup == null || !getGroup.getDeclaringClass().isInstance(group)) {
                getGroup = group.getClass().getMethod("getGroup");
                groupImplGetGroup = getGroup;
            }
            Object serverGroup = getGroup.invoke(group);
            if (serverGroup == null) return null;

            Method getPassword = serverGroupGetPassword;
            if (getPassword == null || !getPassword.getDeclaringClass().isInstance(serverGroup)) {
                getPassword = serverGroup.getClass().getMethod("getPassword");
                serverGroupGetPassword = getPassword;
            }
            Object password = getPassword.invoke(serverGroup);
            return password instanceof String s ? s : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            plugin.getLogger().warning("[VoiceChat] Could not read group password via reflection: " + e);
            return null;
        }
    }

    public List<Player> getGroupMembers(Player player) {
        Group group = getGroup(player);
        return group == null ? new ArrayList<>() : getPlayersInGroup(group.getId());
    }
}
