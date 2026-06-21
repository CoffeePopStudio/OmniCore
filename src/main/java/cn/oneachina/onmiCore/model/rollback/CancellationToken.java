package cn.oneachina.onmiCore.model.rollback;

public final class CancellationToken {
    private volatile boolean cancelled = false;

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
