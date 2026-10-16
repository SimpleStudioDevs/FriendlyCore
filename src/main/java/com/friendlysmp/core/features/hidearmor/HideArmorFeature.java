package com.friendlysmp.core.features.hidearmor;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.config.FeatureConfig;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.placeholder.FriendlyCoreExpansion;
import com.friendlysmp.core.placeholder.PlaceholderProvider;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HideArmorFeature extends Feature implements PlaceholderProvider {

    private final PlayerSettingsStore store;
    private final FeatureConfig config;
    private final Map<Integer, UUID> playerEntityIds = new ConcurrentHashMap<>();

    private HideArmorConditions conditions;
    private ArmorPacketListener registered;
    private ArmorDebugListener debugListener;

    public HideArmorFeature(FriendlyCorePlugin plugin, PlayerSettingsStore store) {
        super(plugin);
        this.store = store;
        this.config = new FeatureConfig(plugin, "FeatureConfigs/hidearmor.yml");
    }

    @Override
    public String id() {
        return "hidearmor";
    }

    @Override
    public void enable() {
        config.load();
        conditions = new HideArmorConditions(plugin, store, this::refreshViewers);
        debugListener = new ArmorDebugListener(plugin.getLogger(), store, conditions);
        applyConfig();

        PluginCommand cmd = plugin.getCommand("hidearmor");
        if (cmd != null) {
            HideArmorCommand exec = new HideArmorCommand(store, this::refreshViewers, this::refreshHiddenArmorFor, conditions);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            playerEntityIds.put(online.getEntityId(), online.getUniqueId());
            store.ensureLoadedAsync(online.getUniqueId());
        }
        registerListener(new HideArmorPlayerListener(plugin, store, playerEntityIds, conditions));
        registerListener(conditions);
        registerListener(new HideArmorMenuListener(plugin));

        registered = new ArmorPacketListener(store, playerEntityIds, conditions);
        PacketEvents.getAPI().getEventManager().registerListener(registered);
        PacketEvents.getAPI().getEventManager().registerListener(debugListener);
    }

    private void applyConfig() {
        conditions.configure(
                config.get().getLong("combat-tag-seconds", 15L),
                config.get().getStringList("disabled-worlds"),
                config.get().getBoolean("hide-own-armor", true));
        debugListener.setEnabled(config.get().getBoolean("debug", false));
    }

    private void refreshViewers(Player target) {
        target.updateInventory();

        for (Player viewer : target.getWorld().getPlayers()) {
            if (viewer.equals(target) || !viewer.canSee(target)) continue;
            sendArmor(target, viewer);
        }
    }

    private void refreshHiddenArmorFor(Player viewer) {
        for (Player target : viewer.getWorld().getPlayers()) {
            if (target.equals(viewer) || !viewer.canSee(target)) continue;
            if (!store.isArmorHidden(target.getUniqueId())) continue;
            target.getScheduler().run(plugin, task -> sendArmor(target, viewer), null);
        }
    }

    private void sendArmor(Player target, Player viewer) {
        PlayerInventory inv = target.getInventory();
        List<Equipment> armor = List.of(
                new Equipment(EquipmentSlot.HELMET, toPacketItem(inv.getHelmet())),
                new Equipment(EquipmentSlot.CHEST_PLATE, toPacketItem(inv.getChestplate())),
                new Equipment(EquipmentSlot.LEGGINGS, toPacketItem(inv.getLeggings())),
                new Equipment(EquipmentSlot.BOOTS, toPacketItem(inv.getBoots()))
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerEntityEquipment(target.getEntityId(), armor));
    }

    private static ItemStack toPacketItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) return ItemStack.EMPTY;
        return SpigotConversionUtil.fromBukkitItemStack(item);
    }

    @Override
    public void registerPlaceholders(FriendlyCoreExpansion expansion) {
        expansion.registerHandler("hidearmor", (player, args) -> {
            boolean seeAll = args.length > 0 && args[0].equalsIgnoreCase("seeall");
            String[] rest = seeAll ? java.util.Arrays.copyOfRange(args, 1, args.length) : args;

            boolean on = seeAll
                    ? store.isSeeingAllArmor(player.getUniqueId())
                    : store.isArmorHidden(player.getUniqueId());
            if (rest.length > 0 && rest[0].equalsIgnoreCase("colored")) return on ? "§aON" : "§cOFF";
            return on ? "ON" : "OFF";
        });
    }

    @Override
    public void disable() {
        if (registered != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(registered);
            registered = null;
        }
        if (debugListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(debugListener);
            debugListener = null;
        }
        playerEntityIds.clear();
    }

    @Override
    public void reload() {
        config.load();
        if (conditions != null) applyConfig();
    }
}
