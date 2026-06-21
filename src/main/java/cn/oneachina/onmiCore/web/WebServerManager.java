package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.config.ConfigManager;
import cn.oneachina.onmiCore.database.DatabaseManager;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class WebServerManager {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final DatabaseManager db;
    private final AuthService authService;
    private AuthController authController;
    private Javalin app;

    public WebServerManager(JavaPlugin plugin, ConfigManager config, DatabaseManager db) {
        this.plugin = plugin;
        this.config = config;
        this.db = db;
        this.authService = new AuthService(config.getData().tokenExpiryDays);
    }

    public void start() {
        int port = config.getWebPanelPort();

        app = Javalin.create(configConsumer()).start(port);

        addAuthMiddleware();

        this.authController = new AuthController(db, authService);
        QueryController queryController = new QueryController(db, config, ((cn.oneachina.onmiCore.OnmiCore) plugin).getRecordService());
        RollbackController rollbackController = new RollbackController(db, config);

        app.get("/api/health", ctx -> ctx.json(new Object() {
            public final String status = "ok";
            public final String version = plugin.getPluginMeta().getVersion();
        }));

        app.post("/api/auth/register", authController::register);
        app.post("/api/auth/login", authController::login);
        app.post("/api/auth/bind", authController::bind);
        app.get("/api/auth/auto-login", authController::autoLogin);
        app.post("/api/auth/session-login", authController::sessionLogin);
        app.post("/api/auth/refresh", authController::refresh);

        app.get("/api/query/blocks", queryController::queryBlocks);
        app.get("/api/query/containers", queryController::queryContainers);
        app.get("/api/query/inventory", queryController::queryInventory);

        app.get("/api/rollback/preview", rollbackController::preview);
        app.post("/api/rollback/execute", rollbackController::execute);
        app.get("/api/rollback/progress", rollbackController::progress);

        app.get("/api/logs/plugin", ctx -> {
            File logFile = new File(plugin.getDataFolder().getParentFile(), "latest.log");
            if (!logFile.exists()) {
                ctx.json(Map.of("content", ""));
                return;
            }
            int lines = ctx.queryParam("lines") != null ? Integer.parseInt(ctx.queryParam("lines")) : 200;
            try {
                java.util.List<String> allLines = java.nio.file.Files.readAllLines(logFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                int from = Math.max(0, allLines.size() - lines);
                String content = String.join("\n", allLines.subList(from, allLines.size()));
                content = content.replaceAll("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b", "<filtered>");
                content = content.replaceAll("\\b[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}\\b", "<filtered>");
                ctx.json(Map.of("content", content, "total_lines", allLines.size(), "lines_requested", lines));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", e.getMessage()));
            }
        });

        app.get("/api/web-version", ctx -> {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("web/web-version.json")) {
                if (is == null) {
                    ctx.json(Map.of("version", "unknown", "pluginVersion", plugin.getPluginMeta().getVersion()));
                    return;
                }
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject node = JsonParser.parseString(json).getAsJsonObject();
                node.addProperty("pluginVersion", plugin.getPluginMeta().getVersion());
                ctx.json(node);
            }
        });

        app.get("/api/stats", ctx -> {
            try (Connection conn = db.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                    "SELECT " +
                    "  (SELECT COUNT(*) FROM block_records) AS blocks, " +
                    "  (SELECT COUNT(*) FROM container_records) AS containers, " +
                    "  (SELECT COUNT(*) FROM inventory_records) AS inventory")) {
                rs.next();
                ctx.json(Map.of(
                    "blocks", rs.getLong("blocks"),
                    "containers", rs.getLong("containers"),
                    "inventory", rs.getLong("inventory")
                ));
            }
        });

        plugin.getSLF4JLogger().info("Web panel started on port {}", port);
    }

    public void stop() {
        if (app != null) {
            app.stop();
            plugin.getSLF4JLogger().info("Web panel stopped");
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public String createBindToken(String uuid) {
        if (authController == null) return null;
        return authController.createBindToken(uuid);
    }

    private Consumer<JavalinConfig> configConsumer() {
        return cfg -> {
            cfg.showJavalinBanner = false;
            cfg.staticFiles.add("/web", Location.CLASSPATH);
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(corsRule -> {
                corsRule.anyHost();
            }));
        };
    }

    private void addAuthMiddleware() {
        List<String> allowedIps = config.getAllowedIps();

        app.before("/api/*", ctx -> {
            ctx.res().setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; connect-src 'self'; img-src 'self' data:; font-src 'self' data:; frame-ancestors 'none'; form-action 'self'");

            String path = ctx.path();
            if (path.equals("/api/health") || path.startsWith("/api/auth/")) {
                return;
            }

            if (!allowedIps.isEmpty()) {
                String remoteIp = ctx.req().getRemoteAddr();
                boolean allowed = allowedIps.stream().anyMatch(ip -> {
                    if (ip.contains("*")) {
                        String pattern = ip.replace(".", "\\.").replace("*", ".*");
                        return remoteIp.matches(pattern);
                    }
                    return ip.equals(remoteIp);
                });
                if (!allowed) {
                    ctx.status(403).json(Map.of("error", "Access denied: IP not allowed"));
                    return;
                }
            }

            String authHeader = ctx.header("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            if (token == null || token.isEmpty()) {
                ctx.status(401).json(Map.of("error", "Missing or invalid Authorization header"));
                return;
            }

            String uuid = authService.getUuidFromToken(token);
            if (uuid == null) {
                ctx.status(401).json(Map.of("error", "Invalid or expired token"));
                return;
            }

            ctx.attribute("uuid", uuid);
        });
    }

    public static String extractToken(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return ctx.queryParam("token");
    }
}
