package cn.oneachina.onmiCore;

import cn.oneachina.onmiCore.config.ConfigManager;
import cn.oneachina.onmiCore.config.MessageManager;
import cn.oneachina.onmiCore.database.DatabaseManager;
import cn.oneachina.onmiCore.database.DatabaseTask;
import cn.oneachina.onmiCore.listener.BlockListener;
import cn.oneachina.onmiCore.listener.ContainerListener;
import cn.oneachina.onmiCore.listener.InventoryListener;
import cn.oneachina.onmiCore.command.CommandManager;
import cn.oneachina.onmiCore.service.PurgeService;
import cn.oneachina.onmiCore.service.RollbackService;
import cn.oneachina.onmiCore.web.WebServerManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class OnmiCore extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private DatabaseTask databaseTask;
    private PurgeService purgeService;
    private RollbackService rollbackService;
    private WebServerManager webServerManager;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        messageManager = new MessageManager(this);
        messageManager.reload();

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        databaseTask = new DatabaseTask(this);
        databaseTask.start();

        rollbackService = new RollbackService();

        registerListeners();
        registerCommands();

        purgeService = new PurgeService(this, databaseManager);
        purgeService.schedule();

        startWebServer();

        getSLF4JLogger().info("OnmiCore enabled in {}ms", System.currentTimeMillis() - start);
    }

    @Override
    public void onDisable() {
        if (webServerManager != null) {
            webServerManager.stop();
        }
        if (databaseTask != null) {
            databaseTask.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    public void reload() {
        onDisable();
        onEnable();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new ContainerListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
    }

    private void registerCommands() {
        CommandManager commandManager = new CommandManager();
        Objects.requireNonNull(getCommand("oc")).setExecutor(commandManager);
        Objects.requireNonNull(getCommand("oc")).setTabCompleter(commandManager);
    }

    private void startWebServer() {
        if (configManager.isWebPanelEnabled()) {
            webServerManager = new WebServerManager(this, configManager, databaseManager);
            webServerManager.start();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DatabaseTask getDatabaseTask() {
        return databaseTask;
    }

    public PurgeService getPurgeService() {
        return purgeService;
    }

    public RollbackService getRollbackService() {
        return rollbackService;
    }

    public WebServerManager getWebServerManager() {
        return webServerManager;
    }
}
