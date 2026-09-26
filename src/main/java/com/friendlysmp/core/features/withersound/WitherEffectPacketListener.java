package com.friendlysmp.core.features.withersound;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEffect;
import org.bukkit.entity.Player;

public final class WitherEffectPacketListener extends PacketListenerAbstract {

    // ProtocolLib example: 1013 = wither "world event" sound
    private static final int WITHER_GLOBAL_EFFECT_ID = 1023;
    private static final int DRAGON_GLOBAL_EFFECT_ID = 1028;

    private final PlayerSettingsStore store;
    private final boolean debug;

    public WitherEffectPacketListener(PlayerSettingsStore store, boolean debug) {
        super(PacketListenerPriority.NORMAL);
        this.store = store;
        this.debug = debug;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Object pObj = event.getPlayer();
        if (!(pObj instanceof Player receiver)) return;

        if (event.getPacketType() != PacketType.Play.Server.EFFECT) return;

        WrapperPlayServerEffect packet = new WrapperPlayServerEffect(event);
        int type = packet.getType();

        // Debug: show EFFECT packets so you can confirm which id is the gong
        if (debug) {
            System.out.println("[EffectTrace] recv=" + receiver.getName()
                    + " type=" + type
                    + " global=" + packet.isGlobalEvent());
        }

        // Per-player toggle (your existing setting name)
        store.ensureLoadedAsync(receiver.getUniqueId());
        if (!store.isWitherDeathMuted(receiver.getUniqueId())) return;

        // Cancel the gong world event
        if (type == WITHER_GLOBAL_EFFECT_ID
        || type == DRAGON_GLOBAL_EFFECT_ID) {
            event.setCancelled(true);
        }
    }
}