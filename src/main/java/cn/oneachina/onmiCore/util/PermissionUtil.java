package cn.oneachina.onmiCore.util;

import org.bukkit.command.CommandSender;

public final class PermissionUtil {

    public static boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    public static boolean hasView(CommandSender sender) {
        return sender.hasPermission("onmicore.web.view");
    }

    public static boolean hasRollback(CommandSender sender) {
        return sender.hasPermission("onmicore.web.rollback");
    }

    public static boolean hasAdmin(CommandSender sender) {
        return sender.hasPermission("onmicore.web.admin");
    }
}
