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

    /**
     * Enable or disable debug mode.
     *
     * @param enabled true to enable debug messages
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        if (!enabled) {
            debugMessages.clear();
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
