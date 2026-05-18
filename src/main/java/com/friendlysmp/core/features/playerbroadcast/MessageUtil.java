package com.friendlysmp.core.features.playerbroadcast;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {
    private static Plugin plugin;
    private static MiniMessage mini;

    // &0–&9, &a–&f, &k–&o, &r
    private static final Pattern LEGACY_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    private MessageUtil() {}

    public static void init(Plugin pl, MiniMessage mm) {
        plugin = pl;
        mini = mm;
    }

    // ---- Public helpers ----

    public static Component mm(String raw) {
        String processed = convertLegacyToMiniMessage(raw);
        return mini.deserialize(processed);
    }

    public static Component mm(String raw, TagResolver... resolvers) {
        String processed = convertLegacyToMiniMessage(raw);
        if (resolvers == null || resolvers.length == 0) {
            return mini.deserialize(processed);
        }
        return mini.deserialize(processed, TagResolver.resolver(resolvers));
    }

    public static Component mmConfig(String key, TagResolver... resolvers) {
        String raw = plugin.getConfig().getString(key, "");
        String processed = convertLegacyToMiniMessage(raw);
        if (resolvers == null || resolvers.length == 0) {
            return mini.deserialize(processed);
        }
        return mini.deserialize(processed, TagResolver.resolver(resolvers));
    }

    public static void send(CommandSender sender, Component component) {
        if (sender instanceof Player player) {
            player.sendMessage(component);
        } else if (sender instanceof Audience audience) {
            audience.sendMessage(component);
        } else {
            sender.sendMessage(component.toString()); // simple fallback for console
        }
    }

    // ---- Internal: & → MiniMessage tags ----

    private static String convertLegacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        Matcher matcher = LEGACY_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            char code = Character.toLowerCase(matcher.group(1).charAt(0));
            String replacement = switch (code) {
                case '0' -> "<black>";
                case '1' -> "<dark_blue>";
                case '2' -> "<dark_green>";
                case '3' -> "<dark_aqua>";
                case '4' -> "<dark_red>";
                case '5' -> "<dark_purple>";
                case '6' -> "<gold>";
                case '7' -> "<gray>";
                case '8' -> "<dark_gray>";
                case '9' -> "<blue>";
                case 'a' -> "<green>";
                case 'b' -> "<aqua>";
                case 'c' -> "<red>";
                case 'd' -> "<light_purple>";
                case 'e' -> "<yellow>";
                case 'f' -> "<white>";
                case 'k' -> "<obfuscated>";
                case 'l' -> "<bold>";
                case 'm' -> "<strikethrough>";
                case 'n' -> "<underlined>";
                case 'o' -> "<italic>";
                case 'r' -> "<reset>";
                default -> ""; // should never hit
            };

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }
}
