package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.config.ConfigManager;
import cn.oneachina.onmiCore.database.DatabaseManager;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.staticfiles.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        this.authController = new AuthController(db, authService);
        QueryController queryController = new QueryController(db, authService, config);
        RollbackController rollbackController = new RollbackController(db, authService, config);

        app.get("/api/health", ctx -> ctx.json(new Object() {
            public final String status = "ok";
            public final String version = plugin.getPluginMeta().getVersion();
        }));

        app.post("/api/auth/register", authController::register);
        app.post("/api/auth/login", authController::login);
        app.post("/api/auth/bind", authController::bind);
        app.get("/api/auth/auto-login", authController::autoLogin);
        app.post("/api/auth/refresh", authController::refresh);

        app.get("/api/query/blocks", queryController::queryBlocks);
        app.get("/api/query/containers", queryController::queryContainers);
        app.get("/api/query/inventory", queryController::queryInventory);

        app.get("/api/rollback/preview", rollbackController::preview);
        app.post("/api/rollback/execute", rollbackController::execute);

        app.get("/api/stats/blocks", ctx -> {
            try (Connection conn = db.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM block_records")) {
                rs.next();
                ctx.json(Map.of("count", rs.getLong(1)));
            }
        });
        app.get("/api/stats/containers", ctx -> {
            try (Connection conn = db.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM container_records")) {
                rs.next();
                ctx.json(Map.of("count", rs.getLong(1)));
            }
        });
        app.get("/api/stats/inventory", ctx -> {
            try (Connection conn = db.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM inventory_records")) {
                rs.next();
                ctx.json(Map.of("count", rs.getLong(1)));
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
        webDir = new File(plugin.getDataFolder(), "web");
        if (webDir.exists()) return;
        webDir.mkdirs();

        String indexContent = extractFile("web/index.html", "");
        if (indexContent == null) {
            plugin.getSLF4JLogger().warn("Web panel index.html not found in JAR");
            webDir = null;
            return;
        }

        Pattern pattern = Pattern.compile("(?:href|src)=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(indexContent);
        boolean any = true;
        while (matcher.find()) {
            String assetPath = matcher.group(1);
            if (assetPath.startsWith("./")) {
                assetPath = "web" + assetPath.substring(1);
            } else if (assetPath.startsWith("/")) {
                assetPath = "web" + assetPath;
            } else {
                assetPath = "web/assets/" + assetPath;
            }
            String targetSubDir = assetPath.contains("/") ?
                assetPath.substring(assetPath.lastIndexOf('/') + 1) : assetPath;
            String targetPath = assetPath.substring("web/".length());
            File target = new File(webDir, targetPath);
            if (target.exists()) continue;
            target.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource(assetPath)) {
                if (in != null) {
                    Files.copy(in, target.toPath());
                }
            } catch (IOException ignored) {}
        }

        if (any) {
            plugin.getSLF4JLogger().info("Web panel static files extracted to {}", webDir.getAbsolutePath());
        }
    }

    private String extractFile(String resourcePath, String targetSubDir) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return null;
            String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            File target = new File(webDir, targetSubDir + fileName);
            target.getParentFile().mkdirs();
            Files.copy(in, target.toPath());
            return new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
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
}
