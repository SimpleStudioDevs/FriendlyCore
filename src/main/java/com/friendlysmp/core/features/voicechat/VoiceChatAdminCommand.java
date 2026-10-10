package com.friendlysmp.core.features.voicechat;

import de.maxhenkel.voicechat.api.Group;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VoiceChatAdminCommand implements CommandExecutor, TabCompleter {
    public static final String PERMISSION = "friendlycore.voicechat.admin";

    private final VoiceChatFeature feature;

    public VoiceChatAdminCommand(VoiceChatFeature feature) {
        this.feature = feature;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(feature.message("no_permission"));
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("password")) {
            sender.sendMessage(feature.message("admin_usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(feature.message("admin_player_not_found", Placeholder.unparsed("player", args[1])));
            return true;
        }

        Group group = feature.getGroup(target);
        if (group == null) {
            sender.sendMessage(feature.message("admin_target_not_in_group", Placeholder.unparsed("player", target.getName())));
            return true;
        }

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("group", group.getName()));

        if (!group.hasPassword()) {
            sender.sendMessage(feature.message("admin_no_password", placeholders));
            return true;
        }

        String password = feature.getGroupPassword(group);
        if (password == null) {
            sender.sendMessage(feature.message("admin_password_failed", placeholders));
            return true;
        }

        sender.sendMessage(feature.message("admin_password", placeholders, Placeholder.unparsed("password", password)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) return List.of("password");
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
