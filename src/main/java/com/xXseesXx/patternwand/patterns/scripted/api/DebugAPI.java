package com.xXseesXx.patternwand.patterns.scripted.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

/**
 * Debug API for pattern scripts.
 * Allows scripts to output debug messages during development.
 * 
 * Enhanced with phase-based timing for batched execution:
 * - Phase 1: Plan Generation (Lua execution)
 * - Phase 2: Material Aggregation
 * - Phase 3: Material Validation
 * - Phase 4: Material Consumption
 * - Phase 5: Block Placement
 */
public class DebugAPI {

    private static boolean debugEnabled = false;
    private static final List<String> debugMessages = new ArrayList<String>();

    // Phase timing fields
    private static EntityPlayer currentPlayer = null;
    private static long operationStartTimeNs = 0;
    private static long phase1StartNs = 0; // Plan generation
    private static long phase2StartNs = 0; // Aggregation
    private static long phase3StartNs = 0; // Validation
    private static long phase4StartNs = 0; // Consumption
    private static long phase5StartNs = 0; // Placement

    private static long phase1DurationNs = 0;
    private static long phase2DurationNs = 0;
    private static long phase3DurationNs = 0;
    private static long phase4DurationNs = 0;
    private static long phase5DurationNs = 0;

    // Pattern execution tracking (within Phase 1)
    private static long totalLuaExecutionNs = 0;
    private static int luaCallCount = 0;

    // Placement tracking (within Phase 5)
    private static int blocksPlaced = 0;
    private static int plannedBlocks = 0;

    /**
     * Enable or disable debug mode.
     *
     * @param enabled true to enable debug messages
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        if (!enabled) {
            debugMessages.clear();
            resetTiming();
        }
    }

    /**
     * Check if debug mode is enabled.
     *
     * @return true if debug is enabled
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Start timing a complete pattern operation.
     * Should be called at the very start of placeBlocksWithPattern().
     * 
     * @param player The player executing the pattern (for sending messages)
     */
    public static void startPatternTiming(EntityPlayer player) {
        if (debugEnabled) {
            currentPlayer = player;
            operationStartTimeNs = System.nanoTime();
            resetTiming();
        }
    }

    /**
     * Start timing a complete pattern operation (backward compatibility).
     * Should be called at the very start of placeBlocksWithPattern().
     */
    public static void startPatternTiming() {
        startPatternTiming(null);
    }

    /**
     * Mark the start of Phase 1: Plan Generation (Lua execution).
     */
    public static void startPhase1() {
        if (debugEnabled) {
            phase1StartNs = System.nanoTime();
        }
    }

    /**
     * Mark the end of Phase 1 and record statistics.
     * 
     * @param plannedBlockCount Number of blocks in the generated plan
     */
    public static void endPhase1(int plannedBlockCount) {
        if (debugEnabled) {
            phase1DurationNs = System.nanoTime() - phase1StartNs;
            plannedBlocks = plannedBlockCount;
        }
    }

    /**
     * Mark the start of Phase 2: Material Aggregation.
     */
    public static void startPhase2() {
        if (debugEnabled) {
            phase2StartNs = System.nanoTime();
        }
    }

    /**
     * Mark the end of Phase 2.
     */
    public static void endPhase2() {
        if (debugEnabled) {
            phase2DurationNs = System.nanoTime() - phase2StartNs;
        }
    }

    /**
     * Mark the start of Phase 3: Material Validation.
     */
    public static void startPhase3() {
        if (debugEnabled) {
            phase3StartNs = System.nanoTime();
        }
    }

    /**
     * Mark the end of Phase 3.
     */
    public static void endPhase3() {
        if (debugEnabled) {
            phase3DurationNs = System.nanoTime() - phase3StartNs;
        }
    }

    /**
     * Mark the start of Phase 4: Material Consumption.
     */
    public static void startPhase4() {
        if (debugEnabled) {
            phase4StartNs = System.nanoTime();
        }
    }

    /**
     * Mark the end of Phase 4.
     */
    public static void endPhase4() {
        if (debugEnabled) {
            phase4DurationNs = System.nanoTime() - phase4StartNs;
        }
    }

    /**
     * Mark the start of Phase 5: Block Placement.
     */
    public static void startPhase5() {
        if (debugEnabled) {
            phase5StartNs = System.nanoTime();
        }
    }

    /**
     * Mark the end of Phase 5.
     * 
     * @param placedBlockCount Number of blocks successfully placed
     */
    public static void endPhase5(int placedBlockCount) {
        if (debugEnabled) {
            phase5DurationNs = System.nanoTime() - phase5StartNs;
            blocksPlaced = placedBlockCount;
        }
    }

    /**
     * Record the execution time for a single Lua pattern call.
     * Called from ScriptEngine during Phase 1.
     *
     * @param executionTimeNs Time taken to execute pattern for one position (in nanoseconds)
     */
    public static void recordBlockExecution(long executionTimeNs) {
        if (debugEnabled) {
            totalLuaExecutionNs += executionTimeNs;
            luaCallCount++;
        }
    }

