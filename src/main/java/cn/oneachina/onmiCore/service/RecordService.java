package cn.oneachina.onmiCore.service;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.BlockRecord;
import cn.oneachina.onmiCore.model.ContainerRecord;
import cn.oneachina.onmiCore.model.InventoryRecord;
import cn.oneachina.onmiCore.util.DatabaseUtil;
import cn.oneachina.onmiCore.util.SqlBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
        SqlBuilder sql = buildBlockQuery(world, x, y, z, playerName, action, blockType, timeAgo, null, null, false);
        sql.orderBy("timestamp DESC").limit(pageSize).offset(page * pageSize);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql.build(), sql.getParams(), BlockRecord.MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query block records", e);
            return Collections.emptyList();
        }
    }

    public List<BlockRecord> queryBlocksWithTimeRange(String world, String playerName,
                                                       String action, String blockType,
                                                       String timeFrom, String timeTo,
                                                       boolean fuzzyPlayer,
                                                       int page, int pageSize) {
        SqlBuilder sql = buildBlockQuery(world, 0, 0, 0, playerName, action, blockType, null, timeFrom, timeTo, false);
        if (fuzzyPlayer && playerName != null && !playerName.isEmpty()) {
            sql.where("(player_name LIKE ? OR player_uuid = ?)", "%" + playerName + "%", playerName);
        }
        sql.orderBy("timestamp DESC").limit(pageSize).offset(page * pageSize);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql.build(), sql.getParams(), BlockRecord.MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query block records", e);
            return Collections.emptyList();
        }
    }

    public int countBlocks(String world, int x, int y, int z,
                            String playerName, String action,
                            String blockType, Duration timeAgo) {
        SqlBuilder sql = buildBlockQuery(world, x, y, z, playerName, action, blockType, timeAgo, null, null, true);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.count(conn, sql.build(), sql.getParams());
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count block records", e);
            return 0;
        }
    }

    private SqlBuilder buildBlockQuery(String world, int x, int y, int z,
                                        String playerName, String action,
                                        String blockType, Duration timeAgo,
                                        String timeFrom, String timeTo,
                                        boolean count) {
        SqlBuilder sql = count ? SqlBuilder.count("block_records") : SqlBuilder.select("*", "block_records");
        if (world != null && !world.isEmpty()) {
            sql.and("world = ?", world);
        }
        if (x != 0 || y != 0 || z != 0) {
            sql.and("x = ?", x).and("y = ?", y).and("z = ?", z);
        }
        if (playerName != null && !playerName.isEmpty()) {
            sql.and("player_name = ?", playerName);
        }
        if (action != null && !action.isEmpty()) {
            sql.and("action = ?", action);
        }
        if (blockType != null && !blockType.isEmpty()) {
            sql.and("(old_block_type = ? OR new_block_type = ?)", blockType, blockType);
        }
        if (timeAgo != null) {
            sql.and("timestamp >= ?", formatTimestamp(Instant.now().minus(timeAgo)));
        }
        if (timeFrom != null && !timeFrom.isEmpty()) {
            sql.and("timestamp >= ?", timeFrom);
        }
        if (timeTo != null && !timeTo.isEmpty()) {
            sql.and("timestamp <= ?", timeTo);
        }
        return sql;
    }

    public List<ContainerRecord> queryContainers(String world, int x, int y, int z,
                                                  String playerName, String action,
                                                  String itemType, Duration timeAgo,
                                                  int page, int pageSize) {
        SqlBuilder sql = buildContainerQuery(world, x, y, z, playerName, action, itemType, timeAgo, false);
        sql.orderBy("timestamp DESC").limit(pageSize).offset(page * pageSize);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql.build(), sql.getParams(), ContainerRecord.MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query container records", e);
            return Collections.emptyList();
        }
    }

    public int countContainers(String world, int x, int y, int z,
                                String playerName, String action,
                                String itemType, Duration timeAgo) {
        SqlBuilder sql = buildContainerQuery(world, x, y, z, playerName, action, itemType, timeAgo, true);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.count(conn, sql.build(), sql.getParams());
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count container records", e);
            return 0;
        }
    }

    private SqlBuilder buildContainerQuery(String world, int x, int y, int z,
                                            String playerName, String action,
                                            String itemType, Duration timeAgo,
                                            boolean count) {
        SqlBuilder sql = count ? SqlBuilder.count("container_records") : SqlBuilder.select("*", "container_records");
        if (world != null && !world.isEmpty()) {
            sql.and("world = ?", world);
        }
        if (x != 0 || y != 0 || z != 0) {
            sql.and("x = ?", x).and("y = ?", y).and("z = ?", z);
        }
        if (playerName != null && !playerName.isEmpty()) {
            sql.and("player_name = ?", playerName);
        }
        if (action != null && !action.isEmpty()) {
            sql.and("action = ?", action);
        }
        if (itemType != null && !itemType.isEmpty()) {
            sql.and("item_type = ?", itemType);
        }
        if (timeAgo != null) {
            sql.and("timestamp >= ?", formatTimestamp(Instant.now().minus(timeAgo)));
        }
        return sql;
    }

    public List<InventoryRecord> queryInventory(String playerName, String action,
                                                 String itemType, Duration timeAgo,
                                                 int page, int pageSize) {
        SqlBuilder sql = buildInventoryQuery(playerName, action, itemType, timeAgo, false);
        sql.orderBy("timestamp DESC").limit(pageSize).offset(page * pageSize);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql.build(), sql.getParams(), InventoryRecord.MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query inventory records", e);
            return Collections.emptyList();
        }
    }

    public int countInventory(String playerName, String action,
                               String itemType, Duration timeAgo) {
        SqlBuilder sql = buildInventoryQuery(playerName, action, itemType, timeAgo, true);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.count(conn, sql.build(), sql.getParams());
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count inventory records", e);
            return 0;
        }
    }

    private SqlBuilder buildInventoryQuery(String playerName, String action,
                                            String itemType, Duration timeAgo,
                                            boolean count) {
        SqlBuilder sql = count ? SqlBuilder.count("inventory_records") : SqlBuilder.select("*", "inventory_records");
        if (playerName != null && !playerName.isEmpty()) {
            sql.and("player_name = ?", playerName);
        }
        if (action != null && !action.isEmpty()) {
            sql.and("action = ?", action);
        }
        if (itemType != null && !itemType.isEmpty()) {
            sql.and("item_type = ?", itemType);
        }
        if (timeAgo != null) {
            sql.and("timestamp >= ?", formatTimestamp(Instant.now().minus(timeAgo)));
        }
        return sql;
    }

    private String formatTimestamp(Instant instant) {
        if (plugin.getDatabaseManager().isMySQL()) {
            return MYSQL_TS.format(instant);
        }
        return instant.toString();
    }
}
