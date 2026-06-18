package cn.oneachina.onmiCore.listener;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

public final class ContainerListener implements Listener {

    private static final Set<InventoryType> CONTAINER_TYPES = EnumSet.of(
            InventoryType.CHEST,
            InventoryType.ENDER_CHEST,
            InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE,
            InventoryType.SMOKER,
            InventoryType.BREWING,
            InventoryType.DISPENSER,
            InventoryType.DROPPER,
            InventoryType.HOPPER,
            InventoryType.SHULKER_BOX,
            InventoryType.BARREL
    );

    private final OnmiCore plugin;
    private final ConfigManager configManager;

    public ContainerListener() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!configManager.isLoggingContainerAccess()) return;

        Inventory top = event.getView().getTopInventory();
        if (top.getLocation() == null) return;
        if (!CONTAINER_TYPES.contains(top.getType())) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) return;

        String action;
        if (clicked.equals(top)) {
            action = "withdraw";
        } else {
            action = "deposit";
        }

        plugin.getDatabaseTask().insertContainerRecord(
                top.getLocation().getWorld().getName(),
                top.getLocation().getBlockX(),
                top.getLocation().getBlockY(),
                top.getLocation().getBlockZ(),
                event.getWhoClicked().getUniqueId().toString(),
                event.getWhoClicked().getName(),
                action,
                current.getType().getKey().toString(),
                current.getAmount(),
                null
        );
    }
}
