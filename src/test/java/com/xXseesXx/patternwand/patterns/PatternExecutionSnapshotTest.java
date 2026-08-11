package com.xXseesXx.patternwand.patterns;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.LuaValue;

import com.xXseesXx.patternwand.patterns.scripted.CompiledScript;
import com.xXseesXx.patternwand.patterns.scripted.PlacementContext;

/**
 * Tests for PatternExecutionSnapshot immutability and thread safety guarantees.
 * 
 * This test suite verifies the async execution contract:
 * - All collections are deeply immutable
 * - No Minecraft objects leak through
 * - Safe to pass between threads
 */
public class PatternExecutionSnapshotTest {

    private CompiledScript mockScript;
    private PlacementContext mockContext;

    @Before
    public void setUp() throws Exception {
        // Create a minimal compiled script for testing
        LuaValue mockFunction = LuaValue.valueOf(0);
        mockScript = new CompiledScript("test_script", mockFunction);

        // Create a minimal placement context
        mockContext = new PlacementContext(
            0,
            64,
            0, // clicked pos
            1, // click face (UP)
            -10,
            50,
            -10, // min
            10,
            80,
            10, // max
            0.0f,
            0.0f, // player yaw/pitch
            1000L,
            6000L // world time, day time
        );
    }

    @Test
    public void testConstructorValidation() {
        List<PatternExecutionSnapshot.Position> positions = createPositionList(5);
        List<PatternExecutionSnapshot.PaletteSlot> palette = createPalette(3);

        // Null compiled script
        try {
            new PatternExecutionSnapshot(null, "test", positions, palette, 123L, null, mockContext);
            fail("Should throw IllegalArgumentException for null script");
        } catch (IllegalArgumentException e) {
            assertTrue(
                e.getMessage()
                    .contains("compiledScript"));
        }

        // Null pattern name
        try {
            new PatternExecutionSnapshot(mockScript, null, positions, palette, 123L, null, mockContext);
            fail("Should throw IllegalArgumentException for null pattern name");
        } catch (IllegalArgumentException e) {
            assertTrue(
                e.getMessage()
                    .contains("patternName"));
        }

        // Empty pattern name
        try {
            new PatternExecutionSnapshot(mockScript, "", positions, palette, 123L, null, mockContext);
            fail("Should throw IllegalArgumentException for empty pattern name");
        } catch (IllegalArgumentException e) {
            assertTrue(
                e.getMessage()
                    .contains("patternName"));
        }

        // Null positions
        try {
            new PatternExecutionSnapshot(mockScript, "test", null, palette, 123L, null, mockContext);
            fail("Should throw IllegalArgumentException for null positions");
        } catch (IllegalArgumentException e) {
            assertTrue(
                e.getMessage()
                    .contains("positions"));
        }

        // Empty positions
        try {
            new PatternExecutionSnapshot(
                mockScript,
                "test",
                new ArrayList<PatternExecutionSnapshot.Position>(),
                palette,
                123L,
                null,
                mockContext);
            fail("Should throw IllegalArgumentException for empty positions");
        } catch (IllegalArgumentException e) {
            assertTrue(
                e.getMessage()
                    .contains("positions"));
        }

        // Null palette
        try {
            new PatternExecutionSnapshot(mockScript, "test", positions, null, 123L, null, mockContext);
            fail("Should throw IllegalArgumentException for null palette");
        } catch (IllegalArgumentException e) {
            assertTrue(
                e.getMessage()
                    .contains("palette"));
        }

        // Null context
        try {
            new PatternExecutionSnapshot(mockScript, "test", positions, palette, 123L, null, null);
            fail("Should throw IllegalArgumentException for null context");
        } catch (IllegalArgumentException e) {
            assertTrue(
                e.getMessage()
                    .contains("context"));
        }
    }