    /**
     * Finish pattern timing and send detailed summary to player.
     * Should be called after pattern placement is complete.
     */
    public static void finishPatternTiming() {
        if (!debugEnabled) {
            return;
        }

        long totalTimeNs = System.nanoTime() - operationStartTimeNs;
        double totalMs = totalTimeNs / 1_000_000.0;

        // Calculate phase times
        double phase1Ms = phase1DurationNs / 1_000_000.0;
        double phase2Ms = phase2DurationNs / 1_000_000.0;
        double phase3Ms = phase3DurationNs / 1_000_000.0;
        double phase4Ms = phase4DurationNs / 1_000_000.0;
        double phase5Ms = phase5DurationNs / 1_000_000.0;

        double luaMs = totalLuaExecutionNs / 1_000_000.0;
        double nsPerLuaCall = luaCallCount > 0 ? (double) totalLuaExecutionNs / luaCallCount : 0;

        // Send summary to player and log
        String header = String.format("§e=== Pattern Execution Timing ===");
        String total = String
            .format("§aTotal: %.2f ms (%d blocks placed, %d planned)", totalMs, blocksPlaced, plannedBlocks);
        String phase1 = String
            .format("§7Phase 1 (Plan Generation): %.2f ms (%.1f%%)", phase1Ms, (phase1Ms / totalMs) * 100);
        String luaDetail = String
            .format("§7  - Lua execution: %.2f ms (%d calls, %.1f ns/call)", luaMs, luaCallCount, nsPerLuaCall);
        String phase2 = String
            .format("§7Phase 2 (Aggregation): %.2f ms (%.1f%%)", phase2Ms, (phase2Ms / totalMs) * 100);
        String phase3 = String.format("§7Phase 3 (Validation): %.2f ms (%.1f%%)", phase3Ms, (phase3Ms / totalMs) * 100);
        String phase4 = String
            .format("§7Phase 4 (Consumption): %.2f ms (%.1f%%)", phase4Ms, (phase4Ms / totalMs) * 100);
        String phase5 = String.format("§7Phase 5 (Placement): %.2f ms (%.1f%%)", phase5Ms, (phase5Ms / totalMs) * 100);

        // Send to player if available
        if (currentPlayer != null) {
            currentPlayer.addChatMessage(new ChatComponentText(header));
            currentPlayer.addChatMessage(new ChatComponentText(total));
            currentPlayer.addChatMessage(new ChatComponentText(phase1));
            currentPlayer.addChatMessage(new ChatComponentText(luaDetail));
            currentPlayer.addChatMessage(new ChatComponentText(phase2));
            currentPlayer.addChatMessage(new ChatComponentText(phase3));
            currentPlayer.addChatMessage(new ChatComponentText(phase4));
            currentPlayer.addChatMessage(new ChatComponentText(phase5));
        }

        // Also log to console
        System.out.println("[PatternWand Debug] " + header);
        System.out.println("[PatternWand Debug] " + total);
        System.out.println("[PatternWand Debug] " + phase1);
        System.out.println("[PatternWand Debug] " + luaDetail);
        System.out.println("[PatternWand Debug] " + phase2);
        System.out.println("[PatternWand Debug] " + phase3);
        System.out.println("[PatternWand Debug] " + phase4);
        System.out.println("[PatternWand Debug] " + phase5);

        // Store in messages
        debugMessages.add(total);
    }

    /**
     * Reset timing statistics.
     */
    public static void resetTiming() {
        operationStartTimeNs = 0;
        phase1StartNs = 0;
        phase2StartNs = 0;
        phase3StartNs = 0;
        phase4StartNs = 0;
        phase5StartNs = 0;
        phase1DurationNs = 0;
        phase2DurationNs = 0;
        phase3DurationNs = 0;
        phase4DurationNs = 0;
        phase5DurationNs = 0;
        totalLuaExecutionNs = 0;
        luaCallCount = 0;
        blocksPlaced = 0;
        plannedBlocks = 0;
        currentPlayer = null;
    }

    /**
     * Get all debug messages.
     *
     * @return List of debug messages
     */
    public static List<String> getMessages() {
        return new ArrayList<String>(debugMessages);
    }

    /**
     * Clear all debug messages.
     */
    public static void clearMessages() {
        debugMessages.clear();
    }

    /**
     * Print a debug message (if debug mode is enabled).
     *
     * @param message Message to print
     */
    public void print(String message) {
        if (debugEnabled) {
            debugMessages.add(message);
            // Also log to console for immediate feedback
            System.out.println("[PatternWand Debug] " + message);

            // Send to current player if available
            if (currentPlayer != null) {
                currentPlayer.addChatMessage(new ChatComponentText("§7[Debug] " + message));
            }
        }
    }

    /**
     * Print a debug message with multiple values.
     *
     * @param values Values to print (will be concatenated)
     */
    public void print(Object... values) {
        if (debugEnabled) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(" ");
                sb.append(values[i]);
            }
            print(sb.toString());
        }
    }
}
