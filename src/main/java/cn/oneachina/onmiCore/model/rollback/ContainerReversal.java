package cn.oneachina.onmiCore.model.rollback;

public final class ContainerReversal {
    public final String world;
    public final int x;
    public final int y;
    public final int z;
    public final String action;
    public final String itemType;
    public final int itemAmount;
    public final byte[] itemData;
    public final String playerUuid;
    public final String playerName;

    public ContainerReversal(String world, int x, int y, int z, String action, String itemType, int itemAmount, byte[] itemData, String playerUuid, String playerName) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.action = action;
        this.itemType = itemType;
        this.itemAmount = itemAmount;
        this.itemData = itemData;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }
}
