package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.config.ConfigManager;
import cn.oneachina.onmiCore.database.DatabaseManager;
import cn.oneachina.onmiCore.model.BlockRecord;
import cn.oneachina.onmiCore.model.ContainerRecord;
import cn.oneachina.onmiCore.model.InventoryRecord;
import cn.oneachina.onmiCore.util.DatabaseUtil;
import cn.oneachina.onmiCore.util.SqlBuilder;
import io.javalin.http.Context;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public final class QueryController {
    private final DatabaseManager db;
    private final ConfigManager config;

    public QueryController(DatabaseManager db, ConfigManager config) {
        this.db = db;
        this.config = config;
    }

    private boolean isPermitted(Context ctx) {
        String uuid = ctx.attribute("uuid");
        if (uuid == null) {
            ctx.status(401).json(Map.of("error", "Not authenticated"));
            return false;
        }
        return true;
    }

    private int parseIntParam(Context ctx, String key, int defaultValue) {
        String val = ctx.queryParam(key);
        if (val == null || val.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void queryBlocks(Context ctx) {
        if (!isPermitted(ctx)) return;
        try {
            String world = ctx.queryParam("world");
            String player = ctx.queryParam("player");
            String action = ctx.queryParam("action");
            String blockType = ctx.queryParam("block_type");
            int page = parseIntParam(ctx, "page", 1);
            int pageSize = parseIntParam(ctx, "page_size", 50);
            String timeFrom = ctx.queryParam("time_from");
            String timeTo = ctx.queryParam("time_to");

            SqlBuilder sql = SqlBuilder.select("*", "block_records");
            if (world != null && !world.isEmpty()) { sql.and("world = ?", world); }
            if (player != null && !player.isEmpty()) { sql.and("(player_name LIKE ? OR player_uuid = ?)", "%" + player + "%", player); }
            if (action != null && !action.isEmpty()) { sql.and("action = ?", action); }
            if (blockType != null && !blockType.isEmpty()) { sql.and("(old_block_type = ? OR new_block_type = ?)", blockType, blockType); }
            if (timeFrom != null && !timeFrom.isEmpty()) { sql.and("timestamp >= ?", timeFrom); }
            if (timeTo != null && !timeTo.isEmpty()) { sql.and("timestamp <= ?", timeTo); }
            int offset = (page - 1) * pageSize;
            sql.orderBy("timestamp DESC").limit(pageSize).offset(offset);

            List<BlockRecord> records;
            try (Connection conn = db.getConnection()) {
                records = DatabaseUtil.query(conn, sql.build(), sql.getParams(), BlockRecord.MAPPER);
            }
            ctx.json(Map.of("records", records, "page", page, "page_size", pageSize));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void queryContainers(Context ctx) {
        if (!isPermitted(ctx)) return;
        try {
            String world = ctx.queryParam("world");
            String player = ctx.queryParam("player");
            String action = ctx.queryParam("action");
            String itemType = ctx.queryParam("item_type");
            int page = parseIntParam(ctx, "page", 1);
            int pageSize = parseIntParam(ctx, "page_size", 50);

            SqlBuilder sql = SqlBuilder.select("*", "container_records");
            if (world != null && !world.isEmpty()) { sql.and("world = ?", world); }
            if (player != null && !player.isEmpty()) { sql.and("(player_name LIKE ? OR player_uuid = ?)", "%" + player + "%", player); }
            if (action != null && !action.isEmpty()) { sql.and("action = ?", action); }
            if (itemType != null && !itemType.isEmpty()) { sql.and("item_type = ?", itemType); }
            int offset = (page - 1) * pageSize;
            sql.orderBy("timestamp DESC").limit(pageSize).offset(offset);

            List<ContainerRecord> records;
            try (Connection conn = db.getConnection()) {
                records = DatabaseUtil.query(conn, sql.build(), sql.getParams(), ContainerRecord.MAPPER);
            }
            ctx.json(Map.of("records", records, "page", page, "page_size", pageSize));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void queryInventory(Context ctx) {
        if (!isPermitted(ctx)) return;
        try {
            String player = ctx.queryParam("player");
            String action = ctx.queryParam("action");
            String itemType = ctx.queryParam("item_type");
            int page = parseIntParam(ctx, "page", 1);
            int pageSize = parseIntParam(ctx, "page_size", 50);

            SqlBuilder sql = SqlBuilder.select("*", "inventory_records");
            if (player != null && !player.isEmpty()) { sql.and("(player_name LIKE ? OR player_uuid = ?)", "%" + player + "%", player); }
            if (action != null && !action.isEmpty()) { sql.and("action = ?", action); }
            if (itemType != null && !itemType.isEmpty()) { sql.and("item_type = ?", itemType); }
            int offset = (page - 1) * pageSize;
            sql.orderBy("timestamp DESC").limit(pageSize).offset(offset);

            List<InventoryRecord> records;
            try (Connection conn = db.getConnection()) {
                records = DatabaseUtil.query(conn, sql.build(), sql.getParams(), InventoryRecord.MAPPER);
            }
            ctx.json(Map.of("records", records, "page", page, "page_size", pageSize));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }
}
