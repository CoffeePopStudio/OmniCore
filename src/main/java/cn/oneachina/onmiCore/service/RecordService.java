package cn.oneachina.onmiCore.service;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.BlockRecord;
import cn.oneachina.onmiCore.model.ContainerRecord;
import cn.oneachina.onmiCore.model.InventoryRecord;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RecordService {

    private static final DateTimeFormatter MYSQL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final OnmiCore plugin;

    public RecordService() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
    }

    public List<BlockRecord> queryBlocks(String world, int x, int y, int z,
                                          String playerName, String action,
                                          String blockType, Duration timeAgo,
                                          int page, int pageSize) {
        List<BlockRecord> records = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM block_records WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (world != null && !world.isEmpty()) {
            sql.append(" AND world = ?");
            params.add(world);
        }
        if (x != 0 || y != 0 || z != 0) {
            sql.append(" AND x = ? AND y = ? AND z = ?");
            params.add(x);
            params.add(y);
            params.add(z);
        }
        if (playerName != null && !playerName.isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(playerName);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (blockType != null && !blockType.isEmpty()) {
            sql.append(" AND (old_block_type = ? OR new_block_type = ?)");
            params.add(blockType);
            params.add(blockType);
        }
        if (timeAgo != null) {
            sql.append(" AND timestamp >= ?");
            params.add(formatTimestamp(Instant.now().minus(timeAgo)));
        }

        sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(page * pageSize);

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
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
            plugin.getSLF4JLogger().error("Failed to query block records", e);
        }

        return records;
    }

    public int countBlocks(String world, int x, int y, int z,
                            String playerName, String action,
                            String blockType, Duration timeAgo) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM block_records WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (world != null && !world.isEmpty()) {
            sql.append(" AND world = ?");
            params.add(world);
        }
        if (x != 0 || y != 0 || z != 0) {
            sql.append(" AND x = ? AND y = ? AND z = ?");
            params.add(x);
            params.add(y);
            params.add(z);
        }
        if (playerName != null && !playerName.isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(playerName);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (blockType != null && !blockType.isEmpty()) {
            sql.append(" AND (old_block_type = ? OR new_block_type = ?)");
            params.add(blockType);
            params.add(blockType);
        }
        if (timeAgo != null) {
            sql.append(" AND timestamp >= ?");
            params.add(formatTimestamp(Instant.now().minus(timeAgo)));
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count block records", e);
        }

        return 0;
    }

    public List<ContainerRecord> queryContainers(String world, int x, int y, int z,
                                                  String playerName, String action,
                                                  String itemType, Duration timeAgo,
                                                  int page, int pageSize) {
        List<ContainerRecord> records = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM container_records WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (world != null && !world.isEmpty()) {
            sql.append(" AND world = ?");
            params.add(world);
        }
        if (x != 0 || y != 0 || z != 0) {
            sql.append(" AND x = ? AND y = ? AND z = ?");
            params.add(x);
            params.add(y);
            params.add(z);
        }
        if (playerName != null && !playerName.isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(playerName);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (itemType != null && !itemType.isEmpty()) {
            sql.append(" AND item_type = ?");
            params.add(itemType);
        }
        if (timeAgo != null) {
            sql.append(" AND timestamp >= ?");
            params.add(formatTimestamp(Instant.now().minus(timeAgo)));
        }

        sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(page * pageSize);

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ContainerRecord record = new ContainerRecord();
                    record.id = rs.getLong("id");
                    record.world = rs.getString("world");
                    record.x = rs.getInt("x");
                    record.y = rs.getInt("y");
                    record.z = rs.getInt("z");
                    record.playerUuid = rs.getString("player_uuid");
                    record.playerName = rs.getString("player_name");
                    record.action = rs.getString("action");
                    record.itemType = rs.getString("item_type");
                    record.itemAmount = rs.getInt("item_amount");
                    record.itemData = rs.getBytes("item_data");
                    record.timestamp = rs.getString("timestamp");
                    record.rollbackId = rs.getInt("rollback_id");
                    records.add(record);
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query container records", e);
        }

        return records;
    }

    public int countContainers(String world, int x, int y, int z,
                                String playerName, String action,
                                String itemType, Duration timeAgo) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM container_records WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (world != null && !world.isEmpty()) {
            sql.append(" AND world = ?");
            params.add(world);
        }
        if (x != 0 || y != 0 || z != 0) {
            sql.append(" AND x = ? AND y = ? AND z = ?");
            params.add(x);
            params.add(y);
            params.add(z);
        }
        if (playerName != null && !playerName.isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(playerName);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (itemType != null && !itemType.isEmpty()) {
            sql.append(" AND item_type = ?");
            params.add(itemType);
        }
        if (timeAgo != null) {
            sql.append(" AND timestamp >= ?");
            params.add(formatTimestamp(Instant.now().minus(timeAgo)));
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count container records", e);
        }

        return 0;
    }

    public List<InventoryRecord> queryInventory(String playerName, String action,
                                                 String itemType, Duration timeAgo,
                                                 int page, int pageSize) {
        List<InventoryRecord> records = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM inventory_records WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (playerName != null && !playerName.isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(playerName);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (itemType != null && !itemType.isEmpty()) {
            sql.append(" AND item_type = ?");
            params.add(itemType);
        }
        if (timeAgo != null) {
            sql.append(" AND timestamp >= ?");
            params.add(formatTimestamp(Instant.now().minus(timeAgo)));
        }

        sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(page * pageSize);

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InventoryRecord record = new InventoryRecord();
                    record.id = rs.getLong("id");
                    record.playerUuid = rs.getString("player_uuid");
                    record.playerName = rs.getString("player_name");
                    record.action = rs.getString("action");
                    record.itemType = rs.getString("item_type");
                    record.itemAmount = rs.getInt("item_amount");
                    record.itemData = rs.getBytes("item_data");
                    record.timestamp = rs.getString("timestamp");
                    record.rollbackId = rs.getInt("rollback_id");
                    records.add(record);
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query inventory records", e);
        }

        return records;
    }

    public int countInventory(String playerName, String action,
                               String itemType, Duration timeAgo) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM inventory_records WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (playerName != null && !playerName.isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(playerName);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (itemType != null && !itemType.isEmpty()) {
            sql.append(" AND item_type = ?");
            params.add(itemType);
        }
        if (timeAgo != null) {
            sql.append(" AND timestamp >= ?");
            params.add(formatTimestamp(Instant.now().minus(timeAgo)));
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count inventory records", e);
        }

        return 0;
    }

    private String formatTimestamp(Instant instant) {
        if (plugin.getDatabaseManager().isMySQL()) {
            return MYSQL_TS.format(instant);
        }
        return instant.toString();
    }
}
