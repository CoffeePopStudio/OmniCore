package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.BlockRecord;
import cn.oneachina.onmiCore.model.ContainerRecord;
import cn.oneachina.onmiCore.model.InventoryRecord;
import cn.oneachina.onmiCore.service.RecordService;
import cn.oneachina.onmiCore.util.PermissionUtil;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryController {
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([mhd])$");

    private static final DateTimeFormatter MYSQL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final OnmiCore plugin;
    private final AuthService authService;
    private final RecordService recordService;

    public QueryController() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        this.authService = new AuthService();
        this.recordService = new RecordService();
    }

    public void queryBlocks(Context ctx) {
        UUID uuid = extractUuid(ctx);
        if (uuid == null) {
            ctx.status(401).json(Map.of("success", false, "message", "unauthorized"));
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !PermissionUtil.hasView(player)) {
            ctx.status(403).json(Map.of("success", false, "message", "forbidden"));
            return;
        }

        String world = ctx.queryParam("world");
        String playerParam = ctx.queryParam("player");
        String action = ctx.queryParam("action");
        String blockType = ctx.queryParam("type");
        Duration time = parseTime(ctx.queryParam("time"));
        int page = parseInt(ctx.queryParam("page"), 0);
        int pageSize = parseInt(ctx.queryParam("pageSize"), 20);

        List<BlockRecord> records = recordService.queryBlocks(world, 0, 0, 0, playerParam, action, blockType, time, page, pageSize);
        int total = recordService.countBlocks(world, 0, 0, 0, playerParam, action, blockType, time);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("records", records);

        ctx.json(Map.of("success", true, "data", data, "message", "ok"));
    }

    public void queryContainers(Context ctx) {
        UUID uuid = extractUuid(ctx);
        if (uuid == null) {
            ctx.status(401).json(Map.of("success", false, "message", "unauthorized"));
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !PermissionUtil.hasView(player)) {
            ctx.status(403).json(Map.of("success", false, "message", "forbidden"));
            return;
        }

        String world = ctx.queryParam("world");
        String playerParam = ctx.queryParam("player");
        String action = ctx.queryParam("action");
        String itemType = ctx.queryParam("item");
        Duration time = parseTime(ctx.queryParam("time"));
        int page = parseInt(ctx.queryParam("page"), 0);
        int pageSize = parseInt(ctx.queryParam("pageSize"), 20);

        List<ContainerRecord> records = recordService.queryContainers(world, 0, 0, 0, playerParam, action, itemType, time, page, pageSize);
        int total = recordService.countContainers(world, 0, 0, 0, playerParam, action, itemType, time);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("records", records);

        ctx.json(Map.of("success", true, "data", data, "message", "ok"));
    }

    public void queryInventory(Context ctx) {
        UUID uuid = extractUuid(ctx);
        if (uuid == null) {
            ctx.status(401).json(Map.of("success", false, "message", "unauthorized"));
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !PermissionUtil.hasView(player)) {
            ctx.status(403).json(Map.of("success", false, "message", "forbidden"));
            return;
        }

        String playerParam = ctx.queryParam("player");
        String action = ctx.queryParam("action");
        String itemType = ctx.queryParam("item");
        Duration time = parseTime(ctx.queryParam("time"));
        int page = parseInt(ctx.queryParam("page"), 0);
        int pageSize = parseInt(ctx.queryParam("pageSize"), 20);

        List<InventoryRecord> records = recordService.queryInventory(playerParam, action, itemType, time, page, pageSize);

        int total = 0;
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM inventory_records WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (playerParam != null && !playerParam.isEmpty()) {
            countSql.append(" AND player_name = ?");
            params.add(playerParam);
        }
        if (action != null && !action.isEmpty()) {
            countSql.append(" AND action = ?");
            params.add(action);
        }
        if (itemType != null && !itemType.isEmpty()) {
            countSql.append(" AND item_type = ?");
            params.add(itemType);
        }
        if (time != null) {
            countSql.append(" AND timestamp >= ?");
            params.add(MYSQL_TS.format(Instant.now().minus(time)));
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count inventory records", e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("records", records);

        ctx.json(Map.of("success", true, "data", data, "message", "ok"));
    }

    private UUID extractUuid(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            return authService.getUuidFromToken(authHeader.substring(7));
        } catch (Exception e) {
            return null;
        }
    }

    private Duration parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        Matcher matcher = TIME_PATTERN.matcher(timeStr);
        if (!matcher.matches()) {
            return null;
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        return switch (unit) {
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> Duration.ofMinutes(amount);
        };
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
