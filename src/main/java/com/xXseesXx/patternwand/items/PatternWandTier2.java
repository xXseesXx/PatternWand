package com.xXseesXx.patternwand.items;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import portablejim.bbw.core.wands.IWand;

/**
 * Tier 2 Pattern Wand with 2048 max blocks.
 */
public class PatternWandTier2 implements IWand {

    @Override
    public int getMaxBlocks(ItemStack itemStack) {
        return 2048;
    }

    @Override
    public boolean placeBlock(ItemStack itemStack, EntityLivingBase entityLivingBase) {
        // Damage the wand when placing blocks
        if (!itemStack.isItemStackDamageable()) {
            return true;
        }

        itemStack.damageItem(1, entityLivingBase);
        return itemStack.getItemDamage() < itemStack.getMaxDamage();
    }
}
