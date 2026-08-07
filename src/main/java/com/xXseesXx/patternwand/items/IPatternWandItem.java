package com.xXseesXx.patternwand.items;

import net.minecraft.item.ItemStack;

import com.xXseesXx.patternwand.palette.PatternPalette;

/**
 * Interface for pattern-based wands.
 */
public interface IPatternWandItem {

    /**
     * Get the palette for this wand.
     */
    PatternPalette getPalette(ItemStack wand);

    /**
     * Save palette to wand NBT.
     */
    void savePalette(ItemStack wand, PatternPalette palette);
}
