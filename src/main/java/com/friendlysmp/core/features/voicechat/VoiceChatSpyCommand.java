package com.friendlysmp.core.features.voicechat;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public final class VoiceChatSpyCommand implements CommandExecutor, TabCompleter {
    public static final String PERMISSION = "friendlycore.voicechat.spy";

    private final VoiceChatFeature feature;
    private final PlayerSettingsStore store;

    public VoiceChatSpyCommand(VoiceChatFeature feature, PlayerSettingsStore store) {
        this.feature = feature;
        this.store = store;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(feature.message("players_only"));
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(feature.message("no_permission"));
            return true;
        }

        UUID uuid = player.getUniqueId();
        store.ensureLoadedAsync(uuid);

        String sub = args.length == 0 ? "toggle" : args[0].toLowerCase();
        switch (sub) {
            case "toggle" -> sendStatus(player, !store.toggleVcSpyDisabled(uuid));
            case "on" -> sendStatus(player, !store.setVcSpyDisabled(uuid, false));
            case "off" -> sendStatus(player, !store.setVcSpyDisabled(uuid, true));
            case "status" -> sendStatus(player, !store.isVcSpyDisabled(uuid));
            default -> player.sendMessage(feature.message("spy_usage"));
        }
        return true;
    }

    private void sendStatus(Player player, boolean spying) {
        player.sendMessage(feature.message("spy_status",
                Placeholder.component("status", feature.message(spying ? "spy_on" : "spy_off"))));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) return List.of("toggle", "on", "off", "status");
        return List.of();
    }
}
