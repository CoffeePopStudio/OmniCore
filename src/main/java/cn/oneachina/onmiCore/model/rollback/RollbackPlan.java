package cn.oneachina.onmiCore.model.rollback;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RollbackPlan {
    public final UUID ticket;
    public final RollbackQuery query;
    public final Instant cutoff;
    public final int totalLocations;
    public final boolean hasContainerOps;
    public final boolean hasInventoryOps;
    public final List<PreviewEntry> sampleTargets;
    public final CursorState cursorState;

    public RollbackPlan(UUID ticket, RollbackQuery query, Instant cutoff, int totalLocations,
                        boolean hasContainerOps, boolean hasInventoryOps,
                        List<PreviewEntry> sampleTargets, CursorState cursorState) {
        this.ticket = ticket;
        this.query = query;
        this.cutoff = cutoff;
        this.totalLocations = totalLocations;
        this.hasContainerOps = hasContainerOps;
        this.hasInventoryOps = hasInventoryOps;
        this.sampleTargets = sampleTargets;
        this.cursorState = cursorState;
    }

    public boolean isEmpty() {
        return totalLocations == 0;
    }
}
