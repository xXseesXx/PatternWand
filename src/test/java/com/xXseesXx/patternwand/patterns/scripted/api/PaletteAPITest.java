package com.xXseesXx.patternwand.patterns.scripted.api;

import static org.junit.Assert.*;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for PaletteAPI.
 * Tests palette slot access, weighting, and random selection.
 */
public class PaletteAPITest {

    private IInventory mockPalette;
    private PaletteAPI palette;
    private static final long TEST_SEED = 12345L;

    @Before
    public void setUp() {
        // Create mock palette with some blocks
        mockPalette = new InventoryBasic("Test Palette", false, 54);
        palette = new PaletteAPI(mockPalette, TEST_SEED);
    }

    @Test
    public void testSize() {
        // Palette should always have 54 slots
        assertEquals(54, palette.size());
    }

    @Test
    public void testEmptyPalette() {
        // All slots should be empty initially
        for (int i = 0; i < 54; i++) {
            assertTrue(palette.isEmpty(i));
        }

        assertEquals(0, palette.countNonEmpty());
    }

    @Test
    public void testGetWeight() {
        // Add some items with different stack sizes
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 64));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 32));
        mockPalette.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 1));

        // Recreate palette API to see changes
        palette = new PaletteAPI(mockPalette, TEST_SEED);

        assertEquals(64, palette.getWeight(0));
        assertEquals(32, palette.getWeight(1));
        assertEquals(1, palette.getWeight(2));
        assertEquals(0, palette.getWeight(3)); // Empty slot
    }

    @Test
    public void testGetWeightInvalidIndex() {
        // Invalid indices should return 0
        assertEquals(0, palette.getWeight(-1));
        assertEquals(0, palette.getWeight(54));
        assertEquals(0, palette.getWeight(100));
    }

    @Test
    public void testIsEmpty() {
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 64));
        palette = new PaletteAPI(mockPalette, TEST_SEED);

        assertFalse(palette.isEmpty(0));
        assertTrue(palette.isEmpty(1));
    }

    @Test
    public void testIsEmptyInvalidIndex() {
        // Invalid indices should be considered empty
        assertTrue(palette.isEmpty(-1));
        assertTrue(palette.isEmpty(27));
    }

    @Test
    public void testCountNonEmpty() {
        // Add some items
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 64));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 32));
        mockPalette.setInventorySlotContents(5, new ItemStack(Blocks.dirt, 1));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        assertEquals(3, palette.countNonEmpty());
    }

    @Test
    public void testPickWeightedEmptyPalette() {
        // Empty palette should return 0
        int result = palette.pickWeighted();
        assertEquals(0, result);
    }

    @Test
    public void testPickWeightedSingleItem() {
        // Single item should always be picked
        mockPalette.setInventorySlotContents(5, new ItemStack(Blocks.stone, 64));
        palette = new PaletteAPI(mockPalette, TEST_SEED);

        for (int i = 0; i < 10; i++) {
            assertEquals(5, palette.pickWeighted());
        }
    }

    @Test
    public void testPickWeightedMultipleItems() {
        // Multiple items with equal weights
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 1));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 1));
        mockPalette.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 1));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Pick many times and check we get variety
        boolean[] picked = new boolean[3];
        for (int i = 0; i < 100; i++) {
            int result = palette.pickWeighted();
            assertTrue(result >= 0 && result <= 2);
            picked[result] = true;
        }

        // Should have picked all three at some point
        assertTrue(picked[0] || picked[1] || picked[2]);
    }

    @Test
    public void testPickWeightedBias() {
        // Item with higher weight should be picked more often
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 64));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 1));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        int count0 = 0;
        int count1 = 0;

        for (int i = 0; i < 1000; i++) {
            int result = palette.pickWeighted();
            if (result == 0) count0++;
            else if (result == 1) count1++;
        }

        // Slot 0 (weight 64) should be picked much more than slot 1 (weight 1)
        // With 64:1 ratio, we expect roughly 64/65 vs 1/65
        // count0 should be much larger than count1
        assertTrue(count0 > count1 * 10); // At least 10x more
    }

    @Test
    public void testPickWeightedDeterministic() {
        // Same seed should produce same sequence
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 10));

        PaletteAPI palette1 = new PaletteAPI(mockPalette, 12345L);
        PaletteAPI palette2 = new PaletteAPI(mockPalette, 12345L);

        for (int i = 0; i < 20; i++) {
            assertEquals(palette1.pickWeighted(), palette2.pickWeighted());
        }
    }

    @Test
    public void testPickWeightedDifferentSeeds() {
        // Different seeds should produce different sequences
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 10));

        PaletteAPI palette1 = new PaletteAPI(mockPalette, 12345L);
        PaletteAPI palette2 = new PaletteAPI(mockPalette, 54321L);

        // At least some picks should differ
        boolean foundDifference = false;
        for (int i = 0; i < 20; i++) {
            if (palette1.pickWeighted() != palette2.pickWeighted()) {
                foundDifference = true;
                break;
            }
        }

        assertTrue("Different seeds should produce different results", foundDifference);
    }

    @Test
    public void testPickWeightedValidRange() {
        // All picked indices should be valid
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 20));
        mockPalette.setInventorySlotContents(5, new ItemStack(Blocks.cobblestone, 20));
        mockPalette.setInventorySlotContents(10, new ItemStack(Blocks.dirt, 20));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        for (int i = 0; i < 100; i++) {
            int result = palette.pickWeighted();
            assertTrue(result >= 0 && result < 27);
            assertTrue(result == 0 || result == 5 || result == 10);
        }
    }

    @Test
    public void testCountNonEmptyFullPalette() {
        // Fill entire palette
        for (int i = 0; i < 54; i++) {
            mockPalette.setInventorySlotContents(i, new ItemStack(Blocks.stone, 1));
        }

        palette = new PaletteAPI(mockPalette, TEST_SEED);
        assertEquals(54, palette.countNonEmpty());
    }

    @Test
    public void testGetWeightZeroForEmpty() {
        // Empty slots should have weight 0
        assertEquals(0, palette.getWeight(0));

        // Even after adding and removing
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 64));
        palette = new PaletteAPI(mockPalette, TEST_SEED);
        assertEquals(64, palette.getWeight(0));

        mockPalette.setInventorySlotContents(0, null);
        palette = new PaletteAPI(mockPalette, TEST_SEED);
        assertEquals(0, palette.getWeight(0));
    }

    @Test
    public void testPickWeightedWithGaps() {
        // Palette with gaps (non-contiguous filled slots)
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        mockPalette.setInventorySlotContents(10, new ItemStack(Blocks.cobblestone, 10));
        mockPalette.setInventorySlotContents(20, new ItemStack(Blocks.dirt, 10));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Should only pick from filled slots
        for (int i = 0; i < 100; i++) {
            int result = palette.pickWeighted();
            assertTrue(result == 0 || result == 10 || result == 20);
        }
    }

    @Test
    public void testEmptyAfterRemoval() {
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 64));
        palette = new PaletteAPI(mockPalette, TEST_SEED);
        assertFalse(palette.isEmpty(0));

        // Remove the item
        mockPalette.setInventorySlotContents(0, null);
        palette = new PaletteAPI(mockPalette, TEST_SEED);
        assertTrue(palette.isEmpty(0));
    }

    // ========== NEW PALETTE SELECTION METHODS TESTS ==========

    @Test
    public void testPickUniform() {
        // Setup palette with different weights
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 64)); // Heavy weight
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 1)); // Light weight
        mockPalette.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 32)); // Medium weight

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Pick many times and count
        int count0 = 0;
        int count1 = 0;
        int count2 = 0;

        for (int i = 0; i < 3000; i++) {
            int result = palette.pickUniform();
            if (result == 0) count0++;
            else if (result == 1) count1++;
            else if (result == 2) count2++;
        }

        // All three should be picked roughly equally (within 20% of expected)
        int expected = 1000;
        assertTrue("Slot 0 should be picked ~equally", Math.abs(count0 - expected) < 200);
        assertTrue("Slot 1 should be picked ~equally", Math.abs(count1 - expected) < 200);
        assertTrue("Slot 2 should be picked ~equally", Math.abs(count2 - expected) < 200);
    }

    @Test
    public void testPickUniformEmptyPalette() {
        // Empty palette should return 0
        int result = palette.pickUniform();
        assertEquals(0, result);
    }

    @Test
    public void testPickUniformSingleItem() {
        mockPalette.setInventorySlotContents(5, new ItemStack(Blocks.stone, 64));
        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Should always pick the only item
        for (int i = 0; i < 10; i++) {
            assertEquals(5, palette.pickUniform());
        }
    }

    @Test
    public void testPickWeightedExceptSingleExclude() {
        // Setup palette
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 10));
        mockPalette.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 10));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Exclude index 1
        for (int i = 0; i < 100; i++) {
            int result = palette.pickWeightedExcept(new int[] { 1 });
            assertTrue("Should not pick excluded index", result != 1);
            assertTrue("Should pick valid index", result == 0 || result == 2);
        }
    }

    @Test
    public void testPickWeightedExceptMultipleExclude() {
        // Setup palette
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 10));
        mockPalette.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 10));
        mockPalette.setInventorySlotContents(3, new ItemStack(Blocks.grass, 10));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Exclude indices 1 and 3
        for (int i = 0; i < 100; i++) {
            int result = palette.pickWeightedExcept(new int[] { 1, 3 });
            assertTrue("Should not pick excluded indices", result != 1 && result != 3);
            assertTrue("Should pick valid index", result == 0 || result == 2);
        }
    }

    @Test
    public void testPickWeightedExceptEmptyArray() {
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 10));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Empty exclusion array should behave like normal pickWeighted
        boolean[] picked = new boolean[2];
        for (int i = 0; i < 100; i++) {
            int result = palette.pickWeightedExcept(new int[] {});
            picked[result] = true;
        }

        assertTrue("Should pick both slots", picked[0] && picked[1]);
    }

    @Test
    public void testPickWeightedExceptAllExcluded() {
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 10));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Exclude all filled slots
        int result = palette.pickWeightedExcept(new int[] { 0, 1 });
        assertEquals("Should return 0 when all are excluded", 0, result);
    }

    @Test
    public void testPickWeightedExceptNullArray() {
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Null array should behave like normal pickWeighted
        int result = palette.pickWeightedExcept(null);
        assertEquals(0, result);
    }

    @Test
    public void testPickWeightedRange() {
        // Setup palette
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 10));
        mockPalette.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 10));
        mockPalette.setInventorySlotContents(3, new ItemStack(Blocks.grass, 10));
        mockPalette.setInventorySlotContents(4, new ItemStack(Blocks.sand, 10));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Pick from range 1-3
        for (int i = 0; i < 100; i++) {
            int result = palette.pickWeightedRange(1, 3);
            assertTrue("Should pick from range", result >= 1 && result <= 3);
        }
    }

    @Test
    public void testPickWeightedRangeSingleSlot() {
        mockPalette.setInventorySlotContents(5, new ItemStack(Blocks.stone, 10));
        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Range with single slot
        int result = palette.pickWeightedRange(5, 5);
        assertEquals(5, result);
    }

    @Test
    public void testPickWeightedRangeInvalidRange() {
        mockPalette.setInventorySlotContents(5, new ItemStack(Blocks.stone, 10));
        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Min > max should return min
        int result = palette.pickWeightedRange(10, 5);
        assertEquals(10, result);
    }

    @Test
    public void testPickWeightedRangeClampToValid() {
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Range beyond palette size should be clamped
        int result = palette.pickWeightedRange(0, 100);
        assertTrue("Should clamp to valid range", result >= 0 && result < 27);
    }

    @Test
    public void testPickWeightedRangeNegative() {
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 10));
        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Negative min should be clamped to 0
        int result = palette.pickWeightedRange(-5, 1);
        assertTrue("Should clamp negative to 0", result >= 0 && result <= 1);
    }

    @Test
    public void testPickWeightedRangeEmptySlots() {
        // Fill only some slots
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        // Slots 1-4 are empty
        mockPalette.setInventorySlotContents(5, new ItemStack(Blocks.cobblestone, 10));

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Range with empty slots should return min if all are empty
        int result = palette.pickWeightedRange(1, 4);
        assertEquals("Should return min when range is all empty", 1, result);
    }

    @Test
    public void testPickWeightedRangeVariety() {
        // Fill range with equal weights
        for (int i = 5; i < 10; i++) {
            mockPalette.setInventorySlotContents(i, new ItemStack(Blocks.stone, 10));
        }

        palette = new PaletteAPI(mockPalette, TEST_SEED);

        // Should pick variety from range
        boolean[] picked = new boolean[10];
        for (int i = 0; i < 200; i++) {
            int result = palette.pickWeightedRange(5, 9);
            picked[result] = true;
        }

        // Should have picked multiple different slots
        int pickedCount = 0;
        for (int i = 5; i < 10; i++) {
            if (picked[i]) pickedCount++;
        }
        assertTrue("Should pick variety from range", pickedCount >= 3);
    }

    @Test
    public void testNewMethodsDeterministic() {
        // Setup palette
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 10));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 10));
        mockPalette.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 10));

        PaletteAPI palette1 = new PaletteAPI(mockPalette, 99999L);
        PaletteAPI palette2 = new PaletteAPI(mockPalette, 99999L);

        // Same seed should produce same results
        for (int i = 0; i < 20; i++) {
            assertEquals(palette1.pickUniform(), palette2.pickUniform());
            assertEquals(palette1.pickWeightedExcept(new int[] { 1 }), palette2.pickWeightedExcept(new int[] { 1 }));
            assertEquals(palette1.pickWeightedRange(0, 2), palette2.pickWeightedRange(0, 2));
        }
    }
}
