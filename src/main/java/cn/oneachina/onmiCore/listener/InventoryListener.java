package cn.oneachina.onmiCore.listener;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class InventoryListener implements Listener {

    private final OnmiCore plugin;
    private final ConfigManager configManager;

    public InventoryListener() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!configManager.isLoggingInventoryChange()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        plugin.getDatabaseTask().insertInventoryRecord(
                player.getUniqueId().toString(),
                player.getName(),
                "pickup",
                event.getItem().getItemStack().getType().getKey().toString(),
                event.getItem().getItemStack().getAmount(),
                null
        );
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!configManager.isLoggingInventoryChange()) return;

        Player player = event.getPlayer();

        plugin.getDatabaseTask().insertInventoryRecord(
                player.getUniqueId().toString(),
                player.getName(),
                "drop",
                event.getItemDrop().getItemStack().getType().getKey().toString(),
                event.getItemDrop().getItemStack().getAmount(),
                null
        );
    }
}
