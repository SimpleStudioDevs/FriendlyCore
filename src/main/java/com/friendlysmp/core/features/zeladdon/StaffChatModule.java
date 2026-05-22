package com.friendlysmp.core.features.zeladdon;


import it.pino.zelchat.api.message.ChatMessage;
import it.pino.zelchat.api.message.channel.ChannelType;
import it.pino.zelchat.api.message.state.MessageState;
import it.pino.zelchat.api.module.ChatModule;
import it.pino.zelchat.api.module.annotation.ChatModuleSettings;
import it.pino.zelchat.api.module.priority.ModulePriority;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@ChatModuleSettings(pluginOwner = "ZelChatAddon", priority = ModulePriority.NORMAL)
public class StaffChatModule implements ChatModule {
    private final Map<String, String> formatPrefixes;

    public StaffChatModule(Map<String, String> formatPrefixes) {
        this.formatPrefixes = formatPrefixes;
    }

    @Override
    public void handleChatMessage(@NotNull ChatMessage chatMessage) {
        if (!chatMessage.getChannel().getType().equals(ChannelType.STAFF)) return;
        if (chatMessage.getState() == MessageState.CANCELLED || chatMessage.getState() == MessageState.FILTERED_CANCELLED) return;

        Player sender = chatMessage.getBukkitPlayer();

        for (Map.Entry<String, String> entry : formatPrefixes.entrySet()) {
            String permKey = entry.getKey();
            String prefixStr = entry.getValue();

            if (sender.hasPermission("zelchat.format." + permKey)) {
                String resolvedPrefix = PlaceholderAPI.setPlaceholders(sender, prefixStr);
                Component prefix = MiniMessage.miniMessage().deserialize(resolvedPrefix);
                Component newMessage = prefix.append(chatMessage.getMessage());
                chatMessage.setMessage(newMessage);
                break;
            }
        }
    }
}
