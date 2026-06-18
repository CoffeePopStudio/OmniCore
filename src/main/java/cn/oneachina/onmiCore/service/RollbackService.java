package cn.oneachina.onmiCore.service;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.BlockRecord;
import cn.oneachina.onmiCore.model.ContainerRecord;
import cn.oneachina.onmiCore.util.LocationSerializer;
import cn.oneachina.onmiCore.util.NBTCompressor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RollbackService {

    private static final DateTimeFormatter MYSQL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final OnmiCore plugin;
    private final Map<UUID, PendingRollback> pendingMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> progressMap = new ConcurrentHashMap<>();

    public RollbackService() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
    }

    public static class RollbackQuery {
        private final Duration timeAmount;
        private final String playerName;
        private final String worldName;
        private final int radius;
        private final Location center;
        private final String blockType;

        public RollbackQuery(Duration timeAmount, String playerName, String worldName, int radius, Location center, String blockType) {
            this.timeAmount = timeAmount;
            this.playerName = playerName;
            this.worldName = worldName;
            this.radius = radius;
            this.center = center;
            this.blockType = blockType;
        }

        public Duration getTimeAmount() { return timeAmount; }
        public String getPlayerName() { return playerName; }
        public String getWorldName() { return worldName; }
        public int getRadius() { return radius; }
        public Location getCenter() { return center; }
        public String getBlockType() { return blockType; }
    }

    public Map<String, String> createPreview(RollbackQuery query) {
        Map<String, String> preview = new LinkedHashMap<>();
        Instant cutoff = Instant.now().minus(query.getTimeAmount());

        List<BlockRecord> records = queryBlockRecords(query, cutoff);
        Set<String> locations = new LinkedHashSet<>();
        for (BlockRecord record : records) {
            locations.add(LocationSerializer.toString(record.world, record.x, record.y, record.z));
        }

        for (String locKey : locations) {
            String[] parts = LocationSerializer.fromString(locKey);
            String world = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            BlockRecord recordBefore = findLastRecordBefore(world, x, y, z, cutoff);
            if (recordBefore != null) {
                String targetType = recordBefore.newBlockType != null ? recordBefore.newBlockType : "air";
                preview.put(locKey, "restore to " + targetType);
            } else {
                preview.put(locKey, "remove (no prior state)");
            }
        }

        List<ContainerRecord> containerRecords = queryContainerRecords(query, cutoff);
        if (!containerRecords.isEmpty()) {
            preview.put("[container]", containerRecords.size() + " container operations to reverse");
        }

        return preview;
    }

    public UUID prepareRollback(RollbackQuery query, CommandSender sender) {
        Map<String, String> preview = createPreview(query);
        if (preview.isEmpty()) {
            return null;
        }

        UUID ticket = UUID.randomUUID();
        Instant cutoff = Instant.now().minus(query.getTimeAmount());

        List<BlockRecord> records = queryBlockRecords(query, cutoff);
        List<LocationBlockState> blockTargets = new ArrayList<>();

        Set<String> locations = new LinkedHashSet<>();
        for (BlockRecord record : records) {
            locations.add(LocationSerializer.toString(record.world, record.x, record.y, record.z));
        }

        for (String locKey : locations) {
            String[] parts = LocationSerializer.fromString(locKey);
            String world = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            BlockRecord recordBefore = findLastRecordBefore(world, x, y, z, cutoff);
            if (recordBefore != null) {
                String blockDataStr = decompressBlockData(recordBefore.newBlockData);
                blockTargets.add(new LocationBlockState(world, x, y, z, blockDataStr, recordBefore.newBlockType));
            } else {
                blockTargets.add(new LocationBlockState(world, x, y, z, null, null));
            }
        }

        List<ContainerRecord> containerRecords = queryContainerRecords(query, cutoff);
        List<ContainerReversal> containerReversals = new ArrayList<>();
        for (ContainerRecord cr : containerRecords) {
            String reversedAction = cr.action.equalsIgnoreCase("withdraw") ? "deposit" : "withdraw";
            containerReversals.add(new ContainerReversal(cr.world, cr.x, cr.y, cr.z, reversedAction, cr.itemType, cr.itemAmount, cr.itemData, cr.playerUuid, cr.playerName));
        }

        List<ContainerRecord> inventoryRecords = queryInventoryRecords(query, cutoff);
        List<InventoryReversal> inventoryReversals = new ArrayList<>();
        for (ContainerRecord ir : inventoryRecords) {
            String reversedAction = ir.action.equalsIgnoreCase("withdraw") ? "deposit" : "withdraw";
            inventoryReversals.add(new InventoryReversal(ir.playerUuid, ir.playerName, reversedAction, ir.itemType, ir.itemAmount, ir.itemData));
        }

        PendingRollback pending = new PendingRollback(ticket, query, sender, blockTargets, containerReversals, inventoryReversals);
        pendingMap.put(ticket, pending);
        progressMap.put(ticket, 0);

        return ticket;
    }

    public void confirmRollback(UUID ticket) {
        PendingRollback pending = pendingMap.remove(ticket);
        if (pending == null) {
            return;
        }

        int rollbackId = nextRollbackId();
        int total = pending.blockTargets.size() + pending.containerReversals.size() + pending.inventoryReversals.size();
        if (total == 0) {
            progressMap.put(ticket, 100);
            return;
        }

        progressMap.put(ticket, 0);

        new BukkitRunnable() {
            int blockIndex = 0;
            int containerIndex = 0;
            int inventoryIndex = 0;
            int processedCount = 0;

            @Override
            public void run() {
                int blocksPerTick = plugin.getConfigManager().getBlocksPerTick();
                int checkpointInterval = plugin.getConfigManager().getCheckpointInterval();
                int processedThisTick = 0;

                while (blockIndex < pending.blockTargets.size() && processedThisTick < blocksPerTick) {
                    LocationBlockState target = pending.blockTargets.get(blockIndex);
                    applyBlockRestore(target, rollbackId);
                    blockIndex++;
                    processedCount++;
                    processedThisTick++;
                    updateProgress(ticket, processedCount, total);
                    if (processedCount % checkpointInterval == 0) {
                        saveCheckpoint(ticket, rollbackId, processedCount, total, "running");
                    }
                }

                while (blockIndex >= pending.blockTargets.size() && containerIndex < pending.containerReversals.size() && processedThisTick < blocksPerTick) {
                    ContainerReversal reversal = pending.containerReversals.get(containerIndex);
                    applyContainerReversal(reversal, rollbackId);
                    containerIndex++;
                    processedCount++;
                    processedThisTick++;
                    updateProgress(ticket, processedCount, total);
                    if (processedCount % checkpointInterval == 0) {
                        saveCheckpoint(ticket, rollbackId, processedCount, total, "running");
                    }
                }

                while (blockIndex >= pending.blockTargets.size() && containerIndex >= pending.containerReversals.size() && inventoryIndex < pending.inventoryReversals.size() && processedThisTick < blocksPerTick) {
                    InventoryReversal reversal = pending.inventoryReversals.get(inventoryIndex);
                    applyInventoryReversal(reversal, rollbackId);
                    inventoryIndex++;
                    processedCount++;
                    processedThisTick++;
                    updateProgress(ticket, processedCount, total);
                    if (processedCount % checkpointInterval == 0) {
                        saveCheckpoint(ticket, rollbackId, processedCount, total, "running");
                    }
                }

                if (blockIndex >= pending.blockTargets.size() && containerIndex >= pending.containerReversals.size() && inventoryIndex >= pending.inventoryReversals.size()) {
                    saveCheckpoint(ticket, rollbackId, processedCount, total, "completed");
                    recordRollbackMeta(ticket, rollbackId, pending);
                    progressMap.put(ticket, 100);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void cancelRollback(UUID ticket) {
        pendingMap.remove(ticket);
        progressMap.remove(ticket);
    }

    public int getProgress(UUID ticket) {
        Integer progress = progressMap.get(ticket);
        return progress != null ? progress : -1;
    }

    public void restoreRollback(int checkpointId, CommandSender sender) {
        List<BlockRecord> rollbackRecords = queryRecordsByRollbackId(checkpointId);
        if (rollbackRecords.isEmpty()) {
            return;
        }

        int restoreRollbackId = nextRollbackId();

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int blocksPerTick = plugin.getConfigManager().getBlocksPerTick();
                int processedThisTick = 0;

                while (index < rollbackRecords.size() && processedThisTick < blocksPerTick) {
                    BlockRecord record = rollbackRecords.get(index);
                    World world = plugin.getServer().getWorld(record.world);
                    if (world != null) {
                        if (record.oldBlockData != null && record.oldBlockData.length > 0) {
                            String dataStr = decompressBlockData(record.oldBlockData);
                            if (dataStr != null) {
                                try {
                                    BlockData data = Bukkit.createBlockData(dataStr);
                                    world.getBlockAt(record.x, record.y, record.z).setBlockData(data);
                                } catch (Exception ignored) {
                                }
                            }
                        } else {
                            world.getBlockAt(record.x, record.y, record.z).setType(Material.AIR);
                        }
                    }
                    index++;
                    processedThisTick++;
                }

                if (index >= rollbackRecords.size()) {
                    plugin.getServer().getLogger().info("Rollback restore completed for checkpoint " + checkpointId);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private List<BlockRecord> queryBlockRecords(RollbackQuery query, Instant cutoff) {
        List<BlockRecord> records = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM block_records WHERE timestamp >= ?");
        List<Object> params = new ArrayList<>();
        params.add(formatTimestamp(cutoff));

        if (query.getPlayerName() != null && !query.getPlayerName().isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(query.getPlayerName());
        }
        if (query.getWorldName() != null && !query.getWorldName().isEmpty()) {
            sql.append(" AND world = ?");
            params.add(query.getWorldName());
        }
        if (query.getBlockType() != null && !query.getBlockType().isEmpty()) {
            sql.append(" AND (old_block_type = ? OR new_block_type = ?)");
            params.add(query.getBlockType());
            params.add(query.getBlockType());
        }
        if (query.getRadius() > 0 && query.getCenter() != null) {
            sql.append(" AND world = ? AND x BETWEEN ? AND ? AND z BETWEEN ? AND ?");
            params.add(query.getCenter().getWorld().getName());
            int cx = query.getCenter().getBlockX();
            int cz = query.getCenter().getBlockZ();
            params.add(cx - query.getRadius());
            params.add(cx + query.getRadius());
            params.add(cz - query.getRadius());
            params.add(cz + query.getRadius());
        }

        sql.append(" ORDER BY timestamp DESC");

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
            plugin.getSLF4JLogger().error("Failed to query block records for rollback", e);
        }

        return records;
    }

    private BlockRecord findLastRecordBefore(String world, int x, int y, int z, Instant cutoff) {
        String sql = "SELECT * FROM block_records WHERE world = ? AND x = ? AND y = ? AND z = ? AND timestamp < ? ORDER BY timestamp DESC LIMIT 1";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.setString(5, formatTimestamp(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
                    return record;
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to find last record before timestamp", e);
        }

        return null;
    }

    private List<ContainerRecord> queryContainerRecords(RollbackQuery query, Instant cutoff) {
        List<ContainerRecord> records = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM container_records WHERE timestamp >= ?");
        List<Object> params = new ArrayList<>();
        params.add(formatTimestamp(cutoff));

        if (query.getPlayerName() != null && !query.getPlayerName().isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(query.getPlayerName());
        }
        if (query.getWorldName() != null && !query.getWorldName().isEmpty()) {
            sql.append(" AND world = ?");
            params.add(query.getWorldName());
        }
        if (query.getRadius() > 0 && query.getCenter() != null) {
            sql.append(" AND world = ? AND x BETWEEN ? AND ? AND z BETWEEN ? AND ?");
            params.add(query.getCenter().getWorld().getName());
            int cx = query.getCenter().getBlockX();
            int cz = query.getCenter().getBlockZ();
            params.add(cx - query.getRadius());
            params.add(cx + query.getRadius());
            params.add(cz - query.getRadius());
            params.add(cz + query.getRadius());
        }

        sql.append(" ORDER BY timestamp DESC");

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
            plugin.getSLF4JLogger().error("Failed to query container records for rollback", e);
        }

        return records;
    }

    private List<ContainerRecord> queryInventoryRecords(RollbackQuery query, Instant cutoff) {
        List<ContainerRecord> records = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM inventory_records WHERE timestamp >= ?");
        List<Object> params = new ArrayList<>();
        params.add(formatTimestamp(cutoff));

        if (query.getPlayerName() != null && !query.getPlayerName().isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(query.getPlayerName());
        }

        sql.append(" ORDER BY timestamp DESC");

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ContainerRecord record = new ContainerRecord();
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
            plugin.getSLF4JLogger().error("Failed to query inventory records for rollback", e);
        }

        return records;
    }

    private List<BlockRecord> queryRecordsByRollbackId(int rollbackId) {
        List<BlockRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM block_records WHERE rollback_id = ? ORDER BY id ASC";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rollbackId);
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
            plugin.getSLF4JLogger().error("Failed to query records by rollback_id", e);
        }

        return records;
    }

    private void applyBlockRestore(LocationBlockState target, int rollbackId) {
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
    }

    private void applyContainerReversal(ContainerReversal reversal, int rollbackId) {
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
    }

    private void applyInventoryReversal(InventoryReversal reversal, int rollbackId) {
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
    }

    private void saveCheckpoint(UUID ticket, int rollbackId, int processed, int total, String status) {
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
    }

    private void recordRollbackMeta(UUID ticket, int rollbackId, PendingRollback pending) {
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
    }

    private int nextRollbackId() {
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

    private void updateProgress(UUID ticket, int processed, int total) {
        int pct = (int) ((double) processed / total * 100);
        if (pct > 100) pct = 100;
        progressMap.put(ticket, pct);
    }

    private String decompressBlockData(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return null;
        }
        try {
            return NBTCompressor.decompress(compressed);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] compressBlockData(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return NBTCompressor.compress(data);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatTimestamp(Instant instant) {
        if (plugin.getDatabaseManager().isMySQL()) {
            return MYSQL_TS.format(instant);
        }
        return instant.toString();
    }

    private static class LocationBlockState {
        final String world;
        final int x;
        final int y;
        final int z;
        final String blockDataStr;
        final String blockType;

        LocationBlockState(String world, int x, int y, int z, String blockDataStr, String blockType) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockDataStr = blockDataStr;
            this.blockType = blockType;
        }
    }

    private static class ContainerReversal {
        final String world;
        final int x;
        final int y;
        final int z;
        final String action;
        final String itemType;
        final int itemAmount;
        final byte[] itemData;
        final String playerUuid;
        final String playerName;

        ContainerReversal(String world, int x, int y, int z, String action, String itemType, int itemAmount, byte[] itemData, String playerUuid, String playerName) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.action = action;
            this.itemType = itemType;
            this.itemAmount = itemAmount;
            this.itemData = itemData;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
        }
    }

    private static class InventoryReversal {
        final String playerUuid;
        final String playerName;
        final String action;
        final String itemType;
        final int itemAmount;
        final byte[] itemData;

        InventoryReversal(String playerUuid, String playerName, String action, String itemType, int itemAmount, byte[] itemData) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.action = action;
            this.itemType = itemType;
            this.itemAmount = itemAmount;
            this.itemData = itemData;
        }
    }

    private static class PendingRollback {
        final UUID ticket;
        final RollbackQuery query;
        final CommandSender sender;
        final List<LocationBlockState> blockTargets;
        final List<ContainerReversal> containerReversals;
        final List<InventoryReversal> inventoryReversals;

        PendingRollback(UUID ticket, RollbackQuery query, CommandSender sender, List<LocationBlockState> blockTargets, List<ContainerReversal> containerReversals, List<InventoryReversal> inventoryReversals) {
            this.ticket = ticket;
            this.query = query;
            this.sender = sender;
            this.blockTargets = blockTargets;
            this.containerReversals = containerReversals;
            this.inventoryReversals = inventoryReversals;
        }
    }
}
