package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.service.RollbackService;
import cn.oneachina.onmiCore.util.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class RestoreCommand implements SubCommand {

    private final OnmiCore plugin;
    private final RollbackService rollbackService;

    public RestoreCommand() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        this.rollbackService = plugin.getRollbackService();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!PermissionUtil.has(sender, "onmicore.rollback")) {
            sender.sendMessage(plugin.getMessageManager().get("command-no-permission"));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().get("invalid-arguments", "/oc restore <rollbackId>"));
            return;
        }

        int rollbackId;
        try {
            rollbackId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().get("prefix").append(
                    net.kyori.adventure.text.Component.text("<red>Invalid rollback ID</red>")));
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            rollbackService.restoreRollback(rollbackId, sender);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getMessageManager().get("restore-success", rollbackId));
            });
        });
    }
}
