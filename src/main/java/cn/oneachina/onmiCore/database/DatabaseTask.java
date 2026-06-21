package cn.oneachina.onmiCore.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DatabaseTask {

    private static final int BATCH_SIZE = 50;
    private static final int FLUSH_INTERVAL_MS = 1000;
    private static final int MAX_QUEUE_SIZE = 10000;

    private final JavaPlugin plugin;
    private final DatabaseManager db;
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    public DatabaseTask(JavaPlugin plugin) {
        this.plugin = plugin;
        this.db = JavaPlugin.getPlugin(cn.oneachina.onmiCore.OnmiCore.class).getDatabaseManager();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = new Thread(this::processQueue, "OnmiCore-DB-Writer");
            worker.setDaemon(true);
            worker.start();
        }
    }

    public void shutdown() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        flush();
    }

    public void submit(Runnable task) {
        if (!queue.offer(task)) {
            plugin.getSLF4JLogger().warn("Database queue is full ({}), dropping write task", MAX_QUEUE_SIZE);
        }
    }

    private void processQueue() {
        List<Runnable> batch = new ArrayList<>(BATCH_SIZE);
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Runnable task = queue.poll(FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
                if (task != null) {
                    batch.add(task);
                    queue.drainTo(batch, BATCH_SIZE - 1);
                }
                if (!batch.isEmpty() && (batch.size() >= BATCH_SIZE || task == null)) {
                    executeBatch(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Database write error", e);
            }
        }
        // Flush remaining
        List<Runnable> remaining = new ArrayList<>();
        queue.drainTo(remaining);
        if (!remaining.isEmpty()) {
            executeBatch(remaining);
        }
    }

    private void executeBatch(List<Runnable> tasks) {
        for (Runnable task : tasks) {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Task execution error", e);
            }
        }
    }

    private void flush() {
        List<Runnable> remaining = new ArrayList<>();
        queue.drainTo(remaining);
        executeBatch(remaining);
    }

    // Convenience: insert a block record
    public void insertBlockRecord(String world, int x, int y, int z,
                                  String playerUuid, String playerName,
                                  String action,
                                  String oldBlockType, String newBlockType,
                                  byte[] oldBlockData, byte[] newBlockData) {
        submit(() -> {
            String sql = "INSERT INTO block_records (world, x, y, z, player_uuid, player_name, "
                    + "action, old_block_type, new_block_type, old_block_data, new_block_data, timestamp) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, world);
                ps.setInt(2, x);
                ps.setInt(3, y);
                ps.setInt(4, z);
                ps.setString(5, playerUuid);
                ps.setString(6, playerName);
                ps.setString(7, action);
                ps.setString(8, oldBlockType);
                ps.setString(9, newBlockType);
                if (oldBlockData != null) {
                    ps.setBytes(10, oldBlockData);
                } else {
                    ps.setNull(10, java.sql.Types.BLOB);
                }
                if (newBlockData != null) {
                    ps.setBytes(11, newBlockData);
                } else {
                    ps.setNull(11, java.sql.Types.BLOB);
                }
                ps.setTimestamp(12, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Failed to insert block record", e);
            }
        });
    }

    // Convenience: insert a container record
    public void insertContainerRecord(String world, int x, int y, int z,
                                      String playerUuid, String playerName,
                                      String action,
                                      String itemType, int itemAmount,
                                      byte[] itemData) {
        submit(() -> {
            String sql = "INSERT INTO container_records (world, x, y, z, player_uuid, player_name, "
                    + "action, item_type, item_amount, item_data, timestamp) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, world);
                ps.setInt(2, x);
                ps.setInt(3, y);
                ps.setInt(4, z);
                ps.setString(5, playerUuid);
                ps.setString(6, playerName);
                ps.setString(7, action);
                ps.setString(8, itemType);
                ps.setInt(9, itemAmount);
                if (itemData != null) {
                    ps.setBytes(10, itemData);
                } else {
                    ps.setNull(10, java.sql.Types.BLOB);
                }
                ps.setTimestamp(11, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Failed to insert container record", e);
            }
        });
    }

    // Convenience: insert an inventory record
    public void insertInventoryRecord(String playerUuid, String playerName,
                                      String action,
                                      String itemType, int itemAmount,
                                      byte[] itemData) {
        submit(() -> {
            String sql = "INSERT INTO inventory_records (player_uuid, player_name, "
                    + "action, item_type, item_amount, item_data, timestamp) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid);
                ps.setString(2, playerName);
                ps.setString(3, action);
                ps.setString(4, itemType);
                ps.setInt(5, itemAmount);
                if (itemData != null) {
                    ps.setBytes(6, itemData);
                } else {
                    ps.setNull(6, java.sql.Types.BLOB);
                }
                ps.setTimestamp(7, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Failed to insert inventory record", e);
            }
        });
    }
}
