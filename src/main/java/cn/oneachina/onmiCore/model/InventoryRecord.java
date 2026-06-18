package cn.oneachina.onmiCore.model;

public final class InventoryRecord {
    public long id;
    public String playerUuid;
    public String playerName;
    public String action;
    public String itemType;
    public int itemAmount;
    public byte[] itemData;
    public String timestamp;
    public int rollbackId;
}
