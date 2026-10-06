package com.friendlysmp.core.features.hidearmor;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemEquippable;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class ArmorDebugListener extends PacketListenerAbstract {

    private final Logger logger;
    private final PlayerSettingsStore store;
    private final HideArmorConditions conditions;

    private volatile boolean enabled;

    public ArmorDebugListener(Logger logger, PlayerSettingsStore store, HideArmorConditions conditions) {
        super(PacketListenerPriority.MONITOR);
        this.logger = logger;
        this.store = store;
        this.conditions = conditions;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!enabled) return;

        Object pObj = event.getPlayer();
        if (!(pObj instanceof Player receiver)) return;
        if (!store.isArmorHidden(receiver.getUniqueId())) return;

        PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
            if (packet.getEntityId() != receiver.getEntityId()) return;

            StringBuilder items = new StringBuilder();
            for (Equipment equipment : packet.getEquipment()) {
                items.append(' ').append(equipment.getSlot()).append('=').append(describe(equipment.getItem()));
            }
            log(event, receiver, "ENTITY_EQUIPMENT (self)" + items);
        } else if (type == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
            if (packet.getWindowId() != ArmorPacketListener.PLAYER_MENU_ID) return;

            List<ItemStack> items = packet.getItems();
            StringBuilder armor = new StringBuilder();
            for (int slot = ArmorPacketListener.MENU_ARMOR_FIRST;
                 slot <= ArmorPacketListener.MENU_ARMOR_LAST && slot < items.size(); slot++) {
                armor.append(" slot").append(slot).append('=').append(describe(items.get(slot)));
            }
            log(event, receiver, "WINDOW_ITEMS window=0" + armor);
        } else if (type == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
            boolean armorSlot = packet.getWindowId() == ArmorPacketListener.PLAYER_MENU_ID
                    && packet.getSlot() >= ArmorPacketListener.MENU_ARMOR_FIRST
                    && packet.getSlot() <= ArmorPacketListener.MENU_ARMOR_LAST;
            if (!armorSlot && !isArmor(packet.getItem())) return;

            log(event, receiver, "SET_SLOT window=" + packet.getWindowId()
                    + " slot=" + packet.getSlot() + " item=" + describe(packet.getItem()));
        } else if (type == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);
            boolean armorSlot = packet.getSlot() >= ArmorPacketListener.INVENTORY_ARMOR_FIRST
                    && packet.getSlot() <= ArmorPacketListener.INVENTORY_ARMOR_LAST;
            if (!armorSlot && !isArmor(packet.getStack())) return;

            log(event, receiver, "SET_PLAYER_INVENTORY slot=" + packet.getSlot()
                    + " item=" + describe(packet.getStack()));
        }
    }

    private void log(PacketSendEvent event, Player receiver, String details) {
        UUID uuid = receiver.getUniqueId();
        logger.info("[HideArmorDebug] to=" + receiver.getName()
                + " client=" + event.getUser().getClientVersion()
                + " hideOwn=" + conditions.hidesOwnArmor()
                + " forcedVisible=" + conditions.isForcedVisible(uuid)
                + " cancelled=" + event.isCancelled()
                + " | " + details);
    }

    private static boolean isArmor(ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        ItemEquippable equippable = item.getComponent(ComponentTypes.EQUIPPABLE).orElse(null);
        return equippable != null && ArmorPacketListener.ARMOR_SLOTS.contains(equippable.getSlot());
    }

    private static String describe(ItemStack item) {
        if (item == null || item.isEmpty()) return "empty";

        ItemEquippable equippable = item.getComponent(ComponentTypes.EQUIPPABLE).orElse(null);
        if (equippable == null) return item.getType().getName() + "[no-equippable]";

        ResourceLocation asset = equippable.getAssetId();
        if (asset == null) return item.getType().getName() + "[no-asset]";

        String state = ArmorPacketListener.isHiddenAsset(asset) ? "HIDDEN" : "VISIBLE";
        return item.getType().getName() + "[" + state + " " + asset + "]";
    }
}
