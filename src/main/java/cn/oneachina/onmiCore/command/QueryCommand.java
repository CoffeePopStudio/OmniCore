package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.BlockRecord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryCommand implements SubCommand {

    private static final DateTimeFormatter MYSQL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([mhd])$");
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

        Duration timeAmount = parseTime(args[0]);
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
                radius = parseInt(arg.substring(7));
                cx = parseInt(args[++i]);
                cy = parseInt(args[++i]);
                cz = parseInt(args[++i]);
            } else if (arg.startsWith("page:")) {
                page = parseInt(arg.substring(5));
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

    private Duration parseTime(String input) {
        Matcher matcher = TIME_PATTERN.matcher(input);
        if (!matcher.matches()) {
            return null;
        }
        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);
        return switch (unit) {
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> null;
        };
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
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
        List<BlockRecord> records = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM block_records WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (world != null && !world.isEmpty()) {
            sql.append(" AND world = ?");
            params.add(world);
        }
        if (playerName != null && !playerName.isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(playerName);
        }
        if (blockType != null && !blockType.isEmpty()) {
            sql.append(" AND (old_block_type = ? OR new_block_type = ?)");
            params.add(blockType);
            params.add(blockType);
        }
        if (timeAgo != null) {
            sql.append(" AND timestamp >= ?");
            params.add(formatTimestamp(Instant.now().minus(timeAgo)));
        }
        if (radius > 0) {
            sql.append(" AND x BETWEEN ? AND ? AND z BETWEEN ? AND ?");
            params.add(x - radius);
            params.add(x + radius);
            params.add(z - radius);
            params.add(z + radius);
            if (!worldNullOrEmpty(world)) {
                sql.append(" AND world = ?");
                params.add(world);
            }
        }

        sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(page * pageSize);

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BlockRecord record = new BlockRecord();
                    record.id = rs.getLong("id");
                    record.world = rs.getString("world");
                    record.x = rs.getInt("x");
                    record.y = rs.getInt("y");
                    record.z = rs.getInt("z");
                    record.playerUuid = rs.getString("player_uuid");
                    record.playerName = rs.getString("player_name");
                    record.action = rs.getString("action");
                    record.oldBlockType = rs.getString("old_block_type");
                    record.newBlockType = rs.getString("new_block_type");
                    record.oldBlockData = rs.getBytes("old_block_data");
                    record.newBlockData = rs.getBytes("new_block_data");
                    record.timestamp = rs.getString("timestamp");
                    record.rollbackId = rs.getInt("rollback_id");
                    records.add(record);
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query block records", e);
        }

        return records;
    }

    private int countBlocks(String world, int x, int y, int z,
                             String playerName, String blockType,
                             Duration timeAgo, int radius) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM block_records WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (world != null && !world.isEmpty()) {
            sql.append(" AND world = ?");
            params.add(world);
        }
        if (playerName != null && !playerName.isEmpty()) {
            sql.append(" AND player_name = ?");
            params.add(playerName);
        }
        if (blockType != null && !blockType.isEmpty()) {
            sql.append(" AND (old_block_type = ? OR new_block_type = ?)");
            params.add(blockType);
            params.add(blockType);
        }
        if (timeAgo != null) {
            sql.append(" AND timestamp >= ?");
            params.add(formatTimestamp(Instant.now().minus(timeAgo)));
        }
        if (radius > 0) {
            sql.append(" AND x BETWEEN ? AND ? AND z BETWEEN ? AND ?");
            params.add(x - radius);
            params.add(x + radius);
            params.add(z - radius);
            params.add(z + radius);
            if (!worldNullOrEmpty(world)) {
                sql.append(" AND world = ?");
                params.add(world);
            }
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to count block records", e);
        }

        return 0;
    }

    private boolean worldNullOrEmpty(String world) {
        return world == null || world.isEmpty();
    }
}
