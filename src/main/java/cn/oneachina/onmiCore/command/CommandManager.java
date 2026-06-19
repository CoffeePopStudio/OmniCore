package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CommandManager implements CommandExecutor, TabCompleter {
    private final OnmiCore plugin;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public CommandManager() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        registerAll();
    }

    private void registerAll() {
        subCommands.put("inspect", new InspectCommand());
        subCommands.put("query", new QueryCommand());
        subCommands.put("rollback", new RollbackCommand());
        subCommands.put("restore", new RestoreCommand());
        subCommands.put("run/plugins/OnmiCore/web", new WebCommand());
        subCommands.put("purge", new PurgeCommand());
        subCommands.put("status", new AdminCommand());
        subCommands.put("reload", new AdminCommand());
        subCommands.put("migrate-world", new AdminCommand());
        subCommands.put("logs", new LogsCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().get("prefix").append(
                    Component.text(" /oc <inspect|query|rollback|restore|web|purge|status|reload|logs>")));
            return true;
        }
        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) {
            sender.sendMessage(plugin.getMessageManager().get("unknown-command"));
            return true;
        }
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        sub.execute(sender, subArgs);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub != null) {
            return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        return Collections.emptyList();
    }

    public Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }
}
