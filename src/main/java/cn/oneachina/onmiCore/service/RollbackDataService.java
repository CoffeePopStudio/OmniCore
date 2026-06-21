package cn.oneachina.onmiCore.service;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.BlockRecord;
import cn.oneachina.onmiCore.model.ContainerRecord;
import cn.oneachina.onmiCore.model.rollback.ContainerReversal;
import cn.oneachina.onmiCore.model.rollback.CursorState;
import cn.oneachina.onmiCore.model.rollback.InventoryReversal;
import cn.oneachina.onmiCore.model.rollback.LocationBlockState;
import cn.oneachina.onmiCore.model.rollback.PendingRollback;
import cn.oneachina.onmiCore.model.rollback.RollbackQuery;
import cn.oneachina.onmiCore.util.DatabaseUtil;
import cn.oneachina.onmiCore.util.NBTCompressor;
import cn.oneachina.onmiCore.util.SqlBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RollbackDataService {

    private static final DateTimeFormatter MYSQL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    static final DatabaseUtil.RowMapper<ContainerRecord> INVENTORY_MAPPER = rs -> {
        ContainerRecord r = new ContainerRecord();
        r.id = rs.getLong("id");
        r.playerUuid = rs.getString("player_uuid");
        r.playerName = rs.getString("player_name");
        r.action = rs.getString("action");
        r.itemType = rs.getString("item_type");
        r.itemAmount = rs.getInt("item_amount");
        r.itemData = rs.getBytes("item_data");
        r.timestamp = rs.getString("timestamp");
        r.rollbackId = rs.getInt("rollback_id");
        return r;
    };

    private final OnmiCore plugin;

    public RollbackDataService() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
    }

    public List<BlockRecord> queryBlockRecordsPage(RollbackQuery query, Instant cutoff, CursorState cursorState, int limit) {
        SqlBuilder sql = SqlBuilder.select("*", "block_records");

        if (cursorState.isFirstPage) {
            sql.and("timestamp >= ?", formatTimestamp(cutoff));
        } else {
            sql.and("(timestamp, id) > (?, ?)", cursorState.lastTimestamp, cursorState.lastId);
            sql.and("timestamp >= ?", formatTimestamp(cutoff));
        }

        if (query.getPlayerName() != null && !query.getPlayerName().isEmpty()) {
            sql.and("player_name = ?", query.getPlayerName());
        }
        if (query.getWorldName() != null && !query.getWorldName().isEmpty()) {
            sql.and("world = ?", query.getWorldName());
        }
        if (query.getBlockType() != null && !query.getBlockType().isEmpty()) {
            sql.and("(old_block_type = ? OR new_block_type = ?)", query.getBlockType(), query.getBlockType());
        }
        if (query.getRadius() > 0 && query.getCenter() != null) {
            sql.and("world = ?", query.getCenter().getWorld().getName());
            int cx = query.getCenter().getBlockX();
            int cz = query.getCenter().getBlockZ();
            sql.and("x BETWEEN ? AND ?", cx - query.getRadius(), cx + query.getRadius());
            sql.and("z BETWEEN ? AND ?", cz - query.getRadius(), cz + query.getRadius());
        }

        sql.orderBy("timestamp ASC, id ASC");
        sql.limit(limit);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            List<BlockRecord> results = DatabaseUtil.query(conn, sql.build(), sql.getParams(), BlockRecord.MAPPER, true);
            if (!results.isEmpty()) {
                BlockRecord last = results.get(results.size() - 1);
                cursorState.lastTimestamp = last.timestamp;
                cursorState.lastId = last.id;
                cursorState.isFirstPage = false;
            }
            return results;
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query block records page for rollback", e);
            return new ArrayList<>();
        }
    }

    public int countContainerOps(RollbackQuery query, Instant cutoff) {
        SqlBuilder sql = SqlBuilder.select("COUNT(1) AS cnt", "container_records");
        sql.and("timestamp >= ?", formatTimestamp(cutoff));

        if (query.getPlayerName() != null && !query.getPlayerName().isEmpty()) {
            sql.and("player_name = ?", query.getPlayerName());
        }
        if (query.getWorldName() != null && !query.getWorldName().isEmpty()) {
            sql.and("world = ?", query.getWorldName());
        }
        if (query.getRadius() > 0 && query.getCenter() != null) {
            sql.and("world = ?", query.getCenter().getWorld().getName());
            int cx = query.getCenter().getBlockX();
            int cz = query.getCenter().getBlockZ();
            sql.and("x BETWEEN ? AND ?", cx - query.getRadius(), cx + query.getRadius());
            sql.and("z BETWEEN ? AND ?", cz - query.getRadius(), cz + query.getRadius());
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.count(conn, sql.build(), sql.getParams());
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count container ops", e);
        }
        return 0;
    }

    public int countInventoryOps(RollbackQuery query, Instant cutoff) {
        SqlBuilder sql = SqlBuilder.select("COUNT(1) AS cnt", "inventory_records");
        sql.and("timestamp >= ?", formatTimestamp(cutoff));

        if (query.getPlayerName() != null && !query.getPlayerName().isEmpty()) {
            sql.and("player_name = ?", query.getPlayerName());
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.count(conn, sql.build(), sql.getParams());
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count inventory ops", e);
        }
        return 0;
    }

    public List<ContainerRecord> queryContainerRecords(RollbackQuery query, Instant cutoff) {
        SqlBuilder sql = SqlBuilder.select("*", "container_records");
        sql.and("timestamp >= ?", formatTimestamp(cutoff));

        if (query.getPlayerName() != null && !query.getPlayerName().isEmpty()) {
            sql.and("player_name = ?", query.getPlayerName());
        }
        if (query.getWorldName() != null && !query.getWorldName().isEmpty()) {
            sql.and("world = ?", query.getWorldName());
        }
        if (query.getRadius() > 0 && query.getCenter() != null) {
            sql.and("world = ?", query.getCenter().getWorld().getName());
            int cx = query.getCenter().getBlockX();
            int cz = query.getCenter().getBlockZ();
            sql.and("x BETWEEN ? AND ?", cx - query.getRadius(), cx + query.getRadius());
            sql.and("z BETWEEN ? AND ?", cz - query.getRadius(), cz + query.getRadius());
        }

        sql.orderBy("timestamp DESC");

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql.build(), sql.getParams(), ContainerRecord.MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query container records for rollback", e);
            return new ArrayList<>();
        }
    }

    public List<ContainerRecord> queryInventoryRecords(RollbackQuery query, Instant cutoff) {
        SqlBuilder sql = SqlBuilder.select("*", "inventory_records");
        sql.and("timestamp >= ?", formatTimestamp(cutoff));

        if (query.getPlayerName() != null && !query.getPlayerName().isEmpty()) {
            sql.and("player_name = ?", query.getPlayerName());
        }

        sql.orderBy("timestamp DESC");

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql.build(), sql.getParams(), INVENTORY_MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query inventory records for rollback", e);
            return new ArrayList<>();
        }
    }

    public List<BlockRecord> queryRecordsByRollbackId(int rollbackId) {
        String sql = "SELECT * FROM block_records WHERE rollback_id = ? ORDER BY id ASC";

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql, List.of(rollbackId), BlockRecord.MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query records by rollback_id", e);
            return new ArrayList<>();
        }
    }

    public List<ContainerRecord> queryContainerRecordsByRollbackId(int rollbackId) {
        String sql = "SELECT * FROM container_records WHERE rollback_id = ? ORDER BY id ASC";

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql, List.of(rollbackId), ContainerRecord.MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query container records by rollback_id", e);
            return new ArrayList<>();
        }
    }

    public List<ContainerRecord> queryInventoryRecordsByRollbackId(int rollbackId) {
        String sql = "SELECT * FROM inventory_records WHERE rollback_id = ? ORDER BY id ASC";

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql, List.of(rollbackId), INVENTORY_MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query inventory records by rollback_id", e);
            return new ArrayList<>();
        }
    }

    public void applyBlockRestore(LocationBlockState target, int rollbackId) {
        World world = plugin.getServer().getWorld(target.world);
        if (world == null) {
            return;
        }

        org.bukkit.block.Block block = world.getBlockAt(target.x, target.y, target.z);
        BlockData oldData = block.getBlockData();
        String oldDataStr = oldData.getAsString();

        if (target.blockDataStr != null) {
            try {
                BlockData newData = Bukkit.createBlockData(target.blockDataStr);
                block.setBlockData(newData);
            } catch (Exception e) {
                plugin.getSLF4JLogger().warn("Failed to set block data at {},{},{}: {}", target.x, target.y, target.z, e.getMessage());
                return;
            }
        } else {
            block.setType(Material.AIR);
        }

        String newDataStr = target.blockDataStr != null ? target.blockDataStr : "minecraft:air";
        String blockType = target.blockType != null ? target.blockType : "air";

        plugin.getDatabaseTask().submit(() -> {
            String insertSQL = "INSERT INTO block_records (world, x, y, z, player_uuid, player_name, action, old_block_type, new_block_type, old_block_data, new_block_data, rollback_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                ps.setString(1, target.world);
                ps.setInt(2, target.x);
                ps.setInt(3, target.y);
                ps.setInt(4, target.z);
                ps.setString(5, null);
                ps.setString(6, "rollback");
                ps.setString(7, "rollback");
                ps.setString(8, target.blockType);
                ps.setString(9, blockType);
                ps.setBytes(10, compressBlockData(oldDataStr));
                ps.setBytes(11, compressBlockData(newDataStr));
                ps.setInt(12, rollbackId);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Failed to insert rollback block record", e);
            }
        });
    }

    public void applyContainerReversal(ContainerReversal reversal, int rollbackId) {
        World world = plugin.getServer().getWorld(reversal.world);
        if (world != null) {
            org.bukkit.block.Block block = world.getBlockAt(reversal.x, reversal.y, reversal.z);
            if (block.getState() instanceof Container container) {
                Material material = Material.getMaterial(reversal.itemType.toUpperCase());
                if (material != null) {
                    ItemStack stack = new ItemStack(material, reversal.itemAmount);
                    if ("deposit".equals(reversal.action)) {
                        container.getInventory().addItem(stack);
                    } else {
                        container.getInventory().removeItem(stack);
                    }
                }
            }
        }

        plugin.getDatabaseTask().submit(() -> {
            String insertSQL = "INSERT INTO container_records (world, x, y, z, player_uuid, player_name, action, item_type, item_amount, item_data, rollback_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                ps.setString(1, reversal.world);
                ps.setInt(2, reversal.x);
                ps.setInt(3, reversal.y);
                ps.setInt(4, reversal.z);
                ps.setString(5, reversal.playerUuid);
                ps.setString(6, reversal.playerName);
                ps.setString(7, reversal.action);
                ps.setString(8, reversal.itemType);
                ps.setInt(9, reversal.itemAmount);
                ps.setBytes(10, reversal.itemData);
                ps.setInt(11, rollbackId);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Failed to insert container reversal record", e);
            }
        });
    }

    public void applyInventoryReversal(InventoryReversal reversal, int rollbackId) {
        if (reversal.playerName != null && !reversal.playerName.isEmpty()) {
            Player player = plugin.getServer().getPlayerExact(reversal.playerName);
            if (player != null && player.isOnline()) {
                Material material = Material.getMaterial(reversal.itemType.toUpperCase());
                if (material != null) {
                    ItemStack stack = new ItemStack(material, reversal.itemAmount);
                    if ("drop".equals(reversal.action)) {
                        player.getInventory().removeItem(stack);
                    } else {
                        player.getInventory().addItem(stack);
                    }
                }
            }
        }

        plugin.getDatabaseTask().submit(() -> {
            String insertSQL = "INSERT INTO inventory_records (player_uuid, player_name, action, item_type, item_amount, item_data, rollback_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                ps.setString(1, reversal.playerUuid);
                ps.setString(2, reversal.playerName);
                ps.setString(3, reversal.action);
                ps.setString(4, reversal.itemType);
                ps.setInt(5, reversal.itemAmount);
                ps.setBytes(6, reversal.itemData);
                ps.setInt(7, rollbackId);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Failed to insert inventory reversal record", e);
            }
        });
    }

    public void saveCheckpoint(UUID ticket, int rollbackId, int processed, int total, String status) {
        plugin.getDatabaseTask().submit(() -> {
            String insertSQL = "INSERT INTO rollback_checkpoints (rollback_ticket, processed_count, total_count, status) VALUES (?, ?, ?, ?)";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, ticket.toString());
                ps.setInt(2, processed);
                ps.setInt(3, total);
                ps.setString(4, status);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Failed to save checkpoint", e);
            }
        });
    }

    public void recordRollbackMeta(UUID ticket, int rollbackId, PendingRollback pending) {
        plugin.getDatabaseTask().submit(() -> {
            String metaKey = "rollback_" + rollbackId;
            String metaValue = ticket.toString() + "|" + (pending.sender != null ? pending.sender.getName() : "console") + "|" + Instant.now().toString();

            String insertSQL = "INSERT OR REPLACE INTO meta (key_name, value) VALUES (?, ?)";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                ps.setString(1, metaKey);
                ps.setString(2, metaValue);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Failed to record rollback meta", e);
            }
        });
    }

    public int nextRollbackId() {
        String selectSQL = "SELECT value FROM meta WHERE key_name = 'rollback_seq'";
        String upsertSQL = "INSERT OR REPLACE INTO meta (key_name, value) VALUES ('rollback_seq', ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            int seq = 1;
            try (PreparedStatement ps = conn.prepareStatement(selectSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    seq = rs.getInt("value") + 1;
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(upsertSQL)) {
                ps.setInt(1, seq);
                ps.executeUpdate();
            }
            return seq;
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to get next rollback id", e);
            return (int) (System.currentTimeMillis() / 1000);
        }
    }

    public String decompressBlockData(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return null;
        }
        try {
            return NBTCompressor.decompress(compressed);
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] compressBlockData(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return NBTCompressor.compress(data);
        } catch (Exception e) {
            return null;
        }
    }

    public String formatTimestamp(Instant instant) {
        if (plugin.getDatabaseManager().isMySQL()) {
            return MYSQL_TS.format(instant);
        }
        return instant.toString();
    }
}
