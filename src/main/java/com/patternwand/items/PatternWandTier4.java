package com.patternwand.items;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import portablejim.bbw.core.wands.IWand;

/**
 * Tier 4 Pattern Wand with 8192 max blocks.
 */
public class PatternWandTier4 implements IWand {

    @Override
    public int getMaxBlocks(ItemStack itemStack) {
        return 8192;
    }

    @Override
    public boolean placeBlock(ItemStack itemStack, EntityLivingBase entityLivingBase) {
        if (!itemStack.isItemStackDamageable()) {
            return true;
        }
        itemStack.damageItem(1, entityLivingBase);
        return itemStack.getItemDamage() < itemStack.getMaxDamage();
    }
}
