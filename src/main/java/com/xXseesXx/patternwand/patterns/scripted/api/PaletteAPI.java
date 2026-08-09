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
     * @param paletteInventory The palette inventory (54 slots)
     * @param seed             Random seed for weighted selection
     */
    public PaletteAPI(IInventory paletteInventory, long seed) {
        this.paletteInventory = paletteInventory;
        this.random = new Random(seed);
    }

    /**
     * Get the number of palette slots.
     *
     * @return Number of slots (always 54)
     */
    public int size() {
        return paletteInventory.getSizeInventory();
    }

    /**
     * Get the weight (stack size) of a palette slot.
     * Higher stack sizes mean higher probability in weighted random selection.
     *
     * @param index Palette slot index (0-53)
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
     * @param index Palette slot index (0-53)
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

    /**
     * Pick a random non-empty palette slot with uniform probability.
     * Each non-empty slot has equal chance regardless of stack size.
     *
     * @return Selected palette slot index, or 0 if all slots are empty
     */
    public int pickUniform() {
        // Collect non-empty slot indices
        java.util.List<Integer> nonEmptySlots = new java.util.ArrayList<Integer>();
        for (int i = 0; i < paletteInventory.getSizeInventory(); i++) {
            ItemStack stack = paletteInventory.getStackInSlot(i);
            if (stack != null) {
                nonEmptySlots.add(i);
            }
        }

        if (nonEmptySlots.isEmpty()) {
            return 0; // All slots empty
        }

        return nonEmptySlots.get(random.nextInt(nonEmptySlots.size()));
    }

    /**
     * Pick a random palette slot based on stack size weights, excluding specified indices.
     *
     * @param excludeIndices Array of indices to exclude
     * @return Selected palette slot index, or 0 if all valid slots are empty
     */
    public int pickWeightedExcept(int[] excludeIndices) {
        // Create set of excluded indices for fast lookup
        java.util.Set<Integer> excluded = new java.util.HashSet<Integer>();
        if (excludeIndices != null) {
            for (int idx : excludeIndices) {
                excluded.add(idx);
            }
        }

        // Calculate total weight excluding specified indices
        int totalWeight = 0;
        for (int i = 0; i < paletteInventory.getSizeInventory(); i++) {
            if (!excluded.contains(i)) {
                ItemStack stack = paletteInventory.getStackInSlot(i);
                if (stack != null) {
                    totalWeight += stack.stackSize;
                }
            }
        }

        if (totalWeight == 0) {
            return 0; // All valid slots empty
        }

        // Pick random value in range [0, totalWeight)
        int rand = random.nextInt(totalWeight);
        int cumulative = 0;

        // Find which slot this random value falls into
        for (int i = 0; i < paletteInventory.getSizeInventory(); i++) {
            if (!excluded.contains(i)) {
                ItemStack stack = paletteInventory.getStackInSlot(i);
                if (stack != null) {
                    cumulative += stack.stackSize;
                    if (rand < cumulative) {
                        return i;
                    }
                }
            }
        }

        return 0; // Fallback
    }

    /**
     * Pick a random palette slot based on stack size weights, only from specified range.
     *
     * @param min Minimum index (inclusive)
     * @param max Maximum index (inclusive)
     * @return Selected palette slot index, or min if all slots in range are empty
     */
    public int pickWeightedRange(int min, int max) {
        // Clamp to valid range
        if (min < 0) min = 0;
        if (max >= paletteInventory.getSizeInventory()) max = paletteInventory.getSizeInventory() - 1;
        if (min > max) {
            return min;
        }

        // Calculate total weight in range
        int totalWeight = 0;
        for (int i = min; i <= max; i++) {
            ItemStack stack = paletteInventory.getStackInSlot(i);
            if (stack != null) {
                totalWeight += stack.stackSize;
            }
        }

        if (totalWeight == 0) {
            return min; // All slots in range empty
        }

        // Pick random value in range [0, totalWeight)
        int rand = random.nextInt(totalWeight);
        int cumulative = 0;

        // Find which slot this random value falls into
        for (int i = min; i <= max; i++) {
            ItemStack stack = paletteInventory.getStackInSlot(i);
            if (stack != null) {
                cumulative += stack.stackSize;
                if (rand < cumulative) {
                    return i;
                }
            }
        }

        return min; // Fallback
    }
}
