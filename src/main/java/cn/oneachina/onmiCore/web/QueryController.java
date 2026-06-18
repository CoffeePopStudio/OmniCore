package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.config.ConfigManager;
import cn.oneachina.onmiCore.database.DatabaseManager;
import cn.oneachina.onmiCore.util.PermissionUtil;
import io.javalin.http.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QueryController {
    private final DatabaseManager db;
    private final AuthService authService;
    private final ConfigManager config;

    public QueryController(DatabaseManager db, AuthService authService, ConfigManager config) {
        this.db = db;
        this.authService = authService;
        this.config = config;
    }

    private boolean authenticate(Context ctx) {
        String token = ctx.queryParam("token");
        if (token == null) {
            ctx.status(401).json(Map.of("error", "Missing token"));
            return false;
        }
        String uuid = authService.getUuidFromToken(token);
        if (uuid == null) {
            ctx.status(401).json(Map.of("error", "Invalid token"));
            return false;
        }
        ctx.attribute("uuid", uuid);
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
        if (!authenticate(ctx)) return;
        try {
            String world = ctx.queryParam("world");
            String player = ctx.queryParam("player");
            String action = ctx.queryParam("action");
            String blockType = ctx.queryParam("block_type");
            int page = parseIntParam(ctx, "page", 1);
            int pageSize = parseIntParam(ctx, "page_size", 50);
            String timeFrom = ctx.queryParam("time_from");
            String timeTo = ctx.queryParam("time_to");

            StringBuilder sql = new StringBuilder("SELECT * FROM block_records WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (world != null && !world.isEmpty()) { sql.append(" AND world = ?"); params.add(world); }
            if (player != null && !player.isEmpty()) { sql.append(" AND (player_name LIKE ? OR player_uuid = ?)"); params.add("%" + player + "%"); params.add(player); }
            if (action != null && !action.isEmpty()) { sql.append(" AND action = ?"); params.add(action); }
            if (blockType != null && !blockType.isEmpty()) { sql.append(" AND (old_block_type = ? OR new_block_type = ?)"); params.add(blockType); params.add(blockType); }
            if (timeFrom != null && !timeFrom.isEmpty()) { sql.append(" AND timestamp >= ?"); params.add(timeFrom); }
            if (timeTo != null && !timeTo.isEmpty()) { sql.append(" AND timestamp <= ?"); params.add(timeTo); }

            sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
            int offset = (page - 1) * pageSize;
            params.add(pageSize);
            params.add(offset);

            List<Map<String, Object>> records = new ArrayList<>();
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> rec = new HashMap<>();
                        rec.put("id", rs.getLong("id"));
                        rec.put("world", rs.getString("world"));
                        rec.put("x", rs.getInt("x"));
                        rec.put("y", rs.getInt("y"));
                        rec.put("z", rs.getInt("z"));
                        rec.put("player_uuid", rs.getString("player_uuid"));
                        rec.put("player_name", rs.getString("player_name"));
                        rec.put("action", rs.getString("action"));
                        rec.put("old_block_type", rs.getString("old_block_type"));
                        rec.put("new_block_type", rs.getString("new_block_type"));
                        rec.put("timestamp", rs.getString("timestamp"));
                        records.add(rec);
                    }
                }
            }
            ctx.json(Map.of("records", records, "page", page, "page_size", pageSize));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void queryContainers(Context ctx) {
        if (!authenticate(ctx)) return;
        try {
            String world = ctx.queryParam("world");
            String player = ctx.queryParam("player");
            String action = ctx.queryParam("action");
            String itemType = ctx.queryParam("item_type");
            int page = parseIntParam(ctx, "page", 1);
            int pageSize = parseIntParam(ctx, "page_size", 50);

            StringBuilder sql = new StringBuilder("SELECT * FROM container_records WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (world != null && !world.isEmpty()) { sql.append(" AND world = ?"); params.add(world); }
            if (player != null && !player.isEmpty()) { sql.append(" AND (player_name LIKE ? OR player_uuid = ?)"); params.add("%" + player + "%"); params.add(player); }
            if (action != null && !action.isEmpty()) { sql.append(" AND action = ?"); params.add(action); }
            if (itemType != null && !itemType.isEmpty()) { sql.append(" AND item_type = ?"); params.add(itemType); }

            sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
            int offset = (page - 1) * pageSize;
            params.add(pageSize);
            params.add(offset);

            List<Map<String, Object>> records = new ArrayList<>();
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> rec = new HashMap<>();
                        rec.put("id", rs.getLong("id"));
                        rec.put("world", rs.getString("world"));
                        rec.put("x", rs.getInt("x"));
                        rec.put("y", rs.getInt("y"));
                        rec.put("z", rs.getInt("z"));
                        rec.put("player_uuid", rs.getString("player_uuid"));
                        rec.put("player_name", rs.getString("player_name"));
                        rec.put("action", rs.getString("action"));
                        rec.put("item_type", rs.getString("item_type"));
                        rec.put("item_amount", rs.getInt("item_amount"));
                        rec.put("timestamp", rs.getString("timestamp"));
                        records.add(rec);
                    }
                }
            }
            ctx.json(Map.of("records", records, "page", page, "page_size", pageSize));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void queryInventory(Context ctx) {
        if (!authenticate(ctx)) return;
        try {
            String player = ctx.queryParam("player");
            String action = ctx.queryParam("action");
            String itemType = ctx.queryParam("item_type");
            int page = parseIntParam(ctx, "page", 1);
            int pageSize = parseIntParam(ctx, "page_size", 50);

            StringBuilder sql = new StringBuilder("SELECT * FROM inventory_records WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (player != null && !player.isEmpty()) { sql.append(" AND (player_name LIKE ? OR player_uuid = ?)"); params.add("%" + player + "%"); params.add(player); }
            if (action != null && !action.isEmpty()) { sql.append(" AND action = ?"); params.add(action); }
            if (itemType != null && !itemType.isEmpty()) { sql.append(" AND item_type = ?"); params.add(itemType); }

            sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
            int offset = (page - 1) * pageSize;
            params.add(pageSize);
            params.add(offset);

            List<Map<String, Object>> records = new ArrayList<>();
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> rec = new HashMap<>();
                        rec.put("id", rs.getLong("id"));
                        rec.put("player_uuid", rs.getString("player_uuid"));
                        rec.put("player_name", rs.getString("player_name"));
                        rec.put("action", rs.getString("action"));
                        rec.put("item_type", rs.getString("item_type"));
                        rec.put("item_amount", rs.getInt("item_amount"));
                        rec.put("timestamp", rs.getString("timestamp"));
                        records.add(rec);
                    }
                }
            }
            ctx.json(Map.of("records", records, "page", page, "page_size", pageSize));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }
}
