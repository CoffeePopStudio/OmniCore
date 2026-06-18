package cn.oneachina.onmiCore.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public final class DatabaseMigrator {

    private DatabaseMigrator() {}

    public static void migrate(DatabaseManager db, int targetVersion) {
        int currentVersion = getCurrentVersion(db);
        if (currentVersion >= targetVersion) return;

        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            if (currentVersion < 1) {
                // Initial schema is created by DatabaseManager.createTables()
            }

            stmt.execute("INSERT OR REPLACE INTO meta (key_name, value) VALUES ('version', '" + targetVersion + "')");

        } catch (Exception e) {
            JavaPlugin.getPlugin(JavaPlugin.class).getSLF4JLogger()
                    .error("Database migration failed", e);
        }
    }

    private static int getCurrentVersion(DatabaseManager db) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT value FROM meta WHERE key_name = 'version'")) {
            if (rs.next()) {
                return Integer.parseInt(rs.getString("value"));
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
