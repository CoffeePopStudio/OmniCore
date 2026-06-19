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
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

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

        app.get("/api/logs/plugin", ctx -> {
            ctx.json(Map.of("logs", new ArrayList<>()));
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
        if (webDir.exists()) {
            plugin.getSLF4JLogger().info("Web files already extracted at {}", webDir.getAbsolutePath());
            return;
        }
        webDir.mkdirs();

        boolean any = extractFromJar("web/index.html", "");
        any |= extractFromJar("web/assets/index-CrqYaQ-y.css", "assets/");
        any |= extractFromJar("web/assets/index-mfu__DhA.js", "assets/");
        any |= extractFromJar("web/assets/client-Do9JGKD1.js", "assets/");
        any |= extractFromJar("web/assets/DashboardView-CDUmnf8s.js", "assets/");
        any |= extractFromJar("web/assets/QueryView-exKDc0BW.js", "assets/");
        any |= extractFromJar("web/assets/LoginView-CdYVgAgs.js", "assets/");
        any |= extractFromJar("web/assets/RegisterView-CPj-XTXY.js", "assets/");
        any |= extractFromJar("web/assets/RollbackView-DHtN2IPK.js", "assets/");
        any |= extractFromJar("web/assets/LayoutView-6iti3gtx.js", "assets/");

        if (!any) {
            plugin.getSLF4JLogger().warn("No web panel static files found in JAR resources");
            webDir = null;
        } else {
            plugin.getSLF4JLogger().info("Web panel static files extracted to {}", webDir.getAbsolutePath());
        }
    }

    private boolean extractFromJar(String resourcePath, String targetSubDir) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return false;
            String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            File target = new File(webDir, targetSubDir + fileName);
            target.getParentFile().mkdirs();
            Files.copy(in, target.toPath());
            return true;
        } catch (IOException e) {
            return false;
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
