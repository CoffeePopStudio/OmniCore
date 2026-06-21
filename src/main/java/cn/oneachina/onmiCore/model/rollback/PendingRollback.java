package cn.oneachina.onmiCore.model.rollback;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

public final class PendingRollback {
    public final UUID ticket;
    public final RollbackQuery query;
    public final CommandSender sender;
    public final List<LocationBlockState> blockTargets;
    public final List<ContainerReversal> containerReversals;
    public final List<InventoryReversal> inventoryReversals;

    public PendingRollback(UUID ticket, RollbackQuery query, CommandSender sender,
                           List<LocationBlockState> blockTargets,
                           List<ContainerReversal> containerReversals,
                           List<InventoryReversal> inventoryReversals) {
        this.ticket = ticket;
        this.query = query;
        this.sender = sender;
        this.blockTargets = blockTargets;
        this.containerReversals = containerReversals;
        this.inventoryReversals = inventoryReversals;
    }
}
