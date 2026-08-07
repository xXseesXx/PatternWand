package com.xXseesXx.patternwand.items;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import com.xXseesXx.patternwand.Config;

import portablejim.bbw.core.wands.IWand;

/**
 * Pattern Wand implementation that uses durability and configuration.
 */
public class PatternWand implements IWand {

    @Override
    public int getMaxBlocks(ItemStack itemStack) {
        return Config.patternWandMaxBlocks;
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
