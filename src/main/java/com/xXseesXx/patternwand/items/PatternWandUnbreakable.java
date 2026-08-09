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

        // Calculate block limit: tier^2 * 16
        // Tier 11: 11^2 * 16 = 1936 (but we want 4096 = 64^2 = tier 64, pattern: tier 11 -> 2^12)
        // Actually BBW uses: 2^(tier-1) for tier >= 11
        // Tier 11 = 2^10 * 4 = 4096
        // Tier 12 = 2^11 * 4 = 8192
        // Tier 13 = 2^12 * 4 = 16384
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
