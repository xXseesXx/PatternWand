package com.xXseesXx.patternwand.patterns.scripted.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug API for pattern scripts.
 * Allows scripts to output debug messages during development.
 */
public class DebugAPI {

    private static boolean debugEnabled = false;
    private static final List<String> debugMessages = new ArrayList<String>();

    // Timing tracking fields
    private static long totalExecutionTimeNs = 0;
    private static int blockCount = 0;
    private static long patternStartTimeNs = 0;

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
     * Start timing a pattern execution sequence.
     * Should be called before pattern placement begins.
     */
    public static void startPatternTiming() {
        if (debugEnabled) {
            patternStartTimeNs = System.nanoTime();
            totalExecutionTimeNs = 0;
            blockCount = 0;
        }
    }

    /**
     * Record the execution time for a single block.
     *
     * @param executionTimeNs Time taken to execute pattern for one block (in nanoseconds)
     */
    public static void recordBlockExecution(long executionTimeNs) {
        if (debugEnabled) {
            totalExecutionTimeNs += executionTimeNs;
            blockCount++;
        }
    }

    /**
     * Finish pattern timing and print summary.
     * Should be called after pattern placement is complete.
     */
    public static void finishPatternTiming() {
        if (debugEnabled && blockCount > 0) {
            long totalTimeNs = System.nanoTime() - patternStartTimeNs;
            double totalMs = totalTimeNs / 1_000_000.0;
            double patternMs = totalExecutionTimeNs / 1_000_000.0;
            double nsPerBlock = (double) totalExecutionTimeNs / blockCount;

            String timingMsg = String.format(
                "Pattern execution complete: %d blocks in %.3f ms (%.3f ms pattern time, %.1f ns/block)",
                blockCount,
                totalMs,
                patternMs,
                nsPerBlock);

            debugMessages.add(timingMsg);
            System.out.println("[PatternWand Debug] " + timingMsg);
        }
    }

    /**
     * Reset timing statistics.
     */
    public static void resetTiming() {
        totalExecutionTimeNs = 0;
        blockCount = 0;
        patternStartTimeNs = 0;
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
