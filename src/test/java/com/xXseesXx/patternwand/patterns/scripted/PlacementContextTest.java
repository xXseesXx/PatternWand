package com.xXseesXx.patternwand.patterns.scripted;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for PlacementContext.
 * Tests that context correctly stores and retrieves placement information.
 */
public class PlacementContextTest {

    @Test
    public void testBasicConstruction() {
        PlacementContext context = new PlacementContext(
            10,
            20,
            30, // clicked position
            2, // click face
            5,
            15,
            25, // min
            15,
            25,
            35, // max
            90.0f,
            -45.0f, // player orientation
            1000L,
            500L // time
        );

        // Test clicked position
        assertEquals(10, context.getClickedX());
        assertEquals(20, context.getClickedY());
        assertEquals(30, context.getClickedZ());
        assertEquals(2, context.getClickFace());

        // Test bounding box
        assertEquals(5, context.getMinX());
        assertEquals(15, context.getMinY());
        assertEquals(25, context.getMinZ());
        assertEquals(15, context.getMaxX());
        assertEquals(25, context.getMaxY());
        assertEquals(35, context.getMaxZ());

        // Test player orientation
        assertEquals(90.0f, context.getPlayerYaw(), 0.001f);
        assertEquals(-45.0f, context.getPlayerPitch(), 0.001f);

        // Test time
        assertEquals(1000L, context.getWorldTime());
        assertEquals(500L, context.getDayTime());
    }

    @Test
    public void testZeroValues() {
        PlacementContext context = new PlacementContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0f, 0.0f, 0L, 0L);

        assertEquals(0, context.getClickedX());
        assertEquals(0, context.getClickedY());
        assertEquals(0, context.getClickedZ());
        assertEquals(0, context.getClickFace());
        assertEquals(0, context.getMinX());
        assertEquals(0, context.getMinY());
        assertEquals(0, context.getMinZ());
        assertEquals(0, context.getMaxX());
        assertEquals(0, context.getMaxY());
        assertEquals(0, context.getMaxZ());
        assertEquals(0.0f, context.getPlayerYaw(), 0.001f);
        assertEquals(0.0f, context.getPlayerPitch(), 0.001f);
        assertEquals(0L, context.getWorldTime());
        assertEquals(0L, context.getDayTime());
    }

    @Test
    public void testNegativeCoordinates() {
        PlacementContext context = new PlacementContext(
            -10,
            -20,
            -30,
            1,
            -15,
            -25,
            -35,
            -5,
            -15,
            -25,
            0.0f,
            0.0f,
            1000L,
            500L);

        assertEquals(-10, context.getClickedX());
        assertEquals(-20, context.getClickedY());
        assertEquals(-30, context.getClickedZ());
        assertEquals(-15, context.getMinX());
        assertEquals(-25, context.getMinY());
        assertEquals(-35, context.getMinZ());
        assertEquals(-5, context.getMaxX());
        assertEquals(-15, context.getMaxY());
        assertEquals(-25, context.getMaxZ());
    }

    @Test
    public void testPlayerOrientationFullRange() {
        // Test yaw (0-360 degrees)
        PlacementContext context1 = new PlacementContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0f, 0.0f, 0L, 0L);
        assertEquals(0.0f, context1.getPlayerYaw(), 0.001f);

        PlacementContext context2 = new PlacementContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 180.0f, 0.0f, 0L, 0L);
        assertEquals(180.0f, context2.getPlayerYaw(), 0.001f);

        PlacementContext context3 = new PlacementContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 359.9f, 0.0f, 0L, 0L);
        assertEquals(359.9f, context3.getPlayerYaw(), 0.001f);

        // Test pitch (-90 to 90 degrees)
        PlacementContext context4 = new PlacementContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0f, -90.0f, 0L, 0L);
        assertEquals(-90.0f, context4.getPlayerPitch(), 0.001f);

        PlacementContext context5 = new PlacementContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0f, 90.0f, 0L, 0L);
        assertEquals(90.0f, context5.getPlayerPitch(), 0.001f);
    }

    @Test
    public void testClickFaceValues() {
        // Test all 6 possible faces (0-5)
        for (int face = 0; face <= 5; face++) {
            PlacementContext context = new PlacementContext(0, 0, 0, face, 0, 0, 0, 0, 0, 0, 0.0f, 0.0f, 0L, 0L);
            assertEquals(face, context.getClickFace());
        }
    }

    @Test
    public void testLargeTimeValues() {
        long largeWorldTime = 999999999L;
        long largeDayTime = 24000L;

        PlacementContext context = new PlacementContext(
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0.0f,
            0.0f,
            largeWorldTime,
            largeDayTime);

        assertEquals(largeWorldTime, context.getWorldTime());
        assertEquals(largeDayTime, context.getDayTime());
    }

    @Test
    public void testBoundingBoxDimensions() {
        PlacementContext context = new PlacementContext(
            10,
            20,
            30,
            0,
            5,
            15,
            25, // min
            15,
            25,
            35, // max
            0.0f,
            0.0f,
            0L,
            0L);

        // Calculate dimensions
        int width = context.getMaxX() - context.getMinX();
        int height = context.getMaxY() - context.getMinY();
        int depth = context.getMaxZ() - context.getMinZ();

        assertEquals(10, width);
        assertEquals(10, height);
        assertEquals(10, depth);
    }

    @Test
    public void testClickedPositionWithinBoundingBox() {
        PlacementContext context = new PlacementContext(
            10,
            20,
            30, // clicked
            0,
            5,
            15,
            25, // min
            15,
            25,
            35, // max
            0.0f,
            0.0f,
            0L,
            0L);

        // Clicked position should be within bounding box
        assertTrue(context.getClickedX() >= context.getMinX());
        assertTrue(context.getClickedX() <= context.getMaxX());
        assertTrue(context.getClickedY() >= context.getMinY());
        assertTrue(context.getClickedY() <= context.getMaxY());
        assertTrue(context.getClickedZ() >= context.getMinZ());
        assertTrue(context.getClickedZ() <= context.getMaxZ());
    }

    @Test
    public void testSingleBlockBoundingBox() {
        // Bounding box with single block (min == max)
        PlacementContext context = new PlacementContext(10, 20, 30, 0, 10, 20, 30, 10, 20, 30, 0.0f, 0.0f, 0L, 0L);

        assertEquals(10, context.getMinX());
        assertEquals(10, context.getMaxX());
        assertEquals(20, context.getMinY());
        assertEquals(20, context.getMaxY());
        assertEquals(30, context.getMinZ());
        assertEquals(30, context.getMaxZ());
    }

    @Test
    public void testDayTimeCycle() {
        // Day time cycles 0-24000
        PlacementContext dawn = new PlacementContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0f, 0.0f, 0L, 0L);
        PlacementContext noon = new PlacementContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0f, 0.0f, 0L, 6000L);
        PlacementContext dusk = new PlacementContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0f, 0.0f, 0L, 12000L);
        PlacementContext midnight = new PlacementContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0f, 0.0f, 0L, 18000L);

        assertEquals(0L, dawn.getDayTime());
        assertEquals(6000L, noon.getDayTime());
        assertEquals(12000L, dusk.getDayTime());
        assertEquals(18000L, midnight.getDayTime());
    }
}
