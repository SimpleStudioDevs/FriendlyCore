package com.friendlysmp.core.features.hidearmor;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import org.bukkit.Material;

import java.util.Set;

public enum ArmorPiece {
    HELMET("Helmet", Material.DIAMOND_HELMET),
    CHESTPLATE("Chestplate", Material.DIAMOND_CHESTPLATE),
    LEGGINGS("Leggings", Material.DIAMOND_LEGGINGS),
    BOOTS("Boots", Material.DIAMOND_BOOTS),
    HEADS("Heads & Skulls", Material.PLAYER_HEAD),
    ELYTRA("Elytra", Material.ELYTRA);

    static final Set<ItemType> HEAD_ITEMS = Set.of(
            ItemTypes.ZOMBIE_HEAD,
            ItemTypes.PLAYER_HEAD,
            ItemTypes.SKELETON_SKULL,
            ItemTypes.WITHER_SKELETON_SKULL,
            ItemTypes.PIGLIN_HEAD,
            ItemTypes.CREEPER_HEAD,
            ItemTypes.DRAGON_HEAD
    );

    private final String displayName;
    private final Material icon;

    ArmorPiece(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public boolean isHidden(int mask) {
        return (mask & bit()) != 0;
    }

    public int toggle(int mask) {
        return mask ^ bit();
    }

    static ArmorPiece of(EquipmentSlot slot, ItemType type) {
        if (type == ItemTypes.ELYTRA) return ELYTRA;
        if (HEAD_ITEMS.contains(type)) return HEADS;
        return switch (slot) {
            case HELMET -> HELMET;
            case CHEST_PLATE -> CHESTPLATE;
            case LEGGINGS -> LEGGINGS;
            case BOOTS -> BOOTS;
            default -> null;
        };
    }
}
