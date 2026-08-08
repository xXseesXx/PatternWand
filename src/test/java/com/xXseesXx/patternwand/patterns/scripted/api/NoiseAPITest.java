package com.xXseesXx.patternwand.patterns.scripted.api;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for NoiseAPI.
 * Tests Perlin and Simplex noise generation.
 */
public class NoiseAPITest {

    private NoiseAPI noise;
    private static final long TEST_SEED = 12345L;
    private static final double EPSILON = 0.001; // For double comparisons

    @Before
    public void setUp() {
        noise = new NoiseAPI(TEST_SEED);
    }

    @Test
    public void testPerlin2DRange() {
        // Test that Perlin noise returns values in [-1, 1]
        for (int i = 0; i < 100; i++) {
            double value = noise.perlin(i * 0.5, i * 0.3);
            assertTrue("Perlin value out of range: " + value, value >= -1.0 && value <= 1.0);
        }
    }

    @Test
    public void testPerlin3DRange() {
        // Test that 3D Perlin noise returns values in [-1, 1]
        for (int i = 0; i < 100; i++) {
            double value = noise.perlin3d(i * 0.5, i * 0.3, i * 0.7);
            assertTrue("Perlin3D value out of range: " + value, value >= -1.0 && value <= 1.0);
        }
    }

    @Test
    public void testSimplex2DRange() {
        // Test that Simplex noise returns values in [-1, 1]
        for (int i = 0; i < 100; i++) {
            double value = noise.simplex(i * 0.5, i * 0.3);
            assertTrue("Simplex value out of range: " + value, value >= -1.0 && value <= 1.0);
        }
    }

    @Test
    public void testSimplex3DRange() {
        // Test that 3D Simplex noise returns values in [-1, 1]
        for (int i = 0; i < 100; i++) {
            double value = noise.simplex3d(i * 0.5, i * 0.3, i * 0.7);
            assertTrue("Simplex3D value out of range: " + value, value >= -1.0 && value <= 1.0);
        }
    }

    @Test
    public void testPerlinDeterministic() {
        // Same input should give same output
        double value1 = noise.perlin(10.5, 20.3);
        double value2 = noise.perlin(10.5, 20.3);
        assertEquals(value1, value2, EPSILON);
    }

    @Test
    public void testSimplexDeterministic() {
        // Same input should give same output
        double value1 = noise.simplex(10.5, 20.3);
        double value2 = noise.simplex(10.5, 20.3);
        assertEquals(value1, value2, EPSILON);
    }

    @Test
    public void testPerlin3DDeterministic() {
        // Same input should give same output
        double value1 = noise.perlin3d(10.5, 15.2, 20.3);
        double value2 = noise.perlin3d(10.5, 15.2, 20.3);
        assertEquals(value1, value2, EPSILON);
    }

    @Test
    public void testSimplex3DDeterministic() {
        // Same input should give same output
        double value1 = noise.simplex3d(10.5, 15.2, 20.3);
        double value2 = noise.simplex3d(10.5, 15.2, 20.3);
        assertEquals(value1, value2, EPSILON);
    }

    @Test
    public void testPerlinVariesWithInput() {
        // Different inputs should give different outputs (usually)
        // Use non-integer coordinates since Perlin noise always returns 0 at integer grid points
        double value1 = noise.perlin(0.5, 0.5);
        double value2 = noise.perlin(100.5, 100.5);
        assertNotEquals(value1, value2, EPSILON);
    }

    @Test
    public void testSimplexVariesWithInput() {
        // Different inputs should give different outputs (usually)
        double value1 = noise.simplex(0, 0);
        double value2 = noise.simplex(100, 100);
        assertNotEquals(value1, value2, EPSILON);
    }

    @Test
    public void testPerlinContinuity() {
        // Noise should be continuous (nearby points have similar values)
        double value1 = noise.perlin(10.0, 10.0);
        double value2 = noise.perlin(10.01, 10.01);

        // Values should be close (within 0.1 for this small step)
        assertTrue(Math.abs(value1 - value2) < 0.1);
    }

    @Test
    public void testSimplexContinuity() {
        // Noise should be continuous (nearby points have similar values)
        double value1 = noise.simplex(10.0, 10.0);
        double value2 = noise.simplex(10.01, 10.01);

        // Values should be close (within 0.1 for this small step)
        assertTrue(Math.abs(value1 - value2) < 0.1);
    }

    @Test
    public void testDifferentSeedsProduceDifferentNoise() {
        NoiseAPI noise2 = new NoiseAPI(54321L);

        // Use non-integer coordinates since Perlin noise always returns 0 at integer grid points
        double value1 = noise.perlin(10.5, 10.5);
        double value2 = noise2.perlin(10.5, 10.5);

        // Different seeds should produce different results
        assertNotEquals(value1, value2, EPSILON);
    }

