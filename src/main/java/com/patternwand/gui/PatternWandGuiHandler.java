package com.patternwand.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.patternwand.items.IPatternWandItem;

import cpw.mods.fml.common.network.IGuiHandler;

/**
 * GUI handler for the pattern wand.
 */
public class PatternWandGuiHandler implements IGuiHandler {

    public static final int GUI_ID = 0;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_ID) {
            ItemStack heldItem = player.getHeldItem();
            if (heldItem != null && heldItem.getItem() instanceof IPatternWandItem) {
                return new ContainerPatternWand(player.inventory, heldItem);
            }
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_ID) {
            ItemStack heldItem = player.getHeldItem();
            if (heldItem != null && heldItem.getItem() instanceof IPatternWandItem) {
                return new GuiPatternWand(player.inventory, heldItem);
            }
        }
        return null;
    }
}
