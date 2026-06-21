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
    public int getPurgeIntervalHours() { return data.purgeIntervalHours; }

    public String getDatabaseType() { return data.databaseType; }
    public String getDatabaseHost() { return data.databaseHost; }
    public int getDatabasePort() { return data.databasePort; }
    public String getDatabaseName() { return data.databaseName; }
    public String getDatabaseUsername() { return data.databaseUsername; }
    public String getDatabasePassword() { return data.databasePassword; }
    public int getDatabasePoolSize() { return data.databasePoolSize; }
    public String getStoragePath() { return data.storagePath; }

    public static final class ConfigData {
        public final boolean webPanelEnabled;
        public final int webPanelPort;
        public final boolean webRequireSsl;
        public final int tokenExpiryDays;
        public final java.util.List<String> allowedIps;

        public final boolean loggingBlockPlace;
        public final boolean loggingBlockBreak;
        public final boolean loggingContainerAccess;
        public final boolean loggingInventoryChange;
        public final boolean loggingPlayerDeath;
        public final boolean loggingEntityDeath;
        public final boolean loggingNaturalChanges;
        public final boolean loggingExplosions;
        public final int dedupTimeMs;

        public final int blocksPerTick;
        public final boolean requireConfirmation;
        public final int checkpointInterval;
        public final int maxConcurrentRollbacks;

        public final String retentionMode;
        public final int retentionMaxDays;
        public final int retentionMaxSizeMb;
        public final double retentionEvictRatio;
        public final int purgeIntervalHours;

        public final String databaseType;
        public final String databaseHost;
        public final int databasePort;
        public final String databaseName;
        public final String databaseUsername;
        public final String databasePassword;
        public final int databasePoolSize;
        public final String storagePath;

        ConfigData(org.bukkit.configuration.file.FileConfiguration config) {
            ConfigurationSection web = config.getConfigurationSection("web-panel");
            this.webPanelEnabled = web.getBoolean("enabled", true);
            this.webPanelPort = web.getInt("port", 9812);
            this.webRequireSsl = web.getBoolean("require-ssl", false);
            this.tokenExpiryDays = web.getInt("token-expiry-days", 7);
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
            this.maxConcurrentRollbacks = rb.getInt("max-concurrent", 3);

            ConfigurationSection ret = config.getConfigurationSection("retention");
            this.retentionMode = ret.getString("mode", "age");
            this.retentionMaxDays = ret.getInt("max-days", 90);
            this.retentionMaxSizeMb = ret.getInt("max-size-mb", 1024);
            this.retentionEvictRatio = ret.getDouble("evict-ratio", 0.05);
            this.purgeIntervalHours = ret.getInt("purge-interval-hours", 6);

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
