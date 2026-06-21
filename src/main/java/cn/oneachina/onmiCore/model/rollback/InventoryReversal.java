package cn.oneachina.onmiCore.model.rollback;

public final class InventoryReversal {
    public final String playerUuid;
    public final String playerName;
    public final String action;
    public final String itemType;
    public final int itemAmount;
    public final byte[] itemData;

    public InventoryReversal(String playerUuid, String playerName, String action, String itemType, int itemAmount, byte[] itemData) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.action = action;
        this.itemType = itemType;
        this.itemAmount = itemAmount;
        this.itemData = itemData;
    }
}
