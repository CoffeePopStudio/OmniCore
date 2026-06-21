package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.rollback.PreviewEntry;
import cn.oneachina.onmiCore.model.rollback.RollbackPlan;
import cn.oneachina.onmiCore.model.rollback.RollbackQuery;
import cn.oneachina.onmiCore.service.RollbackService;
import cn.oneachina.onmiCore.util.PermissionUtil;
import cn.oneachina.onmiCore.util.StringUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RollbackCommand implements SubCommand {

    private final OnmiCore plugin;
    private final RollbackService rollbackService;
    private final Map<UUID, UUID> pendingConfirmations = new ConcurrentHashMap<>();

    public RollbackCommand() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        this.rollbackService = plugin.getRollbackService();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!PermissionUtil.has(sender, "onmicore.rollback")) {
            sender.sendMessage(plugin.getMessageManager().get("command-no-permission"));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().get("invalid-arguments",
                    "/oc rollback <time> [player:<name>] [world:<name>] [radius:<r> <x> <y> <z>] [type:<block>]"));
            return;
        }

        if (args[0].equalsIgnoreCase("confirm")) {
            handleConfirm(sender);
            return;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            handleCancel(sender);
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
            }
        }

        Location center = null;
        if (radius > 0) {
            if (sender instanceof Player player) {
                if (worldName == null) {
                    worldName = player.getWorld().getName();
                }
                center = new Location(player.getServer().getWorld(worldName), cx, cy, cz);
            } else {
                sender.sendMessage(Component.text("<red>Radius requires a player context</red>"));
                return;
            }
        }

        RollbackQuery query = new RollbackQuery(
                timeAmount, playerName, worldName, radius, center, blockType);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            RollbackPlan plan = rollbackService.buildRollbackPlanWithTimeout(query, 30000);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (plan == null) {
                    sender.sendMessage("§cRollback query timed out or failed. Try narrowing the search range.");
                    return;
                }

                if (plan.isEmpty()) {
                    sender.sendMessage(plugin.getMessageManager().get("query-no-results"));
                    return;
                }

                sender.sendMessage(plugin.getMessageManager().get("rollback-preview"));
                sender.sendMessage(plugin.getMessageManager().get("rollback-time-range",
                        "now - " + formatDuration(timeAmount), "now"));
                sender.sendMessage(plugin.getMessageManager().get("rollback-affected-locations", plan.totalLocations));

                for (PreviewEntry entry : plan.sampleTargets) {
                    sender.sendMessage("  " + entry.locationKey + " → " + entry.targetType);
                }

                if (plan.hasContainerOps) {
                    sender.sendMessage("  [container operations to reverse]");
                }

                sender.sendMessage(plugin.getMessageManager().get("rollback-confirm"));
                sender.sendMessage(plugin.getMessageManager().get("rollback-cancel"));

                UUID senderId = ((Player) sender).getUniqueId();
                UUID ticket = rollbackService.prepareRollback(plan, sender);
                if (ticket != null) {
                    pendingConfirmations.put(senderId, ticket);
                }
            });
        });
    }

    private void handleConfirm(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return;
        }

        UUID ticket = pendingConfirmations.remove(player.getUniqueId());
        if (ticket == null) {
            sender.sendMessage(plugin.getMessageManager().get("rollback-no-preview"));
            return;
        }

        sender.sendMessage(plugin.getMessageManager().get("rollback-started"));
        rollbackService.confirmRollback(ticket);
    }

    private void handleCancel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return;
        }

        UUID ticket = pendingConfirmations.remove(player.getUniqueId());
        if (ticket == null) {
            sender.sendMessage(plugin.getMessageManager().get("rollback-no-preview"));
            return;
        }

        rollbackService.cancelRollback(ticket);
        sender.sendMessage(plugin.getMessageManager().get("rollback-cancelled"));
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatDuration(Duration duration) {
        if (duration.toDays() > 0) return duration.toDays() + "d";
        if (duration.toHours() > 0) return duration.toHours() + "h";
        return duration.toMinutes() + "m";
    }
}
