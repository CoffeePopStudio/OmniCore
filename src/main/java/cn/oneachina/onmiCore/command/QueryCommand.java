package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.BlockRecord;
import cn.oneachina.onmiCore.util.DatabaseUtil;
import cn.oneachina.onmiCore.util.SqlBuilder;
import cn.oneachina.onmiCore.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class QueryCommand implements SubCommand {

    private static final DateTimeFormatter MYSQL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));
    private static final int PAGE_SIZE = 10;

    private final OnmiCore plugin;

    public QueryCommand() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().get("invalid-arguments", "/oc query <time> [player:<name>] [world:<name>] [radius:<r> <x> <y> <z>] [type:<block>]"));
            return;
        }

        Duration timeAmount = StringUtil.parseTime(args[0]);
        if (timeAmount == null) {
            sender.sendMessage(plugin.getMessageManager().get("prefix").append(
                    Component.text("<red>Invalid time format. Use e.g. 30m, 1h, 7d</red>")));
            return;
        }

        String playerName = null;
        String worldName = null;
        String blockType = null;
        int radius = 0;
        int cx = 0;
        int cy = 0;
        int cz = 0;
        int page = 0;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("player:")) {
                playerName = arg.substring(7);
            } else if (arg.startsWith("world:")) {
                worldName = arg.substring(6);
            } else if (arg.startsWith("type:")) {
                blockType = arg.substring(5);
            } else if (arg.startsWith("radius:") && i + 3 < args.length) {
                radius = StringUtil.safeParseInt(arg.substring(7), 0);
                cx = StringUtil.safeParseInt(args[++i], 0);
                cy = StringUtil.safeParseInt(args[++i], 0);
                cz = StringUtil.safeParseInt(args[++i], 0);
            } else if (arg.startsWith("page:")) {
                page = StringUtil.safeParseInt(arg.substring(5), 0);
                if (page < 0) page = 0;
            }
        }

        int finalPage = page;
        String finalPlayerName = playerName;
        String finalWorldName = worldName;
        String finalBlockType = blockType;
        int finalRadius = radius;
        int finalCx = cx;
        int finalCy = cy;
        int finalCz = cz;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<BlockRecord> records = queryBlocks(finalWorldName, finalCx, finalCy, finalCz,
                    finalPlayerName, finalBlockType, timeAmount, finalRadius, finalPage, PAGE_SIZE);
            int totalCount = countBlocks(finalWorldName, finalCx, finalCy, finalCz,
                    finalPlayerName, finalBlockType, timeAmount, finalRadius);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (records.isEmpty()) {
                    sender.sendMessage(plugin.getMessageManager().get("query-no-results"));
                    return;
                }

                sender.sendMessage(plugin.getMessageManager().get("query-header", totalCount));
                int index = finalPage * PAGE_SIZE + 1;
                for (BlockRecord record : records) {
                    String actionText = switch (record.action) {
                        case "place" -> plugin.getMessageManager().raw("query-action-place");
                        case "break" -> plugin.getMessageManager().raw("query-action-break");
                        case "container" -> plugin.getMessageManager().raw("query-action-container");
                        case "inventory" -> plugin.getMessageManager().raw("query-action-inventory");
                        default -> record.action;
                    };
                    sender.sendMessage(plugin.getMessageManager().get("query-entry",
                            index++, record.timestamp, record.playerName,
                            actionText, record.newBlockType != null ? record.newBlockType : record.oldBlockType,
                            record.world + "," + record.x + "," + record.y + "," + record.z));
                }

                Component pageNav = buildPageNavigation(finalPage, totalPages, args);
                sender.sendMessage(pageNav);
            });
        });
    }

    private Component buildPageNavigation(int currentPage, int totalPages, String[] originalArgs) {
        StringBuilder baseCmd = new StringBuilder("/oc query ");
        for (String arg : originalArgs) {
            if (!arg.startsWith("page:")) {
                baseCmd.append(arg).append(" ");
            }
        }

        Component nav = plugin.getMessageManager().get("query-page", currentPage + 1, totalPages);
        if (currentPage > 0) {
            nav = nav.append(Component.text(" [")
                    .append(Component.text("◀")
                            .clickEvent(ClickEvent.runCommand(baseCmd + "page:" + (currentPage - 1))))
                    .append(Component.text("] ")));
        }
        if (currentPage + 1 < totalPages) {
            nav = nav.append(Component.text(" [")
                    .append(Component.text("▶")
                            .clickEvent(ClickEvent.runCommand(baseCmd + "page:" + (currentPage + 1))))
                    .append(Component.text("]")));
        }
        return nav;
    }

    private String formatTimestamp(Instant instant) {
        if (plugin.getDatabaseManager().isMySQL()) {
            return MYSQL_TS.format(instant);
        }
        return instant.toString();
    }

    private List<BlockRecord> queryBlocks(String world, int x, int y, int z,
                                           String playerName, String blockType,
                                           Duration timeAgo, int radius, int page, int pageSize) {
        SqlBuilder sb = SqlBuilder.select("*", "block_records");
        if (world != null && !world.isEmpty()) {
            sb.where("world = ?", world);
        }
        if (playerName != null && !playerName.isEmpty()) {
            sb.where("player_name = ?", playerName);
        }
        if (blockType != null && !blockType.isEmpty()) {
            sb.where("(old_block_type = ? OR new_block_type = ?)", blockType, blockType);
        }
        if (timeAgo != null) {
            sb.where("timestamp >= ?", formatTimestamp(Instant.now().minus(timeAgo)));
        }
        if (radius > 0) {
            sb.where("x BETWEEN ? AND ?", x - radius, x + radius);
            sb.where("z BETWEEN ? AND ?", z - radius, z + radius);
            if (!worldNullOrEmpty(world)) {
                sb.where("world = ?", world);
            }
        }
        sb.orderBy("timestamp DESC");
        sb.limit(pageSize);
        sb.offset(page * pageSize);

        String sql = sb.build();
        List<Object> params = sb.getParams();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql, params, BlockRecord.MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query block records", e);
            return new ArrayList<>();
        }
    }

    private int countBlocks(String world, int x, int y, int z,
                             String playerName, String blockType,
                             Duration timeAgo, int radius) {
        SqlBuilder sb = SqlBuilder.count("block_records");
        if (world != null && !world.isEmpty()) {
            sb.where("world = ?", world);
        }
        if (playerName != null && !playerName.isEmpty()) {
            sb.where("player_name = ?", playerName);
        }
        if (blockType != null && !blockType.isEmpty()) {
            sb.where("(old_block_type = ? OR new_block_type = ?)", blockType, blockType);
        }
        if (timeAgo != null) {
            sb.where("timestamp >= ?", formatTimestamp(Instant.now().minus(timeAgo)));
        }
        if (radius > 0) {
            sb.where("x BETWEEN ? AND ?", x - radius, x + radius);
            sb.where("z BETWEEN ? AND ?", z - radius, z + radius);
            if (!worldNullOrEmpty(world)) {
                sb.where("world = ?", world);
            }
        }

        String sql = sb.build();
        List<Object> params = sb.getParams();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.count(conn, sql, params);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count block records", e);
            return 0;
        }
    }

    private boolean worldNullOrEmpty(String world) {
        return world == null || world.isEmpty();
    }
}
