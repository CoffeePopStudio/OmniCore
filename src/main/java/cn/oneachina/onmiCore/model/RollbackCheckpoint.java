package cn.oneachina.onmiCore.model;

import java.util.UUID;

public final class RollbackCheckpoint {
    public long id;
    public UUID rollbackTicket;
    public int processedCount;
    public int totalCount;
    public String status;
    public long startedAt;
    public long completedAt;
}
