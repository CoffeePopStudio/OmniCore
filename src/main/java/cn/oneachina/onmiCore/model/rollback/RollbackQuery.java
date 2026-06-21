package cn.oneachina.onmiCore.model.rollback;

import org.bukkit.Location;

import java.time.Duration;

public final class RollbackQuery {
    private final Duration timeAmount;
    private final String playerName;
    private final String worldName;
    private final int radius;
    private final Location center;
    private final String blockType;

    public RollbackQuery(Duration timeAmount, String playerName, String worldName, int radius, Location center, String blockType) {
        this.timeAmount = timeAmount;
        this.playerName = playerName;
        this.worldName = worldName;
        this.radius = radius;
        this.center = center;
        this.blockType = blockType;
    }

    public Duration getTimeAmount() { return timeAmount; }
    public String getPlayerName() { return playerName; }
    public String getWorldName() { return worldName; }
    public int getRadius() { return radius; }
    public Location getCenter() { return center; }
    public String getBlockType() { return blockType; }
}
