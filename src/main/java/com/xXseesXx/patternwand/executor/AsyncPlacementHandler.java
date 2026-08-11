package com.xXseesXx.patternwand.executor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import net.minecraft.entity.player.EntityPlayer;

import com.xXseesXx.patternwand.PatternWandMod;
import com.xXseesXx.patternwand.patterns.PlacementPlan;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Handles asynchronous pattern placement on the main thread.
 * 
 * <p>
 * Tracks pending Lua execution jobs and applies completed plans to the world
 * during server ticks. This ensures all world modification happens on the main
 * thread while Lua computation runs in the background.
 * 
 * <p>
 * <b>Architecture:</b>
 * <ol>
 * <li>Player uses wand → Job submitted to LuaExecutorService</li>
 * <li>Job tracked here as PendingJob with Future&lt;PlacementPlan&gt;</li>
 * <li>Each server tick: Check if any jobs completed</li>
 * <li>If complete: Apply plan to world, notify player, cleanup</li>
 * </ol>
 * 
 * <p>
 * <b>Thread Safety:</b>
 * <ul>
 * <li>This handler runs ONLY on main thread (TickEvent.ServerTickEvent)</li>
 * <li>Future.isDone() is thread-safe</li>
 * <li>Future.get() retrieves result from completed background computation</li>
 * <li>All world/player interaction happens on main thread</li>
 * </ul>
 * 
 * <p>
 * <b>Job Lifecycle:</b>
 * 
 * <pre>
 * submit() → track() → poll() → apply() → cleanup()
 * </pre>
 */
public class AsyncPlacementHandler {

    /**
     * Represents a pending pattern execution job.
     */
    public static class PendingJob {

        public final UUID playerUUID;
        public final Future<PlacementPlan> future;
        public final long submitTimeMs;
        public final String patternName;
        public final int dimension;
        public final String worldName;

        public PendingJob(UUID playerUUID, Future<PlacementPlan> future, String patternName, int dimension,
            String worldName) {
            this.playerUUID = playerUUID;
            this.future = future;
            this.patternName = patternName;
            this.dimension = dimension;
            this.worldName = worldName;
            this.submitTimeMs = System.currentTimeMillis();
        }

        /**
         * Check if this job has been pending for too long.
         * 
         * @param timeoutMs Maximum time to wait for job completion
         * @return true if job exceeded timeout
         */
        public boolean isTimedOut(long timeoutMs) {
            return (System.currentTimeMillis() - submitTimeMs) > timeoutMs;
        }
    }

    /**
     * Maximum time to wait for job completion before considering it stale (30 seconds).
     * 
     * Patterns should complete much faster than this. If a job times out, it likely
     * indicates an infinite loop or other bug in the pattern script.
     */
    private static final long JOB_TIMEOUT_MS = 30_000;

    /**
     * Map of player UUID → pending job.
     * 
     * Each player can have at most one pending job. If they use the wand again
     * before the first job completes, the old job is cancelled.
     */
    private final Map<UUID, PendingJob> pendingJobs = new HashMap<UUID, PendingJob>();

    /**
     * Track a pending pattern execution job.
     * 
     * <p>
     * If the player already has a pending job, the old job is cancelled
     * and replaced with the new one.
     * 
     * @param playerUUID  Player who initiated the job
     * @param future      Future containing the PlacementPlan result
     * @param patternName Name of pattern being executed (for logging)
     * @param dimension   Dimension ID where job was submitted
     * @param worldName   World name for validation
     */
    public void trackJob(UUID playerUUID, Future<PlacementPlan> future, String patternName, int dimension,
        String worldName) {
        if (playerUUID == null || future == null) {
            PatternWandMod.LOG.warn("Cannot track job with null player UUID or future");
            return;
        }

        // Cancel existing job if present
        PendingJob existingJob = pendingJobs.get(playerUUID);
        if (existingJob != null) {
            PatternWandMod.LOG.debug(
                String
                    .format("Player %s has pending job for '%s', cancelling it", playerUUID, existingJob.patternName));
            existingJob.future.cancel(true);
        }

        // Track new job
        PendingJob job = new PendingJob(playerUUID, future, patternName, dimension, worldName);
        pendingJobs.put(playerUUID, job);

        PatternWandMod.LOG.debug(
            String.format(
                "Tracking async job for player %s: pattern '%s' in dimension %d (%d total pending)",
                playerUUID,
                patternName,
                dimension,
                pendingJobs.size()));
    }

    /**
     * Cancel a player's pending job.
     * 
     * @param playerUUID Player whose job to cancel
     * @return true if a job was cancelled, false if no job was pending
     */
    public boolean cancelJob(UUID playerUUID) {
        PendingJob job = pendingJobs.remove(playerUUID);
        if (job != null) {
            job.future.cancel(true);
            PatternWandMod.LOG
                .debug(String.format("Cancelled pending job for player %s: pattern '%s'", playerUUID, job.patternName));
            return true;
        }
        return false;
    }

    /**
     * Get the number of currently pending jobs.
     */
    public int getPendingJobCount() {
        return pendingJobs.size();
    }

    /**
     * Check if a player has a pending job.
     */
    public boolean hasJob(UUID playerUUID) {
        return pendingJobs.containsKey(playerUUID);
    }

