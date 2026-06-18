package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.BlockRecord;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InspectCommand implements SubCommand, Listener {

    private final OnmiCore plugin;
    private final Map<UUID, Boolean> inspectMode = new ConcurrentHashMap<>();

    public InspectCommand() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void execute(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean current = inspectMode.getOrDefault(uuid, false);
        if (current) {
            inspectMode.put(uuid, false);
            player.sendMessage(plugin.getMessageManager().get("inspect-disabled"));
        } else {
            inspectMode.put(uuid, true);
            player.sendMessage(plugin.getMessageManager().get("inspect-enabled"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!inspectMode.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        event.setCancelled(true);

        var block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        String world = block.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<BlockRecord> records = queryRecentRecords(world, x, y, z);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (records.isEmpty()) {
                    player.sendMessage(plugin.getMessageManager().get("query-no-results"));
                    return;
                }
                player.sendMessage(plugin.getMessageManager().get("query-header", records.size()));
                int index = 1;
                for (BlockRecord record : records) {
                    String actionText = switch (record.action) {
                        case "place" -> plugin.getMessageManager().raw("query-action-place");
                        case "break" -> plugin.getMessageManager().raw("query-action-break");
                        case "container" -> plugin.getMessageManager().raw("query-action-container");
                        case "inventory" -> plugin.getMessageManager().raw("query-action-inventory");
                        default -> record.action;
                    };
                    player.sendMessage(plugin.getMessageManager().get("query-entry",
                            index++, record.timestamp, record.playerName,
                            actionText, record.newBlockType != null ? record.newBlockType : record.oldBlockType,
                            world + "," + x + "," + y + "," + z));
                }
            });
        });
    }

    private List<BlockRecord> queryRecentRecords(String world, int x, int y, int z) {
        List<BlockRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM block_records WHERE world = ? AND x = ? AND y = ? AND z = ? ORDER BY timestamp DESC LIMIT 10";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BlockRecord record = new BlockRecord();
                    record.id = rs.getLong("id");
                    record.world = rs.getString("world");
                    record.x = rs.getInt("x");
                    record.y = rs.getInt("y");
                    record.z = rs.getInt("z");
                    record.playerUuid = rs.getString("player_uuid");
                    record.playerName = rs.getString("player_name");
                    record.action = rs.getString("action");
                    record.oldBlockType = rs.getString("old_block_type");
                    record.newBlockType = rs.getString("new_block_type");
                    record.oldBlockData = rs.getBytes("old_block_data");
                    record.newBlockData = rs.getBytes("new_block_data");
                    record.timestamp = rs.getString("timestamp");
                    record.rollbackId = rs.getInt("rollback_id");
                    records.add(record);
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query block records for inspect", e);
        }

        return records;
    }
}
