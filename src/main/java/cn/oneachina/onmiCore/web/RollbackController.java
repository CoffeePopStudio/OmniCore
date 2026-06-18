package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.service.RollbackService;
import cn.oneachina.onmiCore.util.PermissionUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RollbackController {
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([mhd])$");

    private final OnmiCore plugin;
    private final AuthService authService;
    private final RollbackService rollbackService;
    private final Gson gson;

    public RollbackController() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        this.authService = new AuthService();
        this.rollbackService = new RollbackService();
        this.gson = new Gson();
    }

    public void preview(Context ctx) {
        UUID uuid = extractUuid(ctx);
        if (uuid == null) {
            ctx.status(401).json(Map.of("success", false, "message", "unauthorized"));
            return;
        }

        JsonObject body;
        try {
            body = gson.fromJson(ctx.body(), JsonObject.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("success", false, "message", "invalid json"));
            return;
        }

        if (body == null) {
            ctx.status(400).json(Map.of("success", false, "message", "empty body"));
            return;
        }

        Duration timeAmount = parseTime(body, "timeAmount");
        if (timeAmount == null) {
            ctx.status(400).json(Map.of("success", false, "message", "invalid or missing timeAmount"));
            return;
        }

        String playerName = body.has("player") && !body.get("player").isJsonNull() ? body.get("player").getAsString() : null;
        String worldName = body.has("world") && !body.get("world").isJsonNull() ? body.get("world").getAsString() : null;
        int radius = body.has("radius") && !body.get("radius").isJsonNull() ? body.get("radius").getAsInt() : 0;
        String blockType = body.has("blockType") && !body.get("blockType").isJsonNull() ? body.get("blockType").getAsString() : null;

        RollbackService.RollbackQuery query = new RollbackService.RollbackQuery(
            timeAmount, playerName, worldName, radius, null, blockType);

        try {
            Map<String, String> previewData = rollbackService.createPreview(query);
            ctx.json(Map.of("success", true, "data", previewData, "message", "ok"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public void execute(Context ctx) {
        UUID uuid = extractUuid(ctx);
        if (uuid == null) {
            ctx.status(401).json(Map.of("success", false, "message", "unauthorized"));
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !PermissionUtil.hasRollback(player)) {
            ctx.status(403).json(Map.of("success", false, "message", "forbidden"));
            return;
        }

        JsonObject body;
        try {
            body = gson.fromJson(ctx.body(), JsonObject.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("success", false, "message", "invalid json"));
            return;
        }

        if (body == null) {
            ctx.status(400).json(Map.of("success", false, "message", "empty body"));
            return;
        }

        Duration timeAmount = parseTime(body, "timeAmount");
        if (timeAmount == null) {
            ctx.status(400).json(Map.of("success", false, "message", "invalid or missing timeAmount"));
            return;
        }

        String playerName = body.has("player") && !body.get("player").isJsonNull() ? body.get("player").getAsString() : null;
        String worldName = body.has("world") && !body.get("world").isJsonNull() ? body.get("world").getAsString() : null;
        int radius = body.has("radius") && !body.get("radius").isJsonNull() ? body.get("radius").getAsInt() : 0;
        String blockType = body.has("blockType") && !body.get("blockType").isJsonNull() ? body.get("blockType").getAsString() : null;

        RollbackService.RollbackQuery query = new RollbackService.RollbackQuery(
            timeAmount, playerName, worldName, radius, null, blockType);

        try {
            UUID ticket = rollbackService.prepareRollback(query, player);
            if (ticket == null) {
                ctx.status(400).json(Map.of("success", false, "message", "no records to rollback"));
                return;
            }

            rollbackService.confirmRollback(ticket);

            Map<String, Object> data = new HashMap<>();
            data.put("ticket", ticket.toString());
            ctx.json(Map.of("success", true, "data", data, "message", "rollback started"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public void progress(Context ctx) {
        UUID uuid = extractUuid(ctx);
        if (uuid == null) {
            ctx.status(401).json(Map.of("success", false, "message", "unauthorized"));
            return;
        }

        String ticketStr = ctx.pathParam("ticket");
        UUID ticket;
        try {
            ticket = UUID.fromString(ticketStr);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("success", false, "message", "invalid ticket"));
            return;
        }

        int progress = rollbackService.getProgress(ticket);
        if (progress < 0) {
            ctx.status(404).json(Map.of("success", false, "message", "ticket not found"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("ticket", ticket.toString());
        data.put("progress", progress);
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

    private Duration parseTime(JsonObject body, String key) {
        if (!body.has(key) || body.get(key).isJsonNull()) {
            return null;
        }
        return parseTime(body.get(key).getAsString());
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
}