    /**
     * Server tick event handler - polls pending jobs and applies completed plans.
     * 
     * <p>
     * This method runs on the main thread during every server tick. It:
     * <ol>
     * <li>Iterates through all pending jobs</li>
     * <li>Checks if any have completed (Future.isDone())</li>
     * <li>Retrieves completed plans and applies them to the world</li>
     * <li>Removes completed/failed/timed-out jobs</li>
     * </ol>
     * 
     * <p>
     * Uses {@link TickEvent.Phase#END} to ensure all other game logic has
     * completed before modifying the world.
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // Only process at end of tick, after all other game logic
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Nothing to do if no pending jobs
        if (pendingJobs.isEmpty()) {
            return;
        }

        // Iterate and check for completed jobs
        Iterator<Map.Entry<UUID, PendingJob>> iterator = pendingJobs.entrySet()
            .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingJob> entry = iterator.next();
            PendingJob job = entry.getValue();

            try {
                // Check if job completed
                if (job.future.isDone()) {
                    handleCompletedJob(job);
                    iterator.remove();
                }
                // Check if job timed out
                else if (job.isTimedOut(JOB_TIMEOUT_MS)) {
                    handleTimedOutJob(job);
                    job.future.cancel(true);
                    iterator.remove();
                }
            } catch (Exception e) {
                PatternWandMod.LOG.error(
                    String.format(
                        "Error handling pending job for player %s: pattern '%s'",
                        job.playerUUID,
                        job.patternName),
                    e);
                iterator.remove();
            }
        }
    }

    /**
     * Handle a completed job - retrieve plan and apply to world.
     * 
     * @param job The completed job
     */
    private void handleCompletedJob(PendingJob job) {
        try {
            // Retrieve plan from future (should not block since isDone() = true)
            PlacementPlan plan = job.future.get();

            long elapsedMs = System.currentTimeMillis() - job.submitTimeMs;

            PatternWandMod.LOG.debug(
                String.format(
                    "Pattern '%s' for player %s completed in %dms (%d placements)",
                    job.patternName,
                    job.playerUUID,
                    elapsedMs,
                    plan.size()));

            // Validate job before applying to world
            if (!validateJob(job, plan)) {
                PatternWandMod.LOG
                    .debug(String.format("Job for player %s failed validation, skipping placement", job.playerUUID));
                return;
            }

            // Apply plan to world (on main thread)
            applyPlan(job, plan);

        } catch (java.util.concurrent.CancellationException e) {
            PatternWandMod.LOG
                .debug(String.format("Job cancelled for player %s: pattern '%s'", job.playerUUID, job.patternName));
        } catch (Exception e) {
            PatternWandMod.LOG.error(
                String.format("Failed to retrieve plan for player %s: pattern '%s'", job.playerUUID, job.patternName),
                e);
        }
    }

    /**
     * Validate that a job is still valid before applying its plan.
     */
    private boolean validateJob(PendingJob job, PlacementPlan plan) {
        EntityPlayer player = findPlayerByUUID(job.playerUUID);
        if (player == null) {
            PatternWandMod.LOG.debug(String.format("Player %s not found, job is stale", job.playerUUID));
            return false;
        }

        if (!player.isEntityAlive() || player.isDead) {
            PatternWandMod.LOG
                .debug(String.format("Player %s is dead, skipping placement", player.getCommandSenderName()));
            return false;
        }

        if (player.dimension != job.dimension) {
            PatternWandMod.LOG.debug(
                String.format(
                    "Player %s changed dimensions (was %d, now %d), skipping placement",
                    player.getCommandSenderName(),
                    job.dimension,
                    player.dimension));
            return false;
        }

        if (plan.isEmpty()) {
            PatternWandMod.LOG
                .debug(String.format("Plan for player %s is empty, skipping", player.getCommandSenderName()));
            return false;
        }

        return true;
    }

    /**
     * Apply a validated placement plan to the world.
     */
    private void applyPlan(PendingJob job, PlacementPlan plan) {
        EntityPlayer player = findPlayerByUUID(job.playerUUID);
        if (player == null) return;

        // TODO Milestone 9: Full integration with PatternWandWorker
        PatternWandMod.LOG.info(
            String.format(
                "Would apply plan for player %s: %d blocks from pattern '%s'",
                player.getCommandSenderName(),
                plan.size(),
                job.patternName));
    }

    /**
     * Find a player by UUID across all worlds.
     */
    private EntityPlayer findPlayerByUUID(UUID uuid) {
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (server == null) return null;
        return server.getConfigurationManager()
            .func_152612_a(uuid.toString());
    }

    /**
     * Handle a timed-out job - log warning and cleanup.
     * 
     * @param job The timed-out job
     */
    private void handleTimedOutJob(PendingJob job) {
        long elapsedMs = System.currentTimeMillis() - job.submitTimeMs;

        PatternWandMod.LOG.warn(
            String.format(
                "Pattern '%s' for player %s timed out after %dms (timeout: %dms) - possible infinite loop?",
                job.patternName,
                job.playerUUID,
                elapsedMs,
                JOB_TIMEOUT_MS));

        // TODO: Notify player of timeout
    }

    /**
     * Cancel all pending jobs (called on server shutdown).
     */
    public void cancelAllJobs() {
        PatternWandMod.LOG.info(String.format("Cancelling %d pending async jobs", pendingJobs.size()));

        for (PendingJob job : pendingJobs.values()) {
            job.future.cancel(true);
        }

        pendingJobs.clear();
    }
}
