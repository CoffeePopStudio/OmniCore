package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.service.PurgeService;
import cn.oneachina.onmiCore.util.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class PurgeCommand implements SubCommand {

    private final OnmiCore plugin;
    private final PurgeService purgeService;

    public PurgeCommand() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        this.purgeService = plugin.getPurgeService();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!PermissionUtil.has(sender, "onmicore.admin")) {
            sender.sendMessage(plugin.getMessageManager().get("command-no-permission"));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().get("invalid-arguments", "/oc purge <days>"));
            return;
        }

        int days;
        try {
            days = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().get("prefix").append(
                    net.kyori.adventure.text.Component.text("<red>Invalid number</red>")));
            return;
        }

        sender.sendMessage(plugin.getMessageManager().get("purge-start", days));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int purged = purgeService.purgeByAge(days);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (purged == 0) {
                    sender.sendMessage(plugin.getMessageManager().get("purge-no-data"));
                } else {
                    sender.sendMessage(plugin.getMessageManager().get("purge-complete", purged));
                }
            });
        });
    }
}
