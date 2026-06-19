package cn.oneachina.onmiCore.listener;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.config.ConfigManager;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class BlockListener implements Listener {

    private final OnmiCore plugin;
    private final ConfigManager configManager;
    private final Map<String, Long> lastActionMap = new HashMap<>();

    public BlockListener() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!configManager.isLoggingBlockPlace()) return;

        String key = event.getPlayer().getUniqueId() + ":"
                + event.getBlock().getX() + ":"
                + event.getBlock().getY() + ":"
                + event.getBlock().getZ();
        long now = System.currentTimeMillis();
        Long last = lastActionMap.get(key);
        if (last != null && (now - last) < configManager.getDedupTimeMs()) return;
        lastActionMap.put(key, now);

        BlockState oldState = event.getBlockReplacedState();
        BlockData newData = event.getBlock().getBlockData();

        String oldBlockType = oldState.getType().getKey().toString();
        String newBlockType = newData.getMaterial().getKey().toString();

        var oldBlockData = oldState.getBlockData().getAsString().getBytes(StandardCharsets.UTF_8);
        var newBlockData = newData.getAsString().getBytes(StandardCharsets.UTF_8);

        plugin.getDatabaseTask().insertBlockRecord(
                event.getBlock().getWorld().getName(),
                event.getBlock().getX(),
                event.getBlock().getY(),
                event.getBlock().getZ(),
                event.getPlayer().getUniqueId().toString(),
                event.getPlayer().getName(),
                "place",
                oldBlockType,
                newBlockType,
                oldBlockData,
                newBlockData
        );
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!configManager.isLoggingBlockBreak()) return;

        String key = event.getPlayer().getUniqueId() + ":"
                + event.getBlock().getX() + ":"
                + event.getBlock().getY() + ":"
                + event.getBlock().getZ();
        long now = System.currentTimeMillis();
        Long last = lastActionMap.get(key);
        if (last != null && (now - last) < configManager.getDedupTimeMs()) return;
        lastActionMap.put(key, now);

        BlockState oldState = event.getBlock().getState();
        String oldBlockType = oldState.getType().getKey().getKey();

        byte[] oldBlockData = oldState.getBlockData().getAsString().getBytes(StandardCharsets.UTF_8);

        plugin.getDatabaseTask().insertBlockRecord(
                event.getBlock().getWorld().getName(),
                event.getBlock().getX(),
                event.getBlock().getY(),
                event.getBlock().getZ(),
                event.getPlayer().getUniqueId().toString(),
                event.getPlayer().getName(),
                "break",
                oldBlockType,
                "minecraft:air",
                oldBlockData,
                null
        );
    }
}
