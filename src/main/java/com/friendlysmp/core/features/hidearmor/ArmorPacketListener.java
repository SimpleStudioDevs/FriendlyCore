package com.friendlysmp.core.features.hidearmor;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemEquippable;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import org.bukkit.entity.Player;

import java.util.List;
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

    static final Set<ItemType> skippedItems = Set.of(
            ItemTypes.ZOMBIE_HEAD,
            ItemTypes.PLAYER_HEAD,
            ItemTypes.SKELETON_SKULL,
            ItemTypes.WITHER_SKELETON_SKULL,
            ItemTypes.ELYTRA,
            ItemTypes.PIGLIN_HEAD,
            ItemTypes.CREEPER_HEAD,
            ItemTypes.DRAGON_HEAD
    );

    private static final String HIDDEN_NAMESPACE = "friendlycore";
    private static final String HIDDEN_PREFIX = "hidden/";

    static final int PLAYER_MENU_ID = 0;
    static final int MENU_ARMOR_FIRST = 5;
    static final int MENU_ARMOR_LAST = 8;
    static final int INVENTORY_ARMOR_FIRST = 36;
    static final int INVENTORY_ARMOR_LAST = 39;

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
        Object pObj = event.getPlayer();
        if (!(pObj instanceof Player receiver)) return;

        PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            handleEquipment(event, receiver);
        } else if (type == PacketType.Play.Server.WINDOW_ITEMS) {
            if (!shouldHideOwnFor(receiver.getUniqueId())) return;
            handleWindowItems(event);
        } else if (type == PacketType.Play.Server.SET_SLOT) {
            if (!shouldHideOwnFor(receiver.getUniqueId())) return;
            handleSetSlot(event);
        } else if (type == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            if (!shouldHideOwnFor(receiver.getUniqueId())) return;
            handleSetPlayerInventory(event);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) return;

        WrapperPlayClientCreativeInventoryAction packet = new WrapperPlayClientCreativeInventoryAction(event);
        if (restoreAsset(packet.getItemStack())) event.markForReEncode(true);
    }

    private boolean shouldHideFor(UUID owner) {
        return store.isArmorHidden(owner) && !conditions.isForcedVisible(owner);
    }

    private boolean shouldHideOwnFor(UUID owner) {
        return conditions.hidesOwnArmor() && shouldHideFor(owner);
    }

    private void handleEquipment(PacketSendEvent event, Player receiver) {
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
        int entityId = packet.getEntityId();

        if (entityId == receiver.getEntityId()) return;

        UUID owner = playerEntityIds.get(entityId);
        if (owner == null || !shouldHideFor(owner)) return;

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

    private void handleWindowItems(PacketSendEvent event) {
        WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
        if (packet.getWindowId() != PLAYER_MENU_ID) return;

        List<ItemStack> items = packet.getItems();
        boolean changed = false;
        for (int slot = MENU_ARMOR_FIRST; slot <= MENU_ARMOR_LAST && slot < items.size(); slot++) {
            changed |= hideAsset(items.get(slot));
        }

        if (changed) event.markForReEncode(true);
    }

    private void handleSetSlot(PacketSendEvent event) {
        WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
        if (packet.getWindowId() != PLAYER_MENU_ID) return;
        if (packet.getSlot() < MENU_ARMOR_FIRST || packet.getSlot() > MENU_ARMOR_LAST) return;

        if (hideAsset(packet.getItem())) event.markForReEncode(true);
    }

    private void handleSetPlayerInventory(PacketSendEvent event) {
        WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);
        if (packet.getSlot() < INVENTORY_ARMOR_FIRST || packet.getSlot() > INVENTORY_ARMOR_LAST) return;

        if (hideAsset(packet.getStack())) event.markForReEncode(true);
    }

    private static boolean hideAsset(ItemStack item) {
        if (item == null || item.isEmpty() || skippedItems.contains(item.getType())) return false;

        ItemEquippable equippable = item.getComponent(ComponentTypes.EQUIPPABLE).orElse(null);
        if (equippable == null || !ARMOR_SLOTS.contains(equippable.getSlot())) return false;

        ResourceLocation asset = equippable.getAssetId();
        if (asset == null || isHiddenAsset(asset)) return false;

        ResourceLocation hidden = new ResourceLocation(HIDDEN_NAMESPACE,
                HIDDEN_PREFIX + asset.getNamespace() + "/" + asset.getKey());
        item.setComponent(ComponentTypes.EQUIPPABLE, withAsset(equippable, hidden));
        return true;
    }

    private static boolean restoreAsset(ItemStack item) {
        if (item == null || item.isEmpty()) return false;

        ItemEquippable equippable = item.getComponent(ComponentTypes.EQUIPPABLE).orElse(null);
        if (equippable == null) return false;

        ResourceLocation asset = equippable.getAssetId();
        if (asset == null || !isHiddenAsset(asset)) return false;

        String original = asset.getKey().substring(HIDDEN_PREFIX.length());
        int split = original.indexOf('/');
        if (split <= 0) return false;

        ResourceLocation restored = new ResourceLocation(original.substring(0, split), original.substring(split + 1));
        item.setComponent(ComponentTypes.EQUIPPABLE, withAsset(equippable, restored));
        return true;
    }

    static boolean isHiddenAsset(ResourceLocation asset) {
        return HIDDEN_NAMESPACE.equals(asset.getNamespace()) && asset.getKey().startsWith(HIDDEN_PREFIX);
    }

    private static ItemEquippable withAsset(ItemEquippable source, ResourceLocation asset) {
        return new ItemEquippable(
                source.getSlot(),
                source.getEquipSound(),
                asset,
                source.getCameraOverlay(),
                source.getAllowedEntities(),
                source.isDispensable(),
                source.isSwappable(),
                source.isDamageOnHurt(),
                source.isEquipOnInteract(),
                source.isCanBeSheared(),
                source.getShearingSound());
    }
}
