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

    /**
     * Calculate Euclidean distance between two 3D points.
     *
     * @param x1 X coordinate of first point
     * @param y1 Y coordinate of first point
     * @param z1 Z coordinate of first point
     * @param x2 X coordinate of second point
     * @param y2 Y coordinate of second point
     * @param z2 Z coordinate of second point
     * @return 3D distance
     */
    public double distance3d(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Check if a point is inside a sphere.
     *
     * @param x       Point X coordinate
     * @param y       Point Y coordinate
     * @param z       Point Z coordinate
     * @param centerX Sphere center X
     * @param centerY Sphere center Y
     * @param centerZ Sphere center Z
     * @param radius  Sphere radius
     * @return true if point is inside sphere
     */
    public boolean inSphere(double x, double y, double z, double centerX, double centerY, double centerZ,
        double radius) {
        return distance3d(x, y, z, centerX, centerY, centerZ) <= radius;
    }

    /**
     * Check if a point is inside an axis-aligned bounding box.
     *
     * @param x    Point X coordinate
     * @param y    Point Y coordinate
     * @param z    Point Z coordinate
     * @param minX Box minimum X
     * @param minY Box minimum Y
     * @param minZ Box minimum Z
     * @param maxX Box maximum X
     * @param maxY Box maximum Y
     * @param maxZ Box maximum Z
     * @return true if point is inside box
     */
    public boolean inBox(double x, double y, double z, double minX, double minY, double minZ, double maxX, double maxY,
        double maxZ) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * Rotate a 2D point around the origin.
     *
     * @param x     Point X coordinate
     * @param y     Point Y coordinate
     * @param angle Rotation angle in degrees
     * @return Array [newX, newY]
     */
    public double[] rotate2D(double x, double y, double angle) {
        double radians = Math.toRadians(angle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double newX = x * cos - y * sin;
        double newY = x * sin + y * cos;
        return new double[] { newX, newY };
    }

    /**
     * Modulo operation with proper handling of negative numbers.
     * Unlike Java's % operator, this always returns positive results.
     *
     * @param a Dividend
     * @param b Divisor
     * @return a mod b (always positive if b > 0)
     */
    public double mod(double a, double b) {
        return ((a % b) + b) % b;
    }

    /**
     * Sign function.
     *
     * @param value Input value
     * @return 1 if positive, -1 if negative, 0 if zero
     */
    public double sign(double value) {
        if (value > 0) return 1.0;
        if (value < 0) return -1.0;
        return 0.0;
    }

    /**
     * Smooth Hermite interpolation (smoothstep).
     * Returns smooth transition from 0 to 1 as x goes from edge0 to edge1.
     *
     * @param edge0 Lower edge
     * @param edge1 Upper edge
     * @param x     Input value
     * @return Smoothstep value (0-1)
     */
    public double smoothstep(double edge0, double edge1, double x) {
        // Clamp x to [0, 1]
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        // Hermite interpolation
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Rotate coordinates based on the clicked block face.
     * This transforms relative coordinates (relX, relY, relZ) so that 2D patterns
     * can be oriented correctly on any surface.
     * 
     * After transformation:
     * - u: horizontal axis (left-right when facing the surface)
     * - v: vertical axis (up-down when facing the surface)
     * - w: depth axis (perpendicular to surface, positive = away from surface)
     *
     * Face values: 0=DOWN, 1=UP, 2=NORTH, 3=SOUTH, 4=WEST, 5=EAST
     *
     * @param relX Relative X coordinate
     * @param relY Relative Y coordinate
     * @param relZ Relative Z coordinate
     * @param face Block face that was clicked (0-5)
     * @return Array [u, v, w] - transformed coordinates
     */
    public double[] rotateFace(double relX, double relY, double relZ, int face) {
        double u, v, w;

        switch (face) {
            case 0: // DOWN (bottom face)
                // Looking down at XZ plane
                u = relX;
                v = -relZ; // Flip Z so forward is positive
                w = -relY; // Away from surface is up
                break;

            case 1: // UP (top face)
                // Looking up at XZ plane
                u = relX;
                v = relZ;
                w = relY; // Away from surface is up
                break;

            case 2: // NORTH (negative Z)
                // Looking north along XY plane
                u = -relX; // Flip X for consistent right-hand feel
                v = relY;
                w = -relZ; // Away from surface is toward negative Z
                break;

            case 3: // SOUTH (positive Z)
                // Looking south along XY plane
                u = relX;
                v = relY;
                w = relZ; // Away from surface is toward positive Z
                break;

            case 4: // WEST (negative X)
                // Looking west along ZY plane
                u = relZ;
                v = relY;
                w = -relX; // Away from surface is toward negative X
                break;

            case 5: // EAST (positive X)
                // Looking east along ZY plane
                u = -relZ; // Flip Z for consistent right-hand feel
                v = relY;
                w = relX; // Away from surface is toward positive X
                break;

            default:
                // Invalid face, return unchanged
                u = relX;
                v = relY;
                w = relZ;
                break;
        }

        return new double[] { u, v, w };
    }
}
