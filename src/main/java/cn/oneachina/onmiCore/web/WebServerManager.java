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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class WebServerManager {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final DatabaseManager db;
    private final AuthService authService;
    private AuthController authController;
    private Javalin app;
    private File webDir;

    public WebServerManager(JavaPlugin plugin, ConfigManager config, DatabaseManager db) {
        this.plugin = plugin;
        this.config = config;
        this.db = db;
        this.authService = new AuthService();
    }

    public void start() {
        int port = config.getWebPanelPort();

        extractWebFiles();

        app = Javalin.create(configConsumer()).start(port);

        addAuthMiddleware();

        this.authController = new AuthController(db, authService);
        QueryController queryController = new QueryController(db, config);
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
                ctx.json(Map.of("content", content, "total_lines", allLines.size(), "lines_requested", lines));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", e.getMessage()));
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

    private void extractWebFiles() {
        webDir = new File(plugin.getDataFolder(), "run/plugins/OnmiCore/web");
        if (webDir.exists()) return;
        webDir.mkdirs();

        File jarFile = getPluginJarFile();
        if (jarFile == null || !jarFile.exists()) {
            plugin.getSLF4JLogger().warn("Cannot locate plugin JAR for web resource extraction");
            webDir = null;
            return;
        }

        int count = 0;
        try (ZipFile zip = new ZipFile(jarFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("run/plugins/OnmiCore/web/") || entry.isDirectory()) continue;

                String relativePath = name.substring("run/plugins/OnmiCore/web/".length());
                File target = new File(webDir, relativePath);
                target.getParentFile().mkdirs();
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                count++;
            }
        } catch (IOException e) {
            plugin.getSLF4JLogger().error("Failed to extract web files from JAR", e);
            webDir = null;
            return;
        }

        plugin.getSLF4JLogger().info("Extracted {} web panel files to {}", count, webDir.getAbsolutePath());
    }

    private File getPluginJarFile() {
        try {
            var codeSource = plugin.getClass().getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                return new File(codeSource.getLocation().toURI());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Consumer<JavalinConfig> configConsumer() {
        return cfg -> {
            cfg.showJavalinBanner = false;
            if (webDir != null) {
                cfg.staticFiles.add(webDir.getAbsolutePath(), Location.EXTERNAL);
            }
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(corsRule -> {
                corsRule.anyHost();
            }));
        };
    }

    private void addAuthMiddleware() {
        app.before("/api/*", ctx -> {
            String path = ctx.path();
            if (path.equals("/api/health") || path.startsWith("/api/auth/")) {
                return;
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
