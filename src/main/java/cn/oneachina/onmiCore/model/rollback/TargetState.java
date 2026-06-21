package cn.oneachina.onmiCore.model.rollback;

public final class TargetState {
    public final String blockType;
    public final byte[] blockData;

    public TargetState(String blockType, byte[] blockData) {
        this.blockType = blockType;
        this.blockData = blockData;
    }
}
