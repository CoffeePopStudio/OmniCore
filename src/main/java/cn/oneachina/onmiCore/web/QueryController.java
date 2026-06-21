package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.config.ConfigManager;
import cn.oneachina.onmiCore.database.DatabaseManager;
import cn.oneachina.onmiCore.model.BlockRecord;
import cn.oneachina.onmiCore.model.ContainerRecord;
import cn.oneachina.onmiCore.model.InventoryRecord;
import cn.oneachina.onmiCore.service.RecordService;
import cn.oneachina.onmiCore.util.DatabaseUtil;
import cn.oneachina.onmiCore.util.SqlBuilder;
import io.javalin.http.Context;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public final class QueryController {
    private final DatabaseManager db;
    private final ConfigManager config;
    private final RecordService recordService;

    public QueryController(DatabaseManager db, ConfigManager config, RecordService recordService) {
        this.db = db;
        this.config = config;
        this.recordService = recordService;
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

            boolean fuzzyPlayer = player != null && !player.isEmpty();
            List<BlockRecord> records = recordService.queryBlocksWithTimeRange(
                    world, player, action, blockType, timeFrom, timeTo, fuzzyPlayer, page - 1, pageSize);

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

            List<ContainerRecord> records;
            try (Connection conn = db.getConnection()) {
                records = DatabaseUtil.queryAll(conn, "SELECT * FROM container_records ORDER BY id DESC LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize, ContainerRecord.MAPPER);
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

            List<InventoryRecord> records;
            try (Connection conn = db.getConnection()) {
                records = DatabaseUtil.queryAll(conn, "SELECT * FROM inventory_records ORDER BY id DESC LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize, InventoryRecord.MAPPER);
            }
            ctx.json(Map.of("records", records, "page", page, "page_size", pageSize));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }
}
