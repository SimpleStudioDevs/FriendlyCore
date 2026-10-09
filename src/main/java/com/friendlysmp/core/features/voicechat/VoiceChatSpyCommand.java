package com.friendlysmp.core.features.voicechat;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private final PlayerSettingsStore store;

    public VoiceChatSpyCommand(PlayerSettingsStore store) {
        this.store = store;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("You must be a player to use this command!", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
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
            default -> player.sendMessage(Component.text("Usage: /vcspy [toggle|on|off|status]", NamedTextColor.RED));
        }
        return true;
    }

    private void sendStatus(Player player, boolean spying) {
        player.sendMessage(Component.text("VoiceChat spy: ", NamedTextColor.GRAY)
                .append(spying ? Component.text("ON", NamedTextColor.GREEN) : Component.text("OFF", NamedTextColor.RED)));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) return List.of("toggle", "on", "off", "status");
        return List.of();
    }
}
