package com.friendlysmp.core.features.chatpatrol.managers;

import com.friendlysmp.core.features.chatpatrol.ChatPatrolFeature;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LogManager {

    private ChatPatrolFeature plugin;
    public LogManager(ChatPatrolFeature plugin) {
        this.plugin = plugin;
    }

    public void logPunishment(Player player, String reason, String message) {
        File punishmentsFile = new File(plugin.plugin.getDataFolder(), "punishments.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(punishmentsFile, true))) {
            writer.write("Player: " + player.getName() + " | Reason: " + reason + " | Message: " + message);
            writer.newLine();
        } catch (IOException e) {
            plugin.plugin.getLogger().severe("Failed to write to punishments.txt: " + e.getMessage());
        }
    }
}
