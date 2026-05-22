package com.friendlysmp.core.features.zelwarnaddon;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.features.zeladdon.ZelAddonFeature;
import com.friendlysmp.core.schedulers.Schedulers;
import it.pino.zelchat.api.ZelChatAPI;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.Configuration;

import java.util.logging.Logger;

public class ZelWarnFeature {
    private final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private final ZelAddonFeature plugin;

    public ZelWarnFeature(ZelAddonFeature plugin) {
        this.plugin = plugin;
    }

    public void load
}
