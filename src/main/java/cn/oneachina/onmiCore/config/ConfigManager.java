package cn.oneachina.onmiCore.config;

import org.bukkit.configuration.ConfigurationSection;

public final class ConfigManager {

    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private ConfigData data;

    public ConfigManager(org.bukkit.plugin.java.JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.data = new ConfigData(plugin.getConfig());
    }

    public ConfigData getData() {
        return data;
    }

    public boolean isWebPanelEnabled() {
        return data.webPanelEnabled;
    }

    public int getWebPanelPort() {
        return data.webPanelPort;
    }

    public boolean isWebRequireSsl() {
        return data.webRequireSsl;
    }

    public java.util.List<String> getAllowedIps() {
        return data.allowedIps;
    }

    public boolean isLoggingBlockPlace() { return data.loggingBlockPlace; }
    public boolean isLoggingBlockBreak() { return data.loggingBlockBreak; }
    public boolean isLoggingContainerAccess() { return data.loggingContainerAccess; }
    public boolean isLoggingInventoryChange() { return data.loggingInventoryChange; }
    public boolean isLoggingPlayerDeath() { return data.loggingPlayerDeath; }
    public boolean isLoggingEntityDeath() { return data.loggingEntityDeath; }
    public boolean isLoggingNaturalChanges() { return data.loggingNaturalChanges; }
    public boolean isLoggingExplosions() { return data.loggingExplosions; }
    public int getDedupTimeMs() { return data.dedupTimeMs; }
    public int getBlocksPerTick() { return data.blocksPerTick; }
    public boolean isRequireConfirmation() { return data.requireConfirmation; }
    public int getCheckpointInterval() { return data.checkpointInterval; }
    public String getRetentionMode() { return data.retentionMode; }
    public int getRetentionMaxDays() { return data.retentionMaxDays; }
    public int getRetentionMaxSizeMb() { return data.retentionMaxSizeMb; }
    public double getRetentionEvictRatio() { return data.retentionEvictRatio; }

    public String getDatabaseType() { return data.databaseType; }
    public String getDatabaseHost() { return data.databaseHost; }
    public int getDatabasePort() { return data.databasePort; }
    public String getDatabaseName() { return data.databaseName; }
    public String getDatabaseUsername() { return data.databaseUsername; }
    public String getDatabasePassword() { return data.databasePassword; }
    public int getDatabasePoolSize() { return data.databasePoolSize; }
    public String getStoragePath() { return data.storagePath; }

    public static final class ConfigData {
        final boolean webPanelEnabled;
        final int webPanelPort;
        final boolean webRequireSsl;
        final java.util.List<String> allowedIps;

        final boolean loggingBlockPlace;
        final boolean loggingBlockBreak;
        final boolean loggingContainerAccess;
        final boolean loggingInventoryChange;
        final boolean loggingPlayerDeath;
        final boolean loggingEntityDeath;
        final boolean loggingNaturalChanges;
        final boolean loggingExplosions;
        final int dedupTimeMs;

        final int blocksPerTick;
        final boolean requireConfirmation;
        final int checkpointInterval;

        final String retentionMode;
        final int retentionMaxDays;
        final int retentionMaxSizeMb;
        final double retentionEvictRatio;

        final String databaseType;
        final String databaseHost;
        final int databasePort;
        final String databaseName;
        final String databaseUsername;
        final String databasePassword;
        final int databasePoolSize;
        final String storagePath;

        ConfigData(org.bukkit.configuration.file.FileConfiguration config) {
            ConfigurationSection web = config.getConfigurationSection("web-panel");
            this.webPanelEnabled = web.getBoolean("enabled", true);
            this.webPanelPort = web.getInt("port", 9812);
            this.webRequireSsl = web.getBoolean("require-ssl", false);
            this.allowedIps = web.getStringList("allowed-ips");

            ConfigurationSection log = config.getConfigurationSection("logging");
            this.loggingBlockPlace = log.getBoolean("block-place", true);
            this.loggingBlockBreak = log.getBoolean("block-break", true);
            this.loggingContainerAccess = log.getBoolean("container-access", true);
            this.loggingInventoryChange = log.getBoolean("inventory-change", true);
            this.loggingPlayerDeath = log.getBoolean("player-death", false);
            this.loggingEntityDeath = log.getBoolean("entity-death", false);
            this.loggingNaturalChanges = log.getBoolean("natural-changes", false);
            this.loggingExplosions = log.getBoolean("explosions", false);
            this.dedupTimeMs = log.getInt("dedup-time-ms", 3000);

            ConfigurationSection rb = config.getConfigurationSection("rollback");
            this.blocksPerTick = rb.getInt("blocks-per-tick", 80);
            this.requireConfirmation = rb.getBoolean("require-confirmation", true);
            this.checkpointInterval = rb.getInt("checkpoint-interval", 100);

            ConfigurationSection ret = config.getConfigurationSection("retention");
            this.retentionMode = ret.getString("mode", "age");
            this.retentionMaxDays = ret.getInt("max-days", 90);
            this.retentionMaxSizeMb = ret.getInt("max-size-mb", 1024);
            this.retentionEvictRatio = ret.getDouble("evict-ratio", 0.05);

            ConfigurationSection db = config.getConfigurationSection("database");
            this.databaseType = db.getString("type", "SQLite");
            this.databaseHost = db.getString("mysql.host", "localhost");
            this.databasePort = db.getInt("mysql.port", 3306);
            this.databaseName = db.getString("mysql.database", "onmicore");
            this.databaseUsername = db.getString("mysql.username", "root");
            this.databasePassword = db.getString("mysql.password", "");
            this.databasePoolSize = db.getInt("mysql.pool-size", 10);
            this.storagePath = config.getString("storage-path", "plugins/OnmiCore/data");
        }
    }
}
