package com.xXseesXx.patternwand.patterns.scripted.api;

import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * API wrapper exposing palette information to Lua scripts.
 * Provides access to palette slots and weighted random selection.
 */
public class PaletteAPI {

    private final IInventory paletteInventory;
    private final Random random;

    /**
     * Create a new Palette API.
     *
     * @param paletteInventory The palette inventory (27 slots)
     * @param seed             Random seed for weighted selection
     */
    public PaletteAPI(IInventory paletteInventory, long seed) {
        this.paletteInventory = paletteInventory;
        this.random = new Random(seed);
    }

    /**
     * Get the number of palette slots.
     *
     * @return Number of slots (always 27)
     */
    public int size() {
        return paletteInventory.getSizeInventory();
    }

    /**
     * Get the weight (stack size) of a palette slot.
     * Higher stack sizes mean higher probability in weighted random selection.
     *
     * @param index Palette slot index (0-26)
     * @return Stack size (1-64), or 0 if slot is empty
     */
    public int getWeight(double index) {
        int i = (int) Math.floor(index);
        if (i < 0 || i >= paletteInventory.getSizeInventory()) {
            return 0;
        }

        ItemStack stack = paletteInventory.getStackInSlot(i);
        if (stack == null) {
            return 0;
        }

        return stack.stackSize;
    }

    /**
     * Check if a palette slot is empty.
     *
     * @param index Palette slot index (0-26)
     * @return true if slot is empty
     */
    public boolean isEmpty(double index) {
        int i = (int) Math.floor(index);
        if (i < 0 || i >= paletteInventory.getSizeInventory()) {
            return true;
        }

        ItemStack stack = paletteInventory.getStackInSlot(i);
        return stack == null;
    }

    /**
     * Pick a random palette slot index based on stack size weights.
     * Higher stack sizes have proportionally higher probability of being selected.
     *
     * Example: If slot 0 has stack size 64 and slot 1 has stack size 16,
     * slot 0 has 80% chance and slot 1 has 20% chance.
     *
     * @return Selected palette slot index, or 0 if all slots are empty
     */
    public int pickWeighted() {
        // Calculate total weight
        int totalWeight = 0;
        for (int i = 0; i < paletteInventory.getSizeInventory(); i++) {
            ItemStack stack = paletteInventory.getStackInSlot(i);
            if (stack != null) {
                totalWeight += stack.stackSize;
            }
        }

        if (totalWeight == 0) {
            return 0; // All slots empty, return first slot
        }

        // Pick random value in range [0, totalWeight)
        int rand = random.nextInt(totalWeight);
        int cumulative = 0;

        // Find which slot this random value falls into
        for (int i = 0; i < paletteInventory.getSizeInventory(); i++) {
            ItemStack stack = paletteInventory.getStackInSlot(i);
            if (stack != null) {
                cumulative += stack.stackSize;
                if (rand < cumulative) {
                    return i;
                }
            }
        }

        return 0; // Fallback (should never reach here)
    }

    /**
     * Count how many non-empty slots are in the palette.
     *
     * @return Number of non-empty slots
     */
    public int countNonEmpty() {
        int count = 0;
        for (int i = 0; i < paletteInventory.getSizeInventory(); i++) {
            ItemStack stack = paletteInventory.getStackInSlot(i);
            if (stack != null) {
                count++;
            }
        }
        return count;
    }
}
