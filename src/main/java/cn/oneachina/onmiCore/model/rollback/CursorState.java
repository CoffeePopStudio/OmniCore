package cn.oneachina.onmiCore.model.rollback;

public final class CursorState {
    public String lastTimestamp;
    public long lastId;
    public int processedBlocks;
    public int processedContainers;
    public int processedInventories;
    public boolean isFirstPage;

    public CursorState() {
        this.isFirstPage = true;
    }

    public CursorState copy() {
        CursorState copy = new CursorState();
        copy.lastTimestamp = this.lastTimestamp;
        copy.lastId = this.lastId;
        copy.processedBlocks = this.processedBlocks;
        copy.processedContainers = this.processedContainers;
        copy.processedInventories = this.processedInventories;
        copy.isFirstPage = this.isFirstPage;
        return copy;
    }
}
