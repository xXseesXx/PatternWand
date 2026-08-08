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
        // Hash should produce a valid integer (any value is fine)
        assertTrue(true); // If we got here, hash was computed
    }

    // ========== NEW GEOMETRY FUNCTIONS TESTS ==========

    @Test
    public void testDistance3D() {
        // Test basic distance
        double dist = util.distance3d(0, 0, 0, 3, 4, 0);
        assertEquals(5.0, dist, EPSILON); // 3-4-5 triangle

        // Test zero distance
        dist = util.distance3d(5, 5, 5, 5, 5, 5);
        assertEquals(0.0, dist, EPSILON);

        // Test 3D pythagorean
        dist = util.distance3d(0, 0, 0, 1, 1, 1);
        assertEquals(Math.sqrt(3), dist, EPSILON);

        // Test negative coordinates
        dist = util.distance3d(-1, -1, -1, 1, 1, 1);
        assertEquals(Math.sqrt(12), dist, EPSILON);
    }

    @Test
    public void testInSphere() {
        // Center point should be in sphere
        assertTrue(util.inSphere(5, 5, 5, 5, 5, 5, 10));

        // Point at exact radius should be in sphere
        assertTrue(util.inSphere(15, 5, 5, 5, 5, 5, 10));

        // Point outside radius should not be in sphere
        assertFalse(util.inSphere(16, 5, 5, 5, 5, 5, 10));

        // Test with zero radius
        assertTrue(util.inSphere(0, 0, 0, 0, 0, 0, 0));
        assertFalse(util.inSphere(1, 0, 0, 0, 0, 0, 0));

        // Test negative coordinates
        assertTrue(util.inSphere(-5, -5, -5, -5, -5, -5, 5));
    }

    @Test
    public void testInBox() {
        // Point inside box
        assertTrue(util.inBox(5, 5, 5, 0, 0, 0, 10, 10, 10));

        // Point on boundary should be inside
        assertTrue(util.inBox(0, 0, 0, 0, 0, 0, 10, 10, 10));
        assertTrue(util.inBox(10, 10, 10, 0, 0, 0, 10, 10, 10));

        // Point outside box
        assertFalse(util.inBox(11, 5, 5, 0, 0, 0, 10, 10, 10));
        assertFalse(util.inBox(5, 11, 5, 0, 0, 0, 10, 10, 10));
        assertFalse(util.inBox(5, 5, 11, 0, 0, 0, 10, 10, 10));

        // Test negative coordinates
        assertTrue(util.inBox(-5, -5, -5, -10, -10, -10, 0, 0, 0));
        assertFalse(util.inBox(-11, -5, -5, -10, -10, -10, 0, 0, 0));

        // Test zero-size box
        assertTrue(util.inBox(5, 5, 5, 5, 5, 5, 5, 5, 5));
        assertFalse(util.inBox(6, 5, 5, 5, 5, 5, 5, 5, 5));
    }

    @Test
    public void testRotate2D() {
        // Test 90 degree rotation
        double[] result = util.rotate2D(1, 0, 90);
        assertEquals(0.0, result[0], EPSILON);
        assertEquals(1.0, result[1], EPSILON);

        // Test 180 degree rotation
        result = util.rotate2D(1, 0, 180);
        assertEquals(-1.0, result[0], EPSILON);
        assertEquals(0.0, result[1], EPSILON);

        // Test 270 degree rotation
        result = util.rotate2D(1, 0, 270);
        assertEquals(0.0, result[0], EPSILON);
        assertEquals(-1.0, result[1], EPSILON);

        // Test 360 degree rotation (back to start)
        result = util.rotate2D(1, 0, 360);
        assertEquals(1.0, result[0], EPSILON);
        assertEquals(0.0, result[1], EPSILON);

        // Test no rotation
        result = util.rotate2D(5, 3, 0);
        assertEquals(5.0, result[0], EPSILON);
        assertEquals(3.0, result[1], EPSILON);

        // Test negative angle
        result = util.rotate2D(1, 0, -90);
        assertEquals(0.0, result[0], EPSILON);
        assertEquals(-1.0, result[1], EPSILON);
    }

    @Test
    public void testMod() {
        // Test positive modulo
        assertEquals(2.0, util.mod(7, 5), EPSILON);
        assertEquals(0.0, util.mod(10, 5), EPSILON);
        assertEquals(1.0, util.mod(11, 5), EPSILON);

        // Test negative numbers (should always return positive)
        assertEquals(3.0, util.mod(-7, 5), EPSILON);
        assertEquals(0.0, util.mod(-10, 5), EPSILON);
        assertEquals(4.0, util.mod(-1, 5), EPSILON);

        // Test floating point
        assertEquals(0.5, util.mod(5.5, 2.5), EPSILON);
        assertEquals(1.0, util.mod(-4.0, 5.0), EPSILON);
    }

    @Test
    public void testSign() {
        // Test positive
        assertEquals(1.0, util.sign(5.5), EPSILON);
        assertEquals(1.0, util.sign(0.001), EPSILON);

        // Test negative
        assertEquals(-1.0, util.sign(-5.5), EPSILON);
        assertEquals(-1.0, util.sign(-0.001), EPSILON);

        // Test zero
        assertEquals(0.0, util.sign(0), EPSILON);
        assertEquals(0.0, util.sign(0.0), EPSILON);
    }

    @Test
    public void testSmoothstep() {
        // Test at edges
        assertEquals(0.0, util.smoothstep(0, 1, 0), EPSILON);
        assertEquals(1.0, util.smoothstep(0, 1, 1), EPSILON);

        // Test at midpoint
        assertEquals(0.5, util.smoothstep(0, 1, 0.5), EPSILON);

        // Test outside range (should clamp)
        assertEquals(0.0, util.smoothstep(0, 1, -1), EPSILON);
        assertEquals(1.0, util.smoothstep(0, 1, 2), EPSILON);

        // Test different range
        assertEquals(0.0, util.smoothstep(10, 20, 10), EPSILON);
        assertEquals(1.0, util.smoothstep(10, 20, 20), EPSILON);
        assertEquals(0.5, util.smoothstep(10, 20, 15), EPSILON);

        // Verify smooth curve (should be smooth, not linear)
        double linear = 0.25; // Linear interpolation at 0.25
        double smooth = util.smoothstep(0, 1, 0.25);
        // Smoothstep should be less than linear in first half
        assertTrue(smooth < linear);

        linear = 0.75; // Linear interpolation at 0.75
        smooth = util.smoothstep(0, 1, 0.75);
        // Smoothstep should be more than linear in second half
        assertTrue(smooth > linear);
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
