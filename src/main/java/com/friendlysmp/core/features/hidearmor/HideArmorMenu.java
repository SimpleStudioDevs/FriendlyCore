package com.friendlysmp.core.features.hidearmor;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class HideArmorMenu implements InventoryHolder {
    static final int MASTER_SLOT = 4;
    static final Map<ArmorPiece, Integer> PIECE_SLOTS = Map.of(
            ArmorPiece.HELMET, 10,
            ArmorPiece.CHESTPLATE, 11,
            ArmorPiece.LEGGINGS, 12,
            ArmorPiece.BOOTS, 13,
            ArmorPiece.HEADS, 15,
            ArmorPiece.ELYTRA, 16
    );
    private static final int STATUS_ROW_OFFSET = 9;

    private final Player player;
    private final PlayerSettingsStore store;
    private final Consumer<Player> refreshViewers;
    private final Inventory inv;
    private boolean changed;

    public HideArmorMenu(Player player, PlayerSettingsStore store, Consumer<Player> refreshViewers) {
        this.player = player;
        this.store = store;
        this.refreshViewers = refreshViewers;
        this.inv = Bukkit.createInventory(this, 27, Component.text("Hide Armor", NamedTextColor.DARK_GRAY));
        render();
    }

    public void open() {
        player.openInventory(inv);
    }

    void click(int slot) {
        UUID uuid = player.getUniqueId();
        if (slot == MASTER_SLOT) {
            store.toggleArmorHidden(uuid);
        } else {
            ArmorPiece piece = pieceAt(slot);
            if (piece == null) return;
            store.setHiddenArmorPieces(uuid, piece.toggle(store.getHiddenArmorPieces(uuid)));
        }
        changed = true;
        refreshViewers.accept(player);
        render();
    }

    boolean hasChanged() {
        return changed;
    }

    Player player() {
        return player;
    }

    private ArmorPiece pieceAt(int slot) {
        for (Map.Entry<ArmorPiece, Integer> entry : PIECE_SLOTS.entrySet()) {
            int pieceSlot = entry.getValue();
            if (slot == pieceSlot || slot == pieceSlot + STATUS_ROW_OFFSET) return entry.getKey();
        }
        return null;
    }

    private void render() {
        UUID uuid = player.getUniqueId();
        boolean enabled = store.isArmorHidden(uuid);
        int mask = store.getHiddenArmorPieces(uuid);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        inv.setItem(MASTER_SLOT, item(
                enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                text("Hide Armor: ", NamedTextColor.WHITE).append(enabled
                        ? text("ON", NamedTextColor.GREEN)
                        : text("OFF", NamedTextColor.RED)),
                List.of(text("Hides the pieces selected below", NamedTextColor.GRAY),
                        text("from other players.", NamedTextColor.GRAY),
                        Component.empty(),
                        text("Click to toggle", NamedTextColor.YELLOW))));

        for (Map.Entry<ArmorPiece, Integer> entry : PIECE_SLOTS.entrySet()) {
            ArmorPiece piece = entry.getKey();
            boolean hidden = piece.isHidden(mask);
            Component state = hidden ? text("Hidden", NamedTextColor.RED) : text("Visible", NamedTextColor.GREEN);

            List<Component> lore = new ArrayList<>();
            lore.add(text("Status: ", NamedTextColor.GRAY).append(state));
            if (!enabled) lore.add(text("Hide Armor is currently off", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(text("Click to toggle", NamedTextColor.YELLOW));

            inv.setItem(entry.getValue(), item(piece.icon(), text(piece.displayName(), NamedTextColor.WHITE), lore));
            inv.setItem(entry.getValue() + STATUS_ROW_OFFSET, item(
                    hidden ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE,
                    state, lore));
        }
    }

    private static Component text(String content, NamedTextColor color) {
        return Component.text(content, color).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public @NonNull Inventory getInventory() {
        return inv;
    }
}
