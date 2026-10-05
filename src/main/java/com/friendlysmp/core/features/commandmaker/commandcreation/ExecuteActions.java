package com.friendlysmp.core.features.commandmaker.commandcreation;

import com.friendlysmp.core.FriendlyCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ExecuteActions {
    private static final String[][] LEGACY_CODES = {
            {"0", "<black>"}, {"1", "<dark_blue>"}, {"2", "<dark_green>"}, {"3", "<dark_aqua>"},
            {"4", "<dark_red>"}, {"5", "<dark_purple>"}, {"6", "<gold>"}, {"7", "<gray>"},
            {"8", "<dark_gray>"}, {"9", "<blue>"}, {"a", "<green>"}, {"b", "<aqua>"},
            {"c", "<red>"}, {"d", "<light_purple>"}, {"e", "<yellow>"}, {"f", "<white>"},
            {"l", "<bold>"}, {"o", "<italic>"}, {"n", "<underlined>"}, {"m", "<strikethrough>"},
            {"k", "<obfuscated>"}, {"r", "<reset>"}
    };

    private final FriendlyCorePlugin plugin;

    public ExecuteActions(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void sendMessage(CommandSender sender, String action, boolean broadcast) {
        String convertedAction = convertLegacyToMiniMessage(convertURLToClickable(action));
        Component component = MiniMessage.miniMessage().deserialize(convertedAction);

        if (broadcast) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(component);
            }
        } else {
            sender.sendMessage(component);
        }
    }

    public void runCommand(CommandSender sender, String command, boolean isConsole) {
        if (isConsole) {
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        } else if (sender instanceof Player p) {
            p.getScheduler().run(plugin, t -> Bukkit.dispatchCommand(p, command), null);
        }
    }

    public void playSound(CommandSender sender, String soundName, boolean global) {
        if (!(sender instanceof Player) && !global) return;

        NamespacedKey key = NamespacedKey.fromString(soundName.toLowerCase());
        if (key == null) {
            plugin.getLogger().warning("Invalid sound key: " + soundName);
            return;
        }

        Sound sound = Registry.SOUNDS.get(key);
        if (sound == null) {
            plugin.getLogger().warning("Unknown sound: " + soundName);
            return;
        }

        if (global) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
            }
        } else {
            Player player = (Player) sender;
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    private static String convertLegacyToMiniMessage(String input) {
        for (String[] code : LEGACY_CODES) {
            input = input.replace("&" + code[0], code[1]).replace("§" + code[0], code[1]);
        }
        return input;
    }

    // [text](url) -> clickable link
    private static String convertURLToClickable(String input) {
        return input.replaceAll("\\[([^]]+)]\\(([^)]+)\\)", "<click:open_url:'$2'><hover:show_text:'$2'>$1</hover></click>");
    }
}
