package com.patternwand.items;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.patternwand.Config;

import portablejim.bbw.basics.EnumFluidLock;
import portablejim.bbw.basics.EnumLock;
import portablejim.bbw.core.items.ItemBasicWand;

/**
 * The Pattern Wand item - copies functionality from BetterBuildersWands wands.
 */
public class ItemPatternWand extends ItemBasicWand {

    public ItemPatternWand() {
        super();
        this.setUnlocalizedName("patternwand:patternWand");
        this.setTextureName("patternwand:patternWand");
        this.setCreativeTab(CreativeTabs.tabTools);
        this.setMaxStackSize(1);
        this.setMaxDamage(Config.patternWandDurability);
        this.wand = new PatternWand();
    }

    @Override
    public EnumLock getFaceLock(ItemStack itemStack) {
        if (getMode(itemStack) == EnumLock.HORIZONTAL) {
            return EnumLock.HORIZONTAL;
        }
        return EnumLock.NOLOCK;
    }

    @Override
    public void nextMode(ItemStack itemStack, EntityPlayer player) {
        // Cycle through all lock modes
        switch (getMode(itemStack)) {
            case NORTHSOUTH:
                setMode(itemStack, EnumLock.EASTWEST);
                break;
            case VERTICAL:
                setMode(itemStack, EnumLock.NORTHSOUTH);
                break;
            case VERTICALEASTWEST:
                setMode(itemStack, EnumLock.NOLOCK);
                break;
            case EASTWEST:
                setMode(itemStack, EnumLock.VERTICALNORTHSOUTH);
                break;
            case HORIZONTAL:
                setMode(itemStack, EnumLock.VERTICAL);
                break;
            case VERTICALNORTHSOUTH:
                setMode(itemStack, EnumLock.VERTICALEASTWEST);
                break;
            case NOLOCK:
                setMode(itemStack, EnumLock.HORIZONTAL);
                break;
        }
    }

    @Override
    public void nextFluidMode(ItemStack itemStack, EntityPlayer player) {
        // Cycle through fluid lock modes
        switch (getFluidMode(itemStack)) {
            case STOPAT:
                setFluidMode(itemStack, EnumFluidLock.IGNORE);
                break;
            case IGNORE:
                setFluidMode(itemStack, EnumFluidLock.STOPAT);
                break;
        }
    }

    @Override
    public void getSubItems(Item item, CreativeTabs creativeTabs, java.util.List list) {
        list.add(new ItemStack(item, 1, 0));
    }
}
