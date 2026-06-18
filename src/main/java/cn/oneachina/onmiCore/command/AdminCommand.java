package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.util.PermissionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class AdminCommand implements SubCommand {

    private final OnmiCore plugin;

    public AdminCommand() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!PermissionUtil.has(sender, "onmicore.admin")) {
            sender.sendMessage(plugin.getMessageManager().get("command-no-permission"));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().get("invalid-arguments", "/oc <status|reload|migrate-world <old> <new>>"));
            return;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "status" -> handleStatus(sender);
            case "reload" -> handleReload(sender);
            case "migrate-world" -> handleMigrateWorld(sender, args);
            default -> sender.sendMessage(plugin.getMessageManager().get("unknown-command"));
        }
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().get("plugin-status"));
        sender.sendMessage(plugin.getMessageManager().get("plugin-version",
                plugin.getPluginMeta().getVersion()));
        sender.sendMessage(plugin.getMessageManager().get("database-status",
                plugin.getDatabaseManager().getType(),
                plugin.getDatabaseManager().isMySQL() ? "MySQL" : "SQLite"));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            long recordCount = getTotalRecordCount();
            boolean webEnabled = plugin.getConfigManager().isWebPanelEnabled();
            int webPort = plugin.getConfigManager().getWebPanelPort();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getMessageManager().get("record-count", recordCount));
                sender.sendMessage(plugin.getMessageManager().get("web-status",
                        webEnabled ? "Running" : "Disabled", webPort));
            });
        });
    }

    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(plugin.getMessageManager().get("config-reloaded"));
    }

    private void handleMigrateWorld(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().get("invalid-arguments", "/oc migrate-world <oldName> <newName>"));
            return;
        }

        String oldName = args[1];
        String newName = args[2];

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int updated = 0;
            updated += updateWorldName("block_records", oldName, newName);
            updated += updateWorldName("container_records", oldName, newName);

            int finalUpdated = updated;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getMessageManager().get("prefix").append(
                        Component.text("<green>Updated " + finalUpdated + " records from world '" + oldName + "' to '" + newName + "'</green>")));
            });
        });
    }

    private long getTotalRecordCount() {
        String sql = "SELECT (SELECT COUNT(*) FROM block_records) + (SELECT COUNT(*) FROM container_records) + (SELECT COUNT(*) FROM inventory_records)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to get total record count", e);
        }

        return 0;
    }

    private int updateWorldName(String table, String oldName, String newName) {
        String sql = "UPDATE " + table + " SET world = ? WHERE world = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            return ps.executeUpdate();
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to update world name in {}", table, e);
        }

        return 0;
    }
}
