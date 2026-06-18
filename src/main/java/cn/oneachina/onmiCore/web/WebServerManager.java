package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.config.ConfigManager;
import cn.oneachina.onmiCore.database.DatabaseManager;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

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
        this.authService = new AuthService();
    }

    public void start() {
        int port = config.getWebPanelPort();

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
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(corsRule -> {
                corsRule.anyHost();
            }));
            if (!config.getAllowedIps().isEmpty()) {
                cfg.accessManager((handler, ctx, permittedRoles) -> {
                    String ip = ctx.ip();
                    if (config.getAllowedIps().stream().anyMatch(ip::equals)) {
                        handler.handle(ctx);
                    } else {
                        ctx.status(403).json(new Object() {
                            public final String error = "IP not allowed";
                        });
                    }
                });
            }
        };
    }
}
