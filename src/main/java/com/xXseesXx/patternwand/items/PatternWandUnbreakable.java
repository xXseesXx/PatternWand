package com.xXseesXx.patternwand.items;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import portablejim.bbw.core.wands.IWand;

/**
 * Unbreakable Pattern Wand with 16384 (2^14) max blocks.
 */
public class PatternWandUnbreakable implements IWand {

    @Override
    public int getMaxBlocks(ItemStack itemStack) {
        return 16384; // 2^14
    }

    @Override
    public boolean placeBlock(ItemStack itemStack, EntityLivingBase entityLivingBase) {
        // Unbreakable - always returns true, never damages the item
        return true;
    }
}
