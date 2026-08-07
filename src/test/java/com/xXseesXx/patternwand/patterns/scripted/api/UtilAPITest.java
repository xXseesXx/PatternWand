package com.xXseesXx.patternwand.patterns.scripted.api;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for UtilAPI.
 * Tests utility functions like distance, hash, map, clamp, etc.
 */
public class UtilAPITest {

    private UtilAPI util;
    private static final double EPSILON = 0.001;

    @Before
    public void setUp() {
        util = new UtilAPI();
    }

    @Test
    public void testHash2D() {
        // Hash should be deterministic
        int hash1 = util.hash(10, 20);
        int hash2 = util.hash(10, 20);
        assertEquals(hash1, hash2);

        // Different inputs should give different hashes
        int hash3 = util.hash(11, 20);
        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testHash3D() {
        // Hash should be deterministic
        int hash1 = util.hash3d(10, 20, 30);
        int hash2 = util.hash3d(10, 20, 30);
        assertEquals(hash1, hash2);

        // Different inputs should give different hashes
        int hash3 = util.hash3d(10, 20, 31);
        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testHashNegativeCoordinates() {
        // Should handle negative coordinates
        int hash = util.hash(-10, -20);
        assertNotNull(hash);

        int hash3d = util.hash3d(-10, -20, -30);
        assertNotNull(hash3d);
    }

    @Test
    public void testDistance() {
        // Distance from origin
        double dist = util.distance(0, 0, 3, 4);
        assertEquals(5.0, dist, EPSILON); // 3-4-5 triangle

        // Distance between two points
        double dist2 = util.distance(1, 1, 4, 5);
        assertEquals(5.0, dist2, EPSILON);
    }

    @Test
    public void testDistanceZero() {
        // Distance from point to itself
        double dist = util.distance(10, 20, 10, 20);
        assertEquals(0.0, dist, EPSILON);
    }

    @Test
    public void testDistanceNegative() {
        // Distance with negative coordinates
        double dist = util.distance(-3, -4, 0, 0);
        assertEquals(5.0, dist, EPSILON);
    }

    @Test
    public void testManhattan() {
        // Manhattan distance
        double dist = util.manhattan(0, 0, 3, 4);
        assertEquals(7.0, dist, EPSILON); // |3| + |4|

        double dist2 = util.manhattan(1, 1, 4, 5);
        assertEquals(7.0, dist2, EPSILON); // |4-1| + |5-1|
    }

    @Test
    public void testManhattanZero() {
        // Manhattan distance from point to itself
        double dist = util.manhattan(10, 20, 10, 20);
        assertEquals(0.0, dist, EPSILON);
    }

    @Test
    public void testMap() {
        // Map value from one range to another
        double result = util.map(0.5, 0, 1, 0, 10);
        assertEquals(5.0, result, EPSILON);

        double result2 = util.map(0.0, 0, 1, 10, 20);
        assertEquals(10.0, result2, EPSILON);

        double result3 = util.map(1.0, 0, 1, 10, 20);
        assertEquals(20.0, result3, EPSILON);
    }

    @Test
    public void testMapNegativeRanges() {
        // Map with negative ranges
        double result = util.map(0, -1, 1, 0, 10);
        assertEquals(5.0, result, EPSILON);

        double result2 = util.map(0.5, 0, 1, -10, 10);
        assertEquals(0.0, result2, EPSILON);
    }

    @Test
    public void testClamp() {
        // Clamp values within range
        assertEquals(5.0, util.clamp(5.0, 0, 10), EPSILON);
        assertEquals(0.0, util.clamp(-5.0, 0, 10), EPSILON);
        assertEquals(10.0, util.clamp(15.0, 0, 10), EPSILON);
    }

    @Test
    public void testClampAtBoundaries() {
        // Test clamping exactly at boundaries
        assertEquals(0.0, util.clamp(0.0, 0, 10), EPSILON);
        assertEquals(10.0, util.clamp(10.0, 0, 10), EPSILON);
    }

    @Test
    public void testClampNegativeRange() {
        // Clamp in negative range
        assertEquals(-5.0, util.clamp(-5.0, -10, 0), EPSILON);
        assertEquals(-10.0, util.clamp(-15.0, -10, 0), EPSILON);
        assertEquals(0.0, util.clamp(5.0, -10, 0), EPSILON);
    }

    @Test
    public void testLerp() {
        // Linear interpolation
        assertEquals(5.0, util.lerp(0, 10, 0.5), EPSILON);
        assertEquals(0.0, util.lerp(0, 10, 0.0), EPSILON);
        assertEquals(10.0, util.lerp(0, 10, 1.0), EPSILON);
        assertEquals(2.5, util.lerp(0, 10, 0.25), EPSILON);
    }

    @Test
    public void testLerpNegativeValues() {
        // Lerp with negative values
        assertEquals(0.0, util.lerp(-10, 10, 0.5), EPSILON);
        assertEquals(-5.0, util.lerp(-10, 0, 0.5), EPSILON);
    }

    @Test
    public void testFloor() {
        // Floor function
        assertEquals(5, util.floor(5.9));
        assertEquals(5, util.floor(5.1));
        assertEquals(5, util.floor(5.0));
        assertEquals(-6, util.floor(-5.1)); // Floor of negative rounds down
    }

    @Test
    public void testCeil() {
        // Ceil function
        assertEquals(6, util.ceil(5.1));
        assertEquals(6, util.ceil(5.9));
        assertEquals(5, util.ceil(5.0));
        assertEquals(-5, util.ceil(-5.1)); // Ceil of negative rounds up
    }

    @Test
    public void testAbs() {
        // Absolute value
        assertEquals(5.0, util.abs(5.0), EPSILON);
        assertEquals(5.0, util.abs(-5.0), EPSILON);
        assertEquals(0.0, util.abs(0.0), EPSILON);
        assertEquals(10.5, util.abs(-10.5), EPSILON);
    }

    @Test
    public void testMapInverse() {
        // Test inverse mapping
        double result = util.map(5.0, 0, 10, 0, 1);
        assertEquals(0.5, result, EPSILON);
    }

    @Test
    public void testDistanceSymmetry() {
        // Distance should be symmetric
        double dist1 = util.distance(0, 0, 10, 20);
        double dist2 = util.distance(10, 20, 0, 0);
        assertEquals(dist1, dist2, EPSILON);
    }

    @Test
    public void testManhattanSymmetry() {
        // Manhattan distance should be symmetric
        double dist1 = util.manhattan(0, 0, 10, 20);
        double dist2 = util.manhattan(10, 20, 0, 0);
        assertEquals(dist1, dist2, EPSILON);
    }

    @Test
    public void testLerpExtrapolation() {
        // Lerp can extrapolate beyond [0, 1]
        assertEquals(-5.0, util.lerp(0, 10, -0.5), EPSILON);
        assertEquals(15.0, util.lerp(0, 10, 1.5), EPSILON);
    }

    @Test
    public void testHashDistribution() {
        // Hashes should be well-distributed (basic test)
        int[] hashes = new int[100];
        for (int i = 0; i < 100; i++) {
            hashes[i] = util.hash(i, i);
        }

        // Check that not all hashes are the same
        boolean allSame = true;
        for (int i = 1; i < 100; i++) {
            if (hashes[i] != hashes[0]) {
                allSame = false;
                break;
            }
        }
        assertFalse("All hashes are the same", allSame);
    }

    @Test
    public void testDistanceLargeValues() {
        // Distance with large coordinates
        double dist = util.distance(0, 0, 1000, 1000);
        assertEquals(Math.sqrt(2000000), dist, EPSILON);
    }
}
