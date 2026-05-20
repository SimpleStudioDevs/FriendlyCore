package com.friendlysmp.core.features.chatpatrol.managers;

import com.friendlysmp.core.features.chatpatrol.ChatPatrolFeature;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PunishmentManager {
    private final ChatPatrolFeature plugin;

    public PunishmentManager(ChatPatrolFeature plugin) {
        this.plugin =  plugin;
    }
    public void executeCommand(String command, Player player) {
        if (command == null || command.isBlank()) return;

        String finalCommand = command.replace("{player}", player.getName());
        plugin.getSchedulers().global(() ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));

     }
}
