package com.friendlysmp.core.features.voicechat;

import com.friendlysmp.core.FriendlyCorePlugin;
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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceChatFeature extends Feature implements VoicechatPlugin, Listener {

    private final Schedulers schedulers;
    private final PlayerSettingsStore playerSettings;
    private final ChatPatrolFeature chatPatrol;
    private final ZelAddonFeature zelAddon;

    private volatile VoicechatServerApi serverApi;
    private volatile boolean enabled;
    private volatile List<String> swearWords = List.of();
    private final Set<UUID> toggled = ConcurrentHashMap.newKeySet();
    private boolean registered;

    public VoiceChatFeature(FriendlyCorePlugin plugin, Schedulers schedulers, PlayerSettingsStore playerSettings, ChatPatrolFeature chatPatrol, ZelAddonFeature zelAddon) {
        super(plugin);
        this.schedulers = schedulers;
        this.playerSettings = playerSettings;
        this.chatPatrol = chatPatrol;
        this.zelAddon = zelAddon;
    }

    @Override
    public String id() {
        return "voicechat";
    }

    @Override
    public void enable() {
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
            VoiceChatSpyCommand exec = new VoiceChatSpyCommand(playerSettings);
            spyCommand.setExecutor(exec);
            spyCommand.setTabCompleter(exec);
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
        loadSwearWords();
    }

    private void loadSwearWords() {
        File file = new File(plugin.getDataFolder().getParentFile(), "ZelChat/filter/filter-rules.yml");
        if (!file.exists()) {
            swearWords = List.of();
            plugin.getLogger().warning("[VoiceChat] ZelChat filter file not found at " + file.getPath());
            return;
        }

        List<String> words = new ArrayList<>();
        for (String word : YamlConfiguration.loadConfiguration(file).getStringList("rules.swearing.words")) {
            if (!word.isBlank()) words.add(word.toLowerCase(Locale.ROOT));
        }
        swearWords = List.copyOf(words);
        plugin.getLogger().info("[VoiceChat] Loaded " + swearWords.size() + " swear words from ZelChat.");
    }

    public boolean moderate(Player player, String message, boolean checkChatPatrol) {
        if (player.hasPermission("zelchat.staff")) return true;
        if (checkChatPatrol && chatPatrol.punishIfBlacklisted(player, message, "Blacklisted Word In VoiceChat")) return false;

        String lowered = message.toLowerCase(Locale.ROOT);
        List<String> flagged = new ArrayList<>();
        for (String word : swearWords) {
            if (lowered.contains(word)) flagged.add(word);
        }
        if (flagged.isEmpty()) return true;

        String template = zelAddon.getConfig().getString("COMMAND");
        if (template != null && !template.isBlank()) {
            String command = template
                    .replace("{player}", player.getName())
                    .replace("{message}", message)
                    .replace("{words}", String.join(", ", flagged));
            schedulers.global(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
        return false;
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
                player.sendMessage(Component.text("You are no longer in a VoiceChat group, so /vc toggle was turned off. Your message was not sent.", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("You are not in a VoiceChat group!", NamedTextColor.RED));
            }
            return;
        }

        if (!moderate(player, text, checkChatPatrol)) {
            player.sendMessage(Component.text("Your message was blocked for inappropriate language.", NamedTextColor.RED));
            return;
        }

        Component message = MiniMessage.miniMessage().deserialize("<gray>[<group>] <light_purple><player>: <white><message>",
                Placeholder.unparsed("group", group.getName()),
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("message", text));

        List<Player> members = getPlayersInGroup(group.getId());
        for (Player p : members) {
            p.sendMessage(message);
        }

        Component spyMessage = MiniMessage.miniMessage().deserialize("<dark_gray>[VC Spy] <gray>[<group>] <player>: <message>",
                Placeholder.unparsed("group", group.getName()),
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("message", text));

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (isSpying(p) && !members.contains(p)) p.sendMessage(spyMessage);
        }
        plugin.getServer().getConsoleSender().sendMessage(spyMessage);
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

    public List<Player> getGroupMembers(Player player) {
        Group group = getGroup(player);
        return group == null ? new ArrayList<>() : getPlayersInGroup(group.getId());
    }
}
