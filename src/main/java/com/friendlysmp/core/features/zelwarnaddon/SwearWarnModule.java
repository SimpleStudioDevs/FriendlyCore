package com.friendlysmp.core.features.zelwarnaddon;

import it.pino.zelchat.api.message.ChatMessage;
import it.pino.zelchat.api.message.infraction.ChatInfraction;
import it.pino.zelchat.api.module.ChatModule;
import it.pino.zelchat.api.module.annotation.ChatModuleSettings;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

@ChatModuleSettings(pluginOwner = "ZelChatWarnAddon")
public class SwearWarnModule implements ChatModule {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private final ZelWarnFeature plugin;

    public SwearWarnModule(ZelWarnFeature plugin) { this.plugin = plugin; }

    @Override
    public void load() {plugin.getLogger().info("SwearWarnModule loaded."); }

    @Override
    public void handleChatMessage(@NotNull ChatMessage chatMessage) {
        ChatInfraction infraction = chatMessage.getInfraction();
        if (infraction == null) return;

        String raw = chatMessage.getRawMessage();
        String finalMessage = PLAIN.serialize(chatMessage.getMessage());

        if (raw.equals(finalMessage)) return;

        String words = String.join(", ", infraction.getFlaggedRules().keySet());

        String command = plugin.getConfig()
                .getString("zel-warn-addon.COMMAND", "say {player} swore: {words}")
                .replace("{player}", chatMessage.getBukkitPlayer().getName())
                .replace("{message}", raw)
                .replace("{words}", words);

        plugin.getSchedulers().global(() -> {
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            if (plugin.getConfig().getBoolean("features.zel-warn-addon.debug", false)) {
                plugin.getLogger().info("Executed command: " + command + " | success=" + success);
            }
        });

    }
}
