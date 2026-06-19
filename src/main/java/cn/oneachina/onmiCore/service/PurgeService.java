package cn.oneachina.onmiCore.service;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.config.ConfigManager;
import cn.oneachina.onmiCore.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PurgeService {

    private static final int PURGE_BATCH_SIZE = 1000;

    private static final DateTimeFormatter MYSQL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final OnmiCore plugin;
    private final DatabaseManager databaseManager;

    public PurgeService(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = (OnmiCore) plugin;
        this.databaseManager = databaseManager;
    }

    public void schedule() {
        int intervalHours = plugin.getConfigManager().getPurgeIntervalHours();
        long intervalTicks = 20L * 60L * 60L * intervalHours;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            ConfigManager config = plugin.getConfigManager();
            String mode = config.getRetentionMode();
            if ("age".equalsIgnoreCase(mode)) {
                int days = config.getRetentionMaxDays();
                int purged = purgeByAge(days);
                if (purged > 0) {
                    plugin.getSLF4JLogger().info("Auto-purged {} old records (age > {} days)", purged, days);
                }
            } else if ("size".equalsIgnoreCase(mode)) {
                long maxSize = config.getRetentionMaxSizeMb();
                long currentSize = getDatabaseSize();
                long maxSizeBytes = maxSize * 1024L * 1024L;
                if (currentSize > maxSizeBytes) {
                    int purged = purgeBySize();
                    plugin.getSLF4JLogger().info("Auto-purged {} records (size: {}MB > {}MB limit)", purged, currentSize / 1024 / 1024, maxSize);
                }
            }
        }, intervalTicks, intervalTicks);
    }

    public int purgeByAge(int days) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        String ts = formatTimestamp(cutoff);
        int total = 0;

        total += deleteFromTable("block_records", ts);
        total += deleteFromTable("container_records", ts);
        total += deleteFromTable("inventory_records", ts);

        return total;
    }

    public int purgeBySize() {
        ConfigManager config = plugin.getConfigManager();
        double evictRatio = config.getRetentionEvictRatio();

        long totalCount = getTotalRecordCount();
        if (totalCount <= 0) {
            return 0;
        }

        long evictCount = (long) (totalCount * evictRatio);
        if (evictCount <= 0) {
            evictCount = 1000;
        }

        int total = 0;
        total += deleteOldestFromTable("block_records", evictCount);
        total += deleteOldestFromTable("container_records", evictCount);
        total += deleteOldestFromTable("inventory_records", evictCount);

        return total;
    }

    public long getDatabaseSize() {
        if (databaseManager.isMySQL()) {
            return getMySQLDatabaseSize();
        }
        return getSQLiteDatabaseSize();
    }

    private long getSQLiteDatabaseSize() {
        String storagePath = plugin.getConfigManager().getStoragePath();
        File dbFile = new File(storagePath, "onmicore.db");
        if (dbFile.exists()) {
            return dbFile.length();
        }
        return 0;
    }

    private long getMySQLDatabaseSize() {
        String dbName = plugin.getConfigManager().getDatabaseName();
        String sql = "SELECT SUM(data_length + index_length) FROM information_schema.tables WHERE table_schema = ? AND table_name IN ('block_records', 'container_records', 'inventory_records')";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to get MySQL database size", e);
        }

        return 0;
    }

    private long getTotalRecordCount() {
        String sql = "SELECT (SELECT COUNT(*) FROM block_records) + (SELECT COUNT(*) FROM container_records) + (SELECT COUNT(*) FROM inventory_records)";

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to get total record count", e);
        }

        return 0;
    }

    private int deleteFromTable(String table, String cutoffTimestamp) {
        String sql = "DELETE FROM " + table + " WHERE timestamp < ? LIMIT " + PURGE_BATCH_SIZE;

        int totalDeleted = 0;
        try (Connection conn = databaseManager.getConnection()) {
            boolean hasMore;
            do {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, cutoffTimestamp);
                    int deleted = ps.executeUpdate();
                    hasMore = deleted >= PURGE_BATCH_SIZE;
                    totalDeleted += deleted;
                }
            } while (hasMore);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to purge records from {}", table, e);
        }

        return totalDeleted;
    }

    private int deleteOldestFromTable(String table, long count) {
        int batches = (int) Math.ceil((double) count / PURGE_BATCH_SIZE);
        int totalDeleted = 0;

        try (Connection conn = databaseManager.getConnection()) {
            for (int i = 0; i < batches; i++) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM " + table + " WHERE id IN (SELECT id FROM " + table + " ORDER BY timestamp ASC LIMIT " + PURGE_BATCH_SIZE + ")")) {
                    int deleted = ps.executeUpdate();
                    totalDeleted += deleted;
                    if (deleted < PURGE_BATCH_SIZE) break;
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to purge oldest records from {}", table, e);
        }

        return totalDeleted;
    }

    private String formatTimestamp(Instant instant) {
        if (databaseManager.isMySQL()) {
            return MYSQL_TS.format(instant);
        }
        return instant.toString();
    }
}
