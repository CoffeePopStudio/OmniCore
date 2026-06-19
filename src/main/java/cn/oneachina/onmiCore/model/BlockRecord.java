package cn.oneachina.onmiCore.model;

import cn.oneachina.onmiCore.util.DatabaseUtil;
import cn.oneachina.onmiCore.util.LocationSerializer;

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

    public static final DatabaseUtil.RowMapper<BlockRecord> MAPPER = rs -> {
        BlockRecord r = new BlockRecord();
        r.id = rs.getLong("id");
        r.world = rs.getString("world");
        r.x = rs.getInt("x");
        r.y = rs.getInt("y");
        r.z = rs.getInt("z");
        r.playerUuid = rs.getString("player_uuid");
        r.playerName = rs.getString("player_name");
        r.action = rs.getString("action");
        r.oldBlockType = rs.getString("old_block_type");
        r.newBlockType = rs.getString("new_block_type");
        r.oldBlockData = rs.getBytes("old_block_data");
        r.newBlockData = rs.getBytes("new_block_data");
        r.timestamp = rs.getString("timestamp");
        r.rollbackId = rs.getInt("rollback_id");
        return r;
    };

    public String locationKey() {
        return LocationSerializer.toString(world, x, y, z);
    }
}
