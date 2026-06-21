package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.config.ConfigManager;
import cn.oneachina.onmiCore.database.DatabaseManager;
import cn.oneachina.onmiCore.model.rollback.PreviewEntry;
import cn.oneachina.onmiCore.model.rollback.RollbackPlan;
import cn.oneachina.onmiCore.model.rollback.RollbackQuery;
import cn.oneachina.onmiCore.service.RollbackService;
import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.util.PermissionUtil;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class RollbackController {
    private final DatabaseManager db;
    private final ConfigManager config;
    private final RollbackService rollbackService;
    private final Map<UUID, RollbackPlan> planCache = new ConcurrentHashMap<>();

    public RollbackController(DatabaseManager db, ConfigManager config) {
        this.db = db;
        this.config = config;
        this.rollbackService = JavaPlugin.getPlugin(OnmiCore.class).getRollbackService();
    }

    private boolean isPermitted(Context ctx) {
        String uuid = ctx.attribute("uuid");
        if (uuid == null) {
            ctx.status(401).json(Map.of("error", "Not authenticated"));
            return false;
        }
        return true;
    }

    private boolean isRollbackPermitted(Context ctx) {
        String uuid = ctx.attribute("uuid");
        if (uuid == null) {
            ctx.status(401).json(Map.of("error", "Not authenticated"));
            return false;
        }
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        if (offlinePlayer == null || !offlinePlayer.isOnline() || !Bukkit.getPlayer(UUID.fromString(uuid)).hasPermission("onmicore.web.rollback")) {
            ctx.status(403).json(Map.of("error", "Insufficient permissions"));
            return false;
        }
        return true;
    }

    public void preview(Context ctx) {
        if (!isPermitted(ctx)) return;
        try {
            RollbackQuery query = parseQuery(ctx);
            RollbackPlan plan = rollbackService.buildRollbackPlan(query);
            planCache.put(plan.ticket, plan);
            List<Map<String, String>> samples = plan.sampleTargets.stream()
                .map(e -> Map.of("locationKey", e.locationKey, "targetType", e.targetType, "action", e.action))
                .collect(Collectors.toList());
            ctx.json(Map.of(
                "ticket", plan.ticket.toString(),
                "totalLocations", plan.totalLocations,
                "hasContainerOps", plan.hasContainerOps,
                "hasInventoryOps", plan.hasInventoryOps,
                "sampleTargets", samples
            ));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void execute(Context ctx) {
        if (!isRollbackPermitted(ctx)) return;
        try {
            String ticketStr = ctx.queryParam("ticket");
            RollbackPlan plan = null;
            if (ticketStr != null) {
                UUID ticketId = UUID.fromString(ticketStr);
                plan = planCache.remove(ticketId);
            }
            if (plan == null) {
                RollbackQuery query = parseQuery(ctx);
                plan = rollbackService.buildRollbackPlan(query);
            }
            UUID ticket = rollbackService.prepareRollback(plan, null);
            if (ticket == null) {
                ctx.status(400).json(Map.of("error", "No changes to rollback"));
                return;
            }
            Bukkit.getScheduler().runTask(
                JavaPlugin.getPlugin(OnmiCore.class),
                () -> rollbackService.confirmRollback(ticket)
            );
            ctx.json(Map.of("status", "started", "ticket", ticket.toString()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void progress(Context ctx) {
        if (!isPermitted(ctx)) return;
        String ticketStr = ctx.queryParam("ticket");
        if (ticketStr == null) {
            ctx.status(400).json(Map.of("error", "Missing ticket"));
            return;
        }
        try {
            UUID ticket = UUID.fromString(ticketStr);
            int pct = rollbackService.getProgress(ticket);
            ctx.json(Map.of("progress", pct));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private RollbackQuery parseQuery(Context ctx) {
        String timeStr = ctx.queryParam("time");
        Duration timeAmount = Duration.ofMinutes(5);
        if (timeStr != null) {
            if (timeStr.endsWith("m")) timeAmount = Duration.ofMinutes(Long.parseLong(timeStr.replace("m", "")));
            else if (timeStr.endsWith("h")) timeAmount = Duration.ofHours(Long.parseLong(timeStr.replace("h", "")));
            else if (timeStr.endsWith("d")) timeAmount = Duration.ofDays(Long.parseLong(timeStr.replace("d", "")));
            else timeAmount = Duration.ofMinutes(Long.parseLong(timeStr));
        }

        String playerName = ctx.queryParam("player");
        String worldName = ctx.queryParam("world");
        String blockType = ctx.queryParam("block_type");

        int radius = 0;
        Location center = null;
        String radiusStr = ctx.queryParam("radius");
        String xStr = ctx.queryParam("x");
        String yStr = ctx.queryParam("y");
        String zStr = ctx.queryParam("z");
        if (radiusStr != null && xStr != null && yStr != null && zStr != null) {
            radius = Integer.parseInt(radiusStr);
            World world = worldName != null ? Bukkit.getWorld(worldName) : Bukkit.getWorlds().get(0);
            if (world != null) {
                center = new Location(world, Double.parseDouble(xStr), Double.parseDouble(yStr), Double.parseDouble(zStr));
            }
        }

        return new RollbackQuery(timeAmount, playerName, worldName, radius, center, blockType);
    }
}
