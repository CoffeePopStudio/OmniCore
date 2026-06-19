package cn.oneachina.onmiCore.model;

import cn.oneachina.onmiCore.util.DatabaseUtil;

public final class ContainerRecord {
    public long id;
    public String world;
    public int x;
    public int y;
    public int z;
    public String playerUuid;
    public String playerName;
    public String action;
    public String itemType;
    public int itemAmount;
    public byte[] itemData;
    public String timestamp;
    public int rollbackId;

    public static final DatabaseUtil.RowMapper<ContainerRecord> MAPPER = rs -> {
        ContainerRecord r = new ContainerRecord();
        r.id = rs.getLong("id");
        r.world = rs.getString("world");
        r.x = rs.getInt("x");
        r.y = rs.getInt("y");
        r.z = rs.getInt("z");
        r.playerUuid = rs.getString("player_uuid");
        r.playerName = rs.getString("player_name");
        r.action = rs.getString("action");
        r.itemType = rs.getString("item_type");
        r.itemAmount = rs.getInt("item_amount");
        r.itemData = rs.getBytes("item_data");
        r.timestamp = rs.getString("timestamp");
        r.rollbackId = rs.getInt("rollback_id");
        return r;
    };
}
