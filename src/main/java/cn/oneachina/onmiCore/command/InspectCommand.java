package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.BlockRecord;
import cn.oneachina.onmiCore.util.DatabaseUtil;
import cn.oneachina.onmiCore.util.SqlBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InspectCommand implements SubCommand, Listener {

    private final OnmiCore plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, Boolean> inspectMode = new ConcurrentHashMap<>();

    public InspectCommand() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void execute(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean current = inspectMode.getOrDefault(uuid, false);
        if (current) {
            inspectMode.put(uuid, false);
            player.sendMessage(plugin.getMessageManager().get("inspect-disabled"));
        } else {
            inspectMode.put(uuid, true);
            player.sendMessage(plugin.getMessageManager().get("inspect-enabled"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!inspectMode.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        event.setCancelled(true);

        var block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        String world = block.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<BlockRecord> records = queryRecentRecords(world, x, y, z);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (records.isEmpty()) {
                    player.sendMessage(plugin.getMessageManager().get("query-no-results"));
                    return;
                }
                player.sendMessage(plugin.getMessageManager().get("query-header", records.size()));
                int index = 1;
                for (BlockRecord record : records) {
                    String blockType;
                    String actionVerb;
                    switch (record.action) {
                        case "break" -> {
                            blockType = record.oldBlockType != null ? record.oldBlockType : "unknown";
                            actionVerb = "破坏了";
                        }
                        case "place" -> {
                            blockType = record.newBlockType != null ? record.newBlockType : "unknown";
                            actionVerb = "放置了";
                        }
                        case "container" -> {
                            blockType = record.newBlockType != null ? record.newBlockType : "container";
                            actionVerb = "操作了容器";
                        }
                        case "inventory" -> {
                            blockType = "inventory";
                            actionVerb = "操作了物品";
                        }
                        default -> {
                            blockType = record.newBlockType != null ? record.newBlockType : (record.oldBlockType != null ? record.oldBlockType : "unknown");
                            actionVerb = "操作了";
                        }
                    }
                    String locStr = record.world + " " + record.x + "," + record.y + "," + record.z;
                    Component msg = miniMessage.deserialize(
                            "<gray>#" + index + "</gray> <dark_gray>[</dark_gray><white>" + record.timestamp + "</white><dark_gray>]</dark_gray> " +
                            "<aqua>" + record.playerName + "</aqua> " +
                            "<red>" + actionVerb + "</red> " +
                            "<yellow>" + blockType + "</yellow> " +
                            "<dark_gray>@</dark_gray> <green>" + locStr + "</green>"
                    );
                    player.sendMessage(msg);
                    index++;
                }
            });
        });
    }

    private List<BlockRecord> queryRecentRecords(String world, int x, int y, int z) {
        SqlBuilder sql = SqlBuilder.select("*", "block_records")
                .where("world = ?", world)
                .and("x = ?", x)
                .and("y = ?", y)
                .and("z = ?", z)
                .orderBy("timestamp DESC")
                .limit(10);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            return DatabaseUtil.query(conn, sql.build(), sql.getParams(), BlockRecord.MAPPER);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to query block records for inspect", e);
            return List.of();
        }
    }
}
