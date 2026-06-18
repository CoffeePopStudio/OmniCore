package cn.oneachina.onmiCore.command;

import cn.oneachina.onmiCore.OnmiCore;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebCommand implements SubCommand {

    private final OnmiCore plugin;
    private final Map<UUID, String> bindTokens = new ConcurrentHashMap<>();

    public WebCommand() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return;
        }

        UUID playerUuid = player.getUniqueId();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean alreadyBound = checkWebUserExists(playerUuid);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (alreadyBound) {
                    player.sendMessage(plugin.getMessageManager().get("web-already-bound"));
                    return;
                }

                String bindToken = UUID.randomUUID().toString();
                bindTokens.put(playerUuid, bindToken);

                int port = plugin.getConfigManager().getWebPanelPort();
                String url = "http://localhost:" + port + "/bind?token=" + bindToken + "&uuid=" + playerUuid;

                player.sendMessage(plugin.getMessageManager().get("web-link", url));
                player.sendMessage(plugin.getMessageManager().get("web-link-expires", 30));
            });
        });
    }

    private boolean checkWebUserExists(UUID playerUuid) {
        String sql = "SELECT COUNT(*) FROM web_users WHERE player_uuid = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             ps.setString(1, playerUuid.toString());
             try (ResultSet rs = ps.executeQuery()) {
                 if (rs.next()) {
                     return rs.getInt(1) > 0;
                 }
             }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to check web user existence", e);
        }

        return false;
    }

    public String consumeBindToken(UUID playerUuid) {
        return bindTokens.remove(playerUuid);
    }
}