    @Test
    public void testPositionsImmutability() {
        List<PatternExecutionSnapshot.Position> originalPositions = createPositionList(5);
        List<PatternExecutionSnapshot.PaletteSlot> palette = createPalette(3);

        PatternExecutionSnapshot snapshot = new PatternExecutionSnapshot(
            mockScript,
            "test",
            originalPositions,
            palette,
            123L,
            null,
            mockContext);

        // Get positions list
        List<PatternExecutionSnapshot.Position> positions = snapshot.getPositions();

        // Verify size matches
        assertEquals(5, positions.size());

        // Try to modify returned list - should throw UnsupportedOperationException
        try {
            positions.add(new PatternExecutionSnapshot.Position(100, 100, 100, 0, 0, 0));
            fail("Positions list should be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            positions.clear();
            fail("Positions list should be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        // Verify modifying original list doesn't affect snapshot
        originalPositions.add(new PatternExecutionSnapshot.Position(999, 999, 999, 0, 0, 0));
        assertEquals(
            "Snapshot should be isolated from original list",
            5,
            snapshot.getPositions()
                .size());
    }

    @Test
    public void testPaletteImmutability() {
        List<PatternExecutionSnapshot.Position> positions = createPositionList(5);
        List<PatternExecutionSnapshot.PaletteSlot> originalPalette = createPalette(3);

        PatternExecutionSnapshot snapshot = new PatternExecutionSnapshot(
            mockScript,
            "test",
            positions,
            originalPalette,
            123L,
            null,
            mockContext);

        // Get palette list
        List<PatternExecutionSnapshot.PaletteSlot> palette = snapshot.getPalette();

        // Verify size matches
        assertEquals(3, palette.size());

        // Try to modify returned list
        try {
            palette.add(new PatternExecutionSnapshot.PaletteSlot("minecraft:dirt", 0, 32));
            fail("Palette list should be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        // Verify modifying original list doesn't affect snapshot
        originalPalette.add(new PatternExecutionSnapshot.PaletteSlot("minecraft:gold_block", 0, 64));
        assertEquals(
            "Snapshot should be isolated from original list",
            3,
            snapshot.getPalette()
                .size());
    }

    @Test
    public void testParametersImmutability() {
        List<PatternExecutionSnapshot.Position> positions = createPositionList(5);
        List<PatternExecutionSnapshot.PaletteSlot> palette = createPalette(3);

        // Create original parameters map
        Map<String, Object> originalParams = new HashMap<String, Object>();
        originalParams.put("size", 10);
        originalParams.put("enabled", true);
        originalParams.put("name", "test");

        PatternExecutionSnapshot snapshot = new PatternExecutionSnapshot(
            mockScript,
            "test",
            positions,
            palette,
            123L,
            originalParams,
            mockContext);

        // Get parameters map
        Map<String, Object> params = snapshot.getParameters();

        // Verify contents match
        assertEquals(3, params.size());
        assertEquals(10, params.get("size"));
        assertEquals(true, params.get("enabled"));
        assertEquals("test", params.get("name"));

        // Try to modify returned map
        try {
            params.put("newKey", "newValue");
            fail("Parameters map should be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            params.clear();
            fail("Parameters map should be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        // Verify modifying original map doesn't affect snapshot
        originalParams.put("hacker", "gotcha");
        assertFalse(
            "Snapshot should be isolated from original map",
            snapshot.getParameters()
                .containsKey("hacker"));
        assertEquals(
            3,
            snapshot.getParameters()
                .size());
    }

    @Test
    public void testNullParametersHandling() {
        List<PatternExecutionSnapshot.Position> positions = createPositionList(5);
        List<PatternExecutionSnapshot.PaletteSlot> palette = createPalette(3);

        // Create snapshot with null parameters
        PatternExecutionSnapshot snapshot = new PatternExecutionSnapshot(
            mockScript,
            "test",
            positions,
            palette,
            123L,
            null,
            mockContext);

        // Should return empty immutable map, not null
        Map<String, Object> params = snapshot.getParameters();
        assertNotNull("Parameters should never be null", params);
        assertEquals("Parameters should be empty map", 0, params.size());

        // Should still be immutable
        try {
            params.put("key", "value");
            fail("Empty parameters map should still be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testPositionConversion() {
        // Test Position creation from coordinates
        PatternExecutionSnapshot.Position pos1 = new PatternExecutionSnapshot.Position(10, 64, -20, 5, 10, -15);
        assertEquals(10, pos1.x);
        assertEquals(64, pos1.y);
        assertEquals(-20, pos1.z);
        assertEquals(5, pos1.relX);
        assertEquals(10, pos1.relY);
        assertEquals(-15, pos1.relZ);

        // Test conversion back to Point3d
        portablejim.bbw.basics.Point3d point = pos1.toPoint3d();
        assertEquals(10, point.x);
        assertEquals(64, point.y);
        assertEquals(-20, point.z);
    }

    @Test
    public void testPaletteSlotData() {
        PatternExecutionSnapshot.PaletteSlot slot = new PatternExecutionSnapshot.PaletteSlot("minecraft:stone", 0, 32);

        assertEquals("minecraft:stone", slot.blockRegistryName);
        assertEquals(0, slot.metadata);
        assertEquals(32, slot.weight);

        // Verify toString works
        String str = slot.toString();
        assertTrue(str.contains("minecraft:stone"));
        assertTrue(str.contains("32"));
    }

    @Test
    public void testSnapshotToString() {
        List<PatternExecutionSnapshot.Position> positions = createPositionList(100);
        List<PatternExecutionSnapshot.PaletteSlot> palette = createPalette(10);

        PatternExecutionSnapshot snapshot = new PatternExecutionSnapshot(
            mockScript,
            "my_pattern",
            positions,
            palette,
            42L,
            null,
            mockContext);

        String str = snapshot.toString();
        assertTrue(str.contains("my_pattern"));
        assertTrue(str.contains("100")); // position count
        assertTrue(str.contains("10")); // palette count
        assertTrue(str.contains("42")); // seed
    }

    @Test
    public void testGetters() {
        List<PatternExecutionSnapshot.Position> positions = createPositionList(5);
        List<PatternExecutionSnapshot.PaletteSlot> palette = createPalette(3);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("test", 123);

        PatternExecutionSnapshot snapshot = new PatternExecutionSnapshot(
            mockScript,
            "test_pattern",
            positions,
            palette,
            9876L,
            params,
            mockContext);

        // Verify all getters return correct values
        assertSame(mockScript, snapshot.getCompiledScript());
        assertEquals("test_pattern", snapshot.getPatternName());
        assertEquals(
            5,
            snapshot.getPositions()
                .size());
        assertEquals(
            3,
            snapshot.getPalette()
                .size());
        assertEquals(9876L, snapshot.getSeed());
        assertEquals(
            123,
            snapshot.getParameters()
                .get("test"));
        assertSame(mockContext, snapshot.getContext());
    }

    // Helper methods

    private List<PatternExecutionSnapshot.Position> createPositionList(int count) {
        List<PatternExecutionSnapshot.Position> positions = new ArrayList<PatternExecutionSnapshot.Position>();
        for (int i = 0; i < count; i++) {
            positions.add(new PatternExecutionSnapshot.Position(i, 64 + i, i * 2, i, i, i * 2));
        }
        return positions;
    }

    private List<PatternExecutionSnapshot.PaletteSlot> createPalette(int count) {
        List<PatternExecutionSnapshot.PaletteSlot> palette = new ArrayList<PatternExecutionSnapshot.PaletteSlot>();
        for (int i = 0; i < count; i++) {
            palette.add(new PatternExecutionSnapshot.PaletteSlot("minecraft:block_" + i, i, (i + 1) * 10));
        }
        return palette;
    }
}
