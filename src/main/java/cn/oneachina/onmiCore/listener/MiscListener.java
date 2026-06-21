package cn.oneachina.onmiCore.listener;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiscListener implements Listener {

    private final OnmiCore plugin;
    private final ConfigManager configManager;

    public MiscListener() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!configManager.isLoggingPlayerDeath()) return;

        Player player = event.getPlayer();
        String deathMsg = event.deathMessage() != null ? event.deathMessage().toString() : "";
        plugin.getDatabaseTask().insertBlockRecord(
                player.getWorld().getName(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ(),
                player.getUniqueId().toString(),
                player.getName(),
                "death",
                deathMsg,
                null,
                null,
                null
        );
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!configManager.isLoggingEntityDeath()) return;

        String entityType = event.getEntity().getType().getKey().toString();
        String worldName = event.getEntity().getWorld().getName();
        int x = event.getEntity().getLocation().getBlockX();
        int y = event.getEntity().getLocation().getBlockY();
        int z = event.getEntity().getLocation().getBlockZ();

        plugin.getDatabaseTask().insertBlockRecord(
                worldName, x, y, z,
                null, null,
                "entity_death",
                entityType,
                null, null, null
        );
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        if (!configManager.isLoggingNaturalChanges()) return;

        plugin.getDatabaseTask().insertBlockRecord(
                event.getBlock().getWorld().getName(),
                event.getBlock().getX(),
                event.getBlock().getY(),
                event.getBlock().getZ(),
                null, null,
                "natural",
                event.getNewState().getType().getKey().toString(),
                event.getBlock().getType().getKey().toString(),
                null, null
        );
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!configManager.isLoggingNaturalChanges()) return;

        plugin.getDatabaseTask().insertBlockRecord(
                event.getBlock().getWorld().getName(),
                event.getBlock().getX(),
                event.getBlock().getY(),
                event.getBlock().getZ(),
                null, null,
                "natural",
                event.getBlock().getType().getKey().toString(),
                "minecraft:air",
                null, null
        );
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!configManager.isLoggingNaturalChanges()) return;

        plugin.getDatabaseTask().insertBlockRecord(
                event.getToBlock().getWorld().getName(),
                event.getToBlock().getX(),
                event.getToBlock().getY(),
                event.getToBlock().getZ(),
                null, null,
                "natural",
                event.getToBlock().getType().getKey().toString(),
                event.getBlock().getType().getKey().toString(),
                null, null
        );
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!configManager.isLoggingExplosions()) return;

        String source = event.getEntity() != null ? event.getEntity().getType().getKey().toString() : "block_explosion";
        String worldName = event.getLocation().getWorld().getName();

        for (var block : event.blockList()) {
            plugin.getDatabaseTask().insertBlockRecord(
                    worldName,
                    block.getX(), block.getY(), block.getZ(),
                    null, source,
                    "explosion",
                    block.getType().getKey().toString(),
                    "minecraft:air",
                    null, null
            );
        }
    }
}
