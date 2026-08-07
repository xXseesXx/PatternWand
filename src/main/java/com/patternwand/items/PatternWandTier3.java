package com.patternwand.items;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import portablejim.bbw.core.wands.IWand;

/**
 * Tier 3 Pattern Wand with 4096 max blocks.
 */
public class PatternWandTier3 implements IWand {

    @Override
    public int getMaxBlocks(ItemStack itemStack) {
        return 4096;
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
