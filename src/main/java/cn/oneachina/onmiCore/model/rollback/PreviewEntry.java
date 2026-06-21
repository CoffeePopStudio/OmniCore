package cn.oneachina.onmiCore.model.rollback;

public final class PreviewEntry {
    public final String locationKey;
    public final String targetType;
    public final String action;

    public PreviewEntry(String locationKey, String targetType, String action) {
        this.locationKey = locationKey;
        this.targetType = targetType;
        this.action = action;
    }
}
