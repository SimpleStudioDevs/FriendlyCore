package com.friendlysmp.core.features.hidearmor;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ArmorPacketListener extends PacketListenerAbstract {

    static final Set<EquipmentSlot> ARMOR_SLOTS = Set.of(
            EquipmentSlot.HELMET,
            EquipmentSlot.CHEST_PLATE,
            EquipmentSlot.LEGGINGS,
            EquipmentSlot.BOOTS
    );

    private final PlayerSettingsStore store;
    private final Map<Integer, UUID> playerEntityIds;
    private final HideArmorConditions conditions;

    public ArmorPacketListener(PlayerSettingsStore store, Map<Integer, UUID> playerEntityIds, HideArmorConditions conditions) {
        super(PacketListenerPriority.NORMAL);
        this.store = store;
        this.playerEntityIds = playerEntityIds;
        this.conditions = conditions;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_EQUIPMENT) return;

        Object pObj = event.getPlayer();
        if (!(pObj instanceof Player receiver)) return;

        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
        int entityId = packet.getEntityId();


        UUID owner = playerEntityIds.get(entityId);
        if (owner == null || !store.isArmorHidden(owner) || conditions.isForcedVisible(owner)) return;

        boolean changed = false;
        for (Equipment equipment : packet.getEquipment()) {
            ItemStack item = equipment.getItem();
            if (ARMOR_SLOTS.contains(equipment.getSlot()) && !item.isEmpty() && item.getType() != ItemTypes.ELYTRA) {
                equipment.setItem(ItemStack.EMPTY);
                changed = true;
            }
        }

        if (changed) event.markForReEncode(true);
    }
}
