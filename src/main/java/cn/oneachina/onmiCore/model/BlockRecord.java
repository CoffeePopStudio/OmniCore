package cn.oneachina.onmiCore.model;

public final class BlockRecord {
    public long id;
    public String world;
    public int x;
    public int y;
    public int z;
    public String playerUuid;
    public String playerName;
    public String action;
    public String oldBlockType;
    public String newBlockType;
    public byte[] oldBlockData;
    public byte[] newBlockData;
    public String timestamp;
    public int rollbackId;
}
