package cn.oneachina.onmiCore.model.rollback;

public final class LocationBlockState {
    public final String world;
    public final int x;
    public final int y;
    public final int z;
    public final String blockDataStr;
    public final String blockType;

    public LocationBlockState(String world, int x, int y, int z, String blockDataStr, String blockType) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockDataStr = blockDataStr;
        this.blockType = blockType;
    }
}
