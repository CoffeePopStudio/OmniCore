package cn.oneachina.onmiCore.service;

import cn.oneachina.onmiCore.OnmiCore;
import cn.oneachina.onmiCore.model.BlockRecord;
import cn.oneachina.onmiCore.model.ContainerRecord;
import cn.oneachina.onmiCore.model.rollback.CancellationToken;
import cn.oneachina.onmiCore.model.rollback.ContainerReversal;
import cn.oneachina.onmiCore.model.rollback.CursorState;
import cn.oneachina.onmiCore.model.rollback.InventoryReversal;
import cn.oneachina.onmiCore.model.rollback.LocationBlockState;
import cn.oneachina.onmiCore.model.rollback.PendingRollback;
import cn.oneachina.onmiCore.model.rollback.PreviewEntry;
import cn.oneachina.onmiCore.model.rollback.RollbackPlan;
import cn.oneachina.onmiCore.model.rollback.RollbackQuery;
import cn.oneachina.onmiCore.model.rollback.TargetState;
import cn.oneachina.onmiCore.util.LocationSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class RollbackService {

    private static final int PAGE_SIZE = 1000;
    private static final long TIMEOUT_MILLIS = 30000;

    private final OnmiCore plugin;
    private final RollbackDataService dataService;
    private final Map<UUID, PendingRollback> pendingMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> progressMap = new ConcurrentHashMap<>();
    private final Semaphore rollbackSemaphore;
    private final Map<UUID, CancellationToken> cancellationTokens = new ConcurrentHashMap<>();

    public RollbackService() {
        this.plugin = JavaPlugin.getPlugin(OnmiCore.class);
        this.rollbackSemaphore = new Semaphore(plugin.getConfigManager().getData().maxConcurrentRollbacks);
        this.dataService = new RollbackDataService();
    }

    // ========== Plan Phase ==========

    public RollbackPlan buildRollbackPlan(RollbackQuery query) {
        Instant cutoff = Instant.now().minus(query.getTimeAmount());
        CursorState cursorState = new CursorState();
        Map<String, TargetState> locationTargets = new LinkedHashMap<>();

        List<BlockRecord> page;
        do {
            page = dataService.queryBlockRecordsPage(query, cutoff, cursorState, PAGE_SIZE);
            aggregateLocationTargets(page, locationTargets);
        } while (page.size() >= PAGE_SIZE);

        List<PreviewEntry> sampleTargets = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, TargetState> entry : locationTargets.entrySet()) {
            if (count >= 20) break;
            String targetType = entry.getValue().blockType != null ? entry.getValue().blockType : "air";
            sampleTargets.add(new PreviewEntry(entry.getKey(), targetType, "restore to " + targetType));
            count++;
        }

        boolean hasContainerOps = dataService.countContainerOps(query, cutoff) > 0;
        boolean hasInventoryOps = dataService.countInventoryOps(query, cutoff) > 0;

        UUID ticket = UUID.randomUUID();
        return new RollbackPlan(ticket, query, cutoff, locationTargets.size(), hasContainerOps, hasInventoryOps, sampleTargets, cursorState);
    }

    public RollbackPlan buildRollbackPlanWithTimeout(RollbackQuery query) {
        return buildRollbackPlanWithTimeout(query, TIMEOUT_MILLIS);
    }

    public RollbackPlan buildRollbackPlanWithTimeout(RollbackQuery query, long timeoutMillis) {
        try {
            return CompletableFuture.supplyAsync(() -> buildRollbackPlan(query))
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    // ========== Prepare Phase ==========

    public UUID prepareRollback(RollbackPlan plan, CommandSender sender) {
        int maxConcurrent = plugin.getConfigManager().getData().maxConcurrentRollbacks;
        if (!rollbackSemaphore.tryAcquire()) {
            if (sender != null) {
                sender.sendMessage("§cRollback queue is full (max " + maxConcurrent + " concurrent). Please wait for the current rollback to finish.");
            }
            return null;
        }

        if (plan.isEmpty()) {
            rollbackSemaphore.release();
            return null;
        }

        CancellationToken token = new CancellationToken();
        UUID ticket = plan.ticket;
        cancellationTokens.put(ticket, token);

        PendingRollback pending = new PendingRollback(ticket, plan.query, sender, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        pendingMap.put(ticket, pending);
        progressMap.put(ticket, 0);

        return ticket;
    }

    // ========== Execute Phase ==========

    public void confirmRollback(UUID ticket) {
        PendingRollback pending = pendingMap.remove(ticket);
        if (pending == null) {
            rollbackSemaphore.release();
            return;
        }

        CancellationToken token = cancellationTokens.get(ticket);
        if (token == null) {
            rollbackSemaphore.release();
            return;
        }

        int rollbackId = dataService.nextRollbackId();
        RollbackQuery query = pending.query;
        Instant cutoff = Instant.now().minus(query.getTimeAmount());
        CursorState cursorState = new CursorState();

        List<ContainerReversal> containerReversals = new ArrayList<>();
        List<ContainerRecord> containerRecords = dataService.queryContainerRecords(query, cutoff);
        for (ContainerRecord cr : containerRecords) {
            String reversedAction = cr.action.equalsIgnoreCase("withdraw") ? "deposit" : "withdraw";
            containerReversals.add(new ContainerReversal(cr.world, cr.x, cr.y, cr.z, reversedAction, cr.itemType, cr.itemAmount, cr.itemData, cr.playerUuid, cr.playerName));
        }

        List<InventoryReversal> inventoryReversals = new ArrayList<>();
        List<ContainerRecord> inventoryRecords = dataService.queryInventoryRecords(query, cutoff);
        for (ContainerRecord ir : inventoryRecords) {
            String reversedAction = ir.action.equalsIgnoreCase("pickup") ? "drop" : "pickup";
            inventoryReversals.add(new InventoryReversal(ir.playerUuid, ir.playerName, reversedAction, ir.itemType, ir.itemAmount, ir.itemData));
        }

        int blocksPerTick = plugin.getConfigManager().getBlocksPerTick();
        int checkpointInterval = plugin.getConfigManager().getCheckpointInterval();

        progressMap.put(ticket, 0);

        new BukkitRunnable() {
            int processedCount = 0;
            int containerIndex = 0;
            int inventoryIndex = 0;
            String phase = "block";

            @Override
            public void run() {
                if (token.isCancelled()) {
                    dataService.saveCheckpoint(ticket, rollbackId, processedCount, Math.max(processedCount, 1), "cancelled");
                    rollbackSemaphore.release();
                    cancellationTokens.remove(ticket);
                    cancel();
                    return;
                }

                int processedThisTick = 0;

                if ("block".equals(phase)) {
                    List<BlockRecord> records = dataService.queryBlockRecordsPage(query, cutoff, cursorState, blocksPerTick);
                    for (BlockRecord record : records) {
                        LocationBlockState target = recordToLocationBlockState(record);
                        dataService.applyBlockRestore(target, rollbackId);
                        processedCount++;
                        processedThisTick++;
                        if (processedCount % checkpointInterval == 0) {
                            dataService.saveCheckpoint(ticket, rollbackId, processedCount, Math.max(processedCount, 1), "running");
                        }
                        updateProgress(ticket, processedCount, processedCount + containerReversals.size() + inventoryReversals.size());
                    }
                    if (records.size() < blocksPerTick) {
                        phase = "container";
                    }
                }

                if ("container".equals(phase)) {
                    while (containerIndex < containerReversals.size() && processedThisTick < blocksPerTick) {
                        dataService.applyContainerReversal(containerReversals.get(containerIndex), rollbackId);
                        containerIndex++;
                        processedCount++;
                        processedThisTick++;
                        if (processedCount % checkpointInterval == 0) {
                            dataService.saveCheckpoint(ticket, rollbackId, processedCount, Math.max(processedCount, 1), "running");
                        }
                        updateProgress(ticket, processedCount, processedCount + inventoryReversals.size());
                    }
                    if (containerIndex >= containerReversals.size()) {
                        phase = "inventory";
                    }
                }

                if ("inventory".equals(phase)) {
                    while (inventoryIndex < inventoryReversals.size() && processedThisTick < blocksPerTick) {
                        dataService.applyInventoryReversal(inventoryReversals.get(inventoryIndex), rollbackId);
                        inventoryIndex++;
                        processedCount++;
                        processedThisTick++;
                        if (processedCount % checkpointInterval == 0) {
                            dataService.saveCheckpoint(ticket, rollbackId, processedCount, Math.max(processedCount, 1), "running");
                        }
                        updateProgress(ticket, processedCount, processedCount);
                    }
                    if (inventoryIndex >= inventoryReversals.size()) {
                        dataService.saveCheckpoint(ticket, rollbackId, processedCount, Math.max(processedCount, 1), "completed");
                        dataService.recordRollbackMeta(ticket, rollbackId, pending);
                        progressMap.put(ticket, 100);
                        rollbackSemaphore.release();
                        cancellationTokens.remove(ticket);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ========== Cancel / Progress ==========

    public void cancelRollback(UUID ticket) {
        CancellationToken token = cancellationTokens.remove(ticket);
        if (token != null) {
            token.cancel();
        }
        pendingMap.remove(ticket);
        progressMap.remove(ticket);
        rollbackSemaphore.release();
    }

    public int getProgress(UUID ticket) {
        Integer progress = progressMap.get(ticket);
        return progress != null ? progress : -1;
    }

    // ========== Restore (Undo Rollback) ==========

    public void restoreRollback(int checkpointId, CommandSender sender) {
        List<BlockRecord> blockRecords = dataService.queryRecordsByRollbackId(checkpointId);
        List<ContainerRecord> containerRecords = dataService.queryContainerRecordsByRollbackId(checkpointId);
        List<ContainerRecord> inventoryRecords = dataService.queryInventoryRecordsByRollbackId(checkpointId);

        if (blockRecords.isEmpty() && containerRecords.isEmpty() && inventoryRecords.isEmpty()) {
            if (sender != null) {
                sender.sendMessage("§cNo rollback data found for checkpoint " + checkpointId);
            }
            return;
        }

        int restoreRollbackId = dataService.nextRollbackId();

        new BukkitRunnable() {
            int blockIndex = 0;
            int containerIndex = 0;
            int inventoryIndex = 0;
            String phase = "block";

            @Override
            public void run() {
                int blocksPerTick = plugin.getConfigManager().getBlocksPerTick();
                int processedThisTick = 0;

                if ("block".equals(phase)) {
                    while (blockIndex < blockRecords.size() && processedThisTick < blocksPerTick) {
                        BlockRecord record = blockRecords.get(blockIndex);
                        LocationBlockState target = new LocationBlockState(
                                record.world, record.x, record.y, record.z,
                                dataService.decompressBlockData(record.oldBlockData),
                                record.oldBlockType);
                        dataService.applyBlockRestore(target, restoreRollbackId);
                        blockIndex++;
                        processedThisTick++;
                    }
                    if (blockIndex >= blockRecords.size()) {
                        phase = "container";
                    }
                }

                if ("container".equals(phase)) {
                    while (containerIndex < containerRecords.size() && processedThisTick < blocksPerTick) {
                        ContainerRecord cr = containerRecords.get(containerIndex);
                        String reversedAction = cr.action.equalsIgnoreCase("deposit") ? "withdraw" : "deposit";
                        ContainerReversal reversal = new ContainerReversal(cr.world, cr.x, cr.y, cr.z,
                                reversedAction, cr.itemType, cr.itemAmount, cr.itemData, cr.playerUuid, cr.playerName);
                        dataService.applyContainerReversal(reversal, restoreRollbackId);
                        containerIndex++;
                        processedThisTick++;
                    }
                    if (containerIndex >= containerRecords.size()) {
                        phase = "inventory";
                    }
                }

                if ("inventory".equals(phase)) {
                    while (inventoryIndex < inventoryRecords.size() && processedThisTick < blocksPerTick) {
                        ContainerRecord ir = inventoryRecords.get(inventoryIndex);
                        String reversedAction = ir.action.equalsIgnoreCase("drop") ? "pickup" : "drop";
                        InventoryReversal reversal = new InventoryReversal(ir.playerUuid, ir.playerName,
                                reversedAction, ir.itemType, ir.itemAmount, ir.itemData);
                        dataService.applyInventoryReversal(reversal, restoreRollbackId);
                        inventoryIndex++;
                        processedThisTick++;
                    }
                    if (inventoryIndex >= inventoryRecords.size()) {
                        plugin.getServer().getLogger().info("Rollback restore completed for checkpoint " + checkpointId);
                        if (sender != null) {
                            sender.sendMessage("§aRollback restore completed for checkpoint " + checkpointId);
                        }
                        cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ========== Helpers ==========

    private void aggregateLocationTargets(List<BlockRecord> records, Map<String, TargetState> accumulator) {
        for (BlockRecord record : records) {
            String key = LocationSerializer.toString(record.world, record.x, record.y, record.z);
            if (!accumulator.containsKey(key)) {
                accumulator.put(key, new TargetState(record.oldBlockType, record.oldBlockData));
            }
        }
    }

    private LocationBlockState recordToLocationBlockState(BlockRecord record) {
        String blockDataStr = dataService.decompressBlockData(record.oldBlockData);
        return new LocationBlockState(record.world, record.x, record.y, record.z, blockDataStr, record.oldBlockType);
    }

    private void updateProgress(UUID ticket, int processed, int total) {
        int pct = (int) ((double) processed / total * 100);
        if (pct > 100) pct = 100;
        progressMap.put(ticket, pct);
    }
}
