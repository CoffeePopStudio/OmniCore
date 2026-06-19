package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class LogsCommand implements SubCommand {

    private final OnmiCore plugin;

    public LogsCommand() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        final int lines;
        if (args.length > 0) {
            try {
                int parsed = Integer.parseInt(args[0]);
                if (parsed < 1) parsed = 1;
                if (parsed > 500) parsed = 500;
                lines = parsed;
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid number. Usage: /oc logs [lines]", NamedTextColor.RED));
                return;
            }
        } else {
            lines = 50;
        }

        File logFile = new File(plugin.getDataFolder().getParentFile(), "latest.log");
        if (!logFile.exists()) {
            sender.sendMessage(Component.text("No log file found.", NamedTextColor.RED));
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<String> allLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
                int from = Math.max(0, allLines.size() - lines);
                List<String> tail = allLines.subList(from, allLines.size());

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(Component.text("--- Latest " + tail.size() + " lines (of " + allLines.size() + ") ---", NamedTextColor.GRAY));
                    for (String line : tail) {
                        if (line.length() > 200) {
                            line = line.substring(0, 200) + "...";
                        }
                        sender.sendMessage(Component.text(line, NamedTextColor.WHITE));
                    }
                    sender.sendMessage(Component.text("--- End of log ---", NamedTextColor.GRAY));
                });
            } catch (IOException e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(Component.text("Failed to read log file: " + e.getMessage(), NamedTextColor.RED));
                });
            }
        });
    }
}