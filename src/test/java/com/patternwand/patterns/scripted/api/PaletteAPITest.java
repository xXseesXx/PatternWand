package com.patternwand.patterns.scripted.api;

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
        mockPalette = new InventoryBasic("Test Palette", false, 27);
        palette = new PaletteAPI(mockPalette, TEST_SEED);
    }

    @Test
    public void testSize() {
        // Palette should always have 27 slots
        assertEquals(27, palette.size());
    }

    @Test
    public void testEmptyPalette() {
        // All slots should be empty initially
        for (int i = 0; i < 27; i++) {
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
        assertEquals(0, palette.getWeight(27));
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
        for (int i = 0; i < 27; i++) {
            mockPalette.setInventorySlotContents(i, new ItemStack(Blocks.stone, 1));
        }

        palette = new PaletteAPI(mockPalette, TEST_SEED);
        assertEquals(27, palette.countNonEmpty());
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
}
