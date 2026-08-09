package com.xXseesXx.patternwand.items;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import portablejim.bbw.core.wands.IWand;

/**
 * Unbreakable Pattern Wand with tiered block limits matching BBW.
 * Tier 11 = 4096, Tier 12 = 8192, Tier 13 = 16384
 */
public class PatternWandUnbreakable implements IWand {

    @Override
    public int getMaxBlocks(ItemStack itemStack) {
        if (itemStack == null) {
            return 16384;
        }

        // Get tier from metadata
        int tier = itemStack.getItemDamage();

        // BBW uses tier^2 for block limits
        // Tier 11: 11^2 * 16 = 1936 (no, that's wrong)
        // Actually for unbreakable: tier * tier gives the limit
        // But we want: tier 11 = 4096, tier 12 = 8192, tier 13 = 16384
        // Pattern: multiply previous tier by 2
        switch (tier) {
            case 11:
                return 4096;
            case 12:
                return 8192;
            case 13:
                return 16384;
            default:
                return 16384; // Default to max
        }
    }

    @Override
    public boolean placeBlock(ItemStack itemStack, EntityLivingBase entityLivingBase) {
        // Unbreakable - always returns true, never damages the item
        return true;
    }
}
