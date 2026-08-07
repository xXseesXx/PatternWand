package com.xXseesXx.patternwand.patterns.scripted.api;

/**
 * Utility functions exposed to Lua scripts.
 * Provides helper functions for math, distance, and mapping.
 */
public class UtilAPI {

    /**
     * Deterministic hash function for 2D coordinates.
     * Useful for pseudorandom patterns without noise.
     *
     * @param x X coordinate
     * @param z Z coordinate
     * @return Hash value (integer)
     */
    public int hash(double x, double z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        int h = ix * 374761393 + iz * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return h ^ (h >> 16);
    }

    /**
     * Deterministic hash function for 3D coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Hash value (integer)
     */
    public int hash3d(double x, double y, double z) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iz = (int) Math.floor(z);
        int h = ix * 374761393 + iy * 668265263 + iz * 1771875;
        h = (h ^ (h >> 13)) * 1274126177;
        return h ^ (h >> 16);
    }

    /**
     * Calculate Euclidean distance between two 2D points.
     *
     * @param x1 X coordinate of first point
     * @param y1 Y coordinate of first point
     * @param x2 X coordinate of second point
     * @param y2 Y coordinate of second point
     * @return Distance
     */
    public double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculate Manhattan (taxicab) distance between two 2D points.
     * Sum of absolute differences in coordinates.
     *
     * @param x1 X coordinate of first point
     * @param y1 Y coordinate of first point
     * @param x2 X coordinate of second point
     * @param y2 Y coordinate of second point
     * @return Manhattan distance
     */
    public double manhattan(double x1, double y1, double x2, double y2) {
        return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }

    /**
     * Map a value from one range to another.
     * Useful for converting noise values to palette indices.
     *
     * Example: map(0.5, 0, 1, 0, 10) returns 5
     *
     * @param value  Input value
     * @param inMin  Input range minimum
     * @param inMax  Input range maximum
     * @param outMin Output range minimum
     * @param outMax Output range maximum
     * @return Mapped value
     */
    public double map(double value, double inMin, double inMax, double outMin, double outMax) {
        return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
    }

    /**
     * Clamp a value to a range [min, max].
     *
     * @param value Value to clamp
     * @param min   Minimum value
     * @param max   Maximum value
     * @return Clamped value
     */
    public double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Linear interpolation between two values.
     *
     * @param a Start value
     * @param b End value
     * @param t Interpolation factor (0-1)
     * @return Interpolated value
     */
    public double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    /**
     * Round down to nearest integer.
     * Same as math.floor but exposed for convenience.
     *
     * @param value Value to floor
     * @return Floored value
     */
    public int floor(double value) {
        return (int) Math.floor(value);
    }

    /**
     * Round up to nearest integer.
     * Same as math.ceil but exposed for convenience.
     *
     * @param value Value to ceil
     * @return Ceiled value
     */
    public int ceil(double value) {
        return (int) Math.ceil(value);
    }

    /**
     * Absolute value.
     *
     * @param value Input value
     * @return Absolute value
     */
    public double abs(double value) {
        return Math.abs(value);
    }
}
