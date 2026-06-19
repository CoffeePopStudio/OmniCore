package cn.oneachina.onmiCore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private static final int DATABASE_VERSION = 2;

    private final JavaPlugin plugin;
    private final String type;
    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.type = plugin.getConfig().getString("database.type", "SQLite");
    }

    public void initialize() {
        try {
            if (type.equalsIgnoreCase("MySQL")) {
                setupMySQL();
            } else {
                setupSQLite();
            }
            createTables();
            DatabaseMigrator.migrate(this, DATABASE_VERSION);
            plugin.getSLF4JLogger().info("Database initialized: {}", type);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to initialize database", e);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    private void setupSQLite() {
        HikariConfig config = new HikariConfig();
        String storagePath = plugin.getConfig().getString("storage-path", "plugins/OnmiCore/data");
        File dataDir = new File(storagePath);
        dataDir.mkdirs();
        File dbFile = new File(dataDir, "onmicore.db");

        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("OnmiCore-SQLite");
        config.setMinimumIdle(0);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(60000);

        dataSource = new HikariDataSource(config);
    }

    private void setupMySQL() {
        HikariConfig config = new HikariConfig();
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "onmicore");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");
        int poolSize = plugin.getConfig().getInt("database.mysql.pool-size", 10);
        boolean useSSL = plugin.getConfig().getBoolean("database.mysql.use-ssl", false);
        boolean verifyServerCertificate = plugin.getConfig().getBoolean("database.mysql.verify-server-certificate", false);
        String trustCertificateKeyStoreUrl = plugin.getConfig().getString("database.mysql.trust-certificate-keystore-url", "");

        StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql://")
            .append(host).append(":").append(port).append("/").append(database)
            .append("?serverTimezone=UTC");
        jdbcUrl.append("&useSSL=").append(useSSL);
        if (useSSL && verifyServerCertificate && !trustCertificateKeyStoreUrl.isEmpty()) {
            jdbcUrl.append("&verifyServerCertificate=true");
            jdbcUrl.append("&trustCertificateKeyStoreUrl=").append(trustCertificateKeyStoreUrl);
            String trustKeyStorePassword = plugin.getConfig().getString("database.mysql.trust-certificate-keystore-password", "");
            if (!trustKeyStorePassword.isEmpty()) {
                jdbcUrl.append("&trustCertificateKeyStorePassword=").append(trustKeyStorePassword);
            }
        } else if (useSSL) {
            jdbcUrl.append("&verifyServerCertificate=false");
        }

        config.setJdbcUrl(jdbcUrl.toString());
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(2);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("OnmiCore-MySQL");
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);

        dataSource = new HikariDataSource(config);
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            boolean isMySQL = type.equalsIgnoreCase("MySQL");
            String blobType = isMySQL ? "LONGBLOB" : "BLOB";
            String textType = isMySQL ? "TEXT" : "TEXT";
            String bigInt = isMySQL ? "BIGINT" : "INTEGER";
            String autoIncrement = isMySQL ? "BIGINT AUTO_INCREMENT" : "INTEGER PRIMARY KEY AUTOINCREMENT";
            String tsDefault = isMySQL ? "TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3)" : "TEXT";

            stmt.execute("CREATE TABLE IF NOT EXISTS block_records ("
                    + "id " + autoIncrement + ","
                    + "world " + textType + " NOT NULL,"
                    + "x INT NOT NULL,"
                    + "y INT NOT NULL,"
                    + "z INT NOT NULL,"
                    + "player_uuid " + textType + ","
                    + "player_name " + textType + ","
                    + "action " + textType + " NOT NULL,"
                    + "old_block_type " + textType + ","
                    + "new_block_type " + textType + ","
                    + "old_block_data " + blobType + ","
                    + "new_block_data " + blobType + ","
                    + "timestamp " + tsDefault + ","
                    + "rollback_id INT"
                    + (isMySQL ? ",PRIMARY KEY (id)" : "")
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS container_records ("
                    + "id " + autoIncrement + ","
                    + "world " + textType + " NOT NULL,"
                    + "x INT NOT NULL,"
                    + "y INT NOT NULL,"
                    + "z INT NOT NULL,"
                    + "player_uuid " + textType + ","
                    + "player_name " + textType + ","
                    + "action " + textType + " NOT NULL,"
                    + "item_type " + textType + ","
                    + "item_amount INT,"
                    + "item_data " + blobType + ","
                    + "timestamp " + tsDefault + ","
                    + "rollback_id INT"
                    + (isMySQL ? ",PRIMARY KEY (id)" : "")
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS inventory_records ("
                    + "id " + autoIncrement + ","
                    + "player_uuid " + textType + " NOT NULL,"
                    + "player_name " + textType + ","
                    + "action " + textType + " NOT NULL,"
                    + "item_type " + textType + ","
                    + "item_amount INT,"
                    + "item_data " + blobType + ","
                    + "timestamp " + tsDefault + ","
                    + "rollback_id INT"
                    + (isMySQL ? ",PRIMARY KEY (id)" : "")
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS rollback_checkpoints ("
                    + "id " + autoIncrement + ","
                    + "rollback_ticket " + textType + " NOT NULL,"
                    + "processed_count INT NOT NULL DEFAULT 0,"
                    + "total_count INT NOT NULL DEFAULT 0,"
                    + "status " + textType + " NOT NULL DEFAULT 'running',"
                    + "started_at " + tsDefault + ","
                    + "completed_at " + textType
                    + (isMySQL ? ",PRIMARY KEY (id)" : "")
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS web_users ("
                    + "id " + autoIncrement + ","
                    + "player_uuid " + textType + " NOT NULL UNIQUE,"
                    + "player_name " + textType + ","
                    + "username " + textType + " NOT NULL,"
                    + "password_hash " + textType + " NOT NULL,"
                    + "token_encrypted " + textType + ","
                    + "created_at " + tsDefault + ","
                    + "last_login " + textType
                    + (isMySQL ? ",PRIMARY KEY (id)" : "")
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS meta ("
                    + "key_name " + textType + " PRIMARY KEY,"
                    + "value " + textType
                    + ")");

            stmt.execute("INSERT OR IGNORE INTO meta (key_name, value) VALUES ('version', '1')");

            createIndexes(stmt, isMySQL);
        }
    }

    private void createIndexes(Statement stmt, boolean isMySQL) {
        String[] indexStatements = {
            "CREATE INDEX IF NOT EXISTS idx_block_ts ON block_records(timestamp)",
            "CREATE INDEX IF NOT EXISTS idx_block_pos ON block_records(world, x, y, z)",
            "CREATE INDEX IF NOT EXISTS idx_block_player ON block_records(player_name)",
            "CREATE INDEX IF NOT EXISTS idx_block_rollback ON block_records(rollback_id)",
            "CREATE INDEX IF NOT EXISTS idx_container_ts ON container_records(timestamp)",
            "CREATE INDEX IF NOT EXISTS idx_container_pos ON container_records(world, x, y, z)",
            "CREATE INDEX IF NOT EXISTS idx_container_player ON container_records(player_name)",
            "CREATE INDEX IF NOT EXISTS idx_inventory_ts ON inventory_records(timestamp)",
            "CREATE INDEX IF NOT EXISTS idx_inventory_player ON inventory_records(player_name)",
        };

        for (String indexSql : indexStatements) {
            try {
                stmt.execute(indexSql);
            } catch (Exception ignored) {
            }
        }

        if (isMySQL) {
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_action ON block_records(action)");
            } catch (Exception ignored) {
            }
        }

        plugin.getSLF4JLogger().info("Database indexes verified");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database not initialized");
        }
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getSLF4JLogger().info("Database connection closed");
        }
    }

    public boolean isMySQL() {
        return type.equalsIgnoreCase("MySQL");
    }

    public String getType() {
        return type;
    }
}