    @Test
    public void testPerlin3DContinuity() {
        // 3D noise should also be continuous
        double value1 = noise.perlin3d(10.0, 10.0, 10.0);
        double value2 = noise.perlin3d(10.01, 10.01, 10.01);

        assertTrue(Math.abs(value1 - value2) < 0.1);
    }

    @Test
    public void testNoiseAtOrigin() {
        // Test noise at origin (0,0)
        double perlinValue = noise.perlin(0, 0);
        double simplexValue = noise.simplex(0, 0);

        assertTrue(perlinValue >= -1.0 && perlinValue <= 1.0);
        assertTrue(simplexValue >= -1.0 && simplexValue <= 1.0);
    }

    @Test
    public void testNoiseWithNegativeCoordinates() {
        // Test with negative coordinates
        double value = noise.perlin(-10.5, -20.3);
        assertTrue(value >= -1.0 && value <= 1.0);

        value = noise.simplex(-10.5, -20.3);
        assertTrue(value >= -1.0 && value <= 1.0);
    }

    @Test
    public void testNoiseWithLargeCoordinates() {
        // Test with very large coordinates
        double value = noise.perlin(10000.0, 20000.0);
        assertTrue(value >= -1.0 && value <= 1.0);

        value = noise.simplex(10000.0, 20000.0);
        assertTrue(value >= -1.0 && value <= 1.0);
    }

    // -------------------------------------------------------------------------
    // Value noise tests
    // -------------------------------------------------------------------------

    @Test
    public void testValue2DRange() {
        // Test that value noise returns values in [-1, 1]
        for (int i = 0; i < 100; i++) {
            double v = noise.value(i * 0.5, i * 0.3);
            assertTrue("Value noise 2D out of range: " + v, v >= -1.0 && v <= 1.0);
        }
    }

    @Test
    public void testValue3DRange() {
        // Test that 3D value noise returns values in [-1, 1]
        for (int i = 0; i < 100; i++) {
            double v = noise.value3d(i * 0.5, i * 0.3, i * 0.7);
            assertTrue("Value noise 3D out of range: " + v, v >= -1.0 && v <= 1.0);
        }
    }

    @Test
    public void testValue2DDeterministic() {
        // Same input should give same output
        double v1 = noise.value(10.5, 20.3);
        double v2 = noise.value(10.5, 20.3);
        assertEquals(v1, v2, EPSILON);
    }

    @Test
    public void testValue3DDeterministic() {
        // Same input should give same output
        double v1 = noise.value3d(10.5, 15.2, 20.3);
        double v2 = noise.value3d(10.5, 15.2, 20.3);
        assertEquals(v1, v2, EPSILON);
    }

    @Test
    public void testValue2DVariesWithInput() {
        // Different inputs should give different outputs
        double v1 = noise.value(0.5, 0.5);
        double v2 = noise.value(100.5, 100.5);
        assertNotEquals(v1, v2, EPSILON);
    }

    @Test
    public void testValue3DVariesWithInput() {
        // Different inputs should give different outputs
        double v1 = noise.value3d(0.5, 0.5, 0.5);
        double v2 = noise.value3d(100.5, 100.5, 100.5);
        assertNotEquals(v1, v2, EPSILON);
    }

    @Test
    public void testValue2DContinuity() {
        // Value noise should be continuous: nearby points have similar values
        double v1 = noise.value(10.0, 10.0);
        double v2 = noise.value(10.01, 10.01);
        assertTrue("Value noise 2D not continuous: " + Math.abs(v1 - v2), Math.abs(v1 - v2) < 0.1);
    }

    @Test
    public void testValue3DContinuity() {
        // 3D value noise should also be continuous
        double v1 = noise.value3d(10.0, 10.0, 10.0);
        double v2 = noise.value3d(10.01, 10.01, 10.01);
        assertTrue("Value noise 3D not continuous: " + Math.abs(v1 - v2), Math.abs(v1 - v2) < 0.1);
    }

    @Test
    public void testValueDifferentSeedsDifferentResults() {
        NoiseAPI noise2 = new NoiseAPI(54321L);
        double v1 = noise.value(10.5, 10.5);
        double v2 = noise2.value(10.5, 10.5);
        assertNotEquals(v1, v2, EPSILON);
    }

    @Test
    public void testValueWithNegativeCoordinates() {
        // Negative coordinates should still return in-range values
        double v = noise.value(-10.5, -20.3);
        assertTrue(v >= -1.0 && v <= 1.0);

        v = noise.value3d(-10.5, -5.0, -20.3);
        assertTrue(v >= -1.0 && v <= 1.0);
    }

    @Test
    public void testValueWithLargeCoordinates() {
        // Large coordinates should still return in-range values
        double v = noise.value(10000.0, 20000.0);
        assertTrue(v >= -1.0 && v <= 1.0);

        v = noise.value3d(10000.0, 5000.0, 20000.0);
        assertTrue(v >= -1.0 && v <= 1.0);
    }
}
