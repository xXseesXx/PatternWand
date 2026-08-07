package com.xXseesXx.patternwand.client;

import java.util.LinkedList;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.xXseesXx.patternwand.items.IPatternWandItem;
import com.xXseesXx.patternwand.items.ItemPatternWand;
import com.xXseesXx.patternwand.items.PatternWandWorker;
import com.xXseesXx.patternwand.palette.BlockMatcher;
import com.xXseesXx.patternwand.palette.PatternPalette;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.compat.ztones.Ztones;
import portablejim.bbw.core.wands.IWand;
import portablejim.bbw.shims.BasicPlayerShim;
import portablejim.bbw.shims.BasicWorldShim;
import portablejim.bbw.shims.CreativePlayerShim;
import portablejim.bbw.shims.IPlayerShim;
import portablejim.bbw.shims.IWorldShim;

/**
 * Client-side event handler for Pattern Wand block highlighting.
 * This handler runs at HIGH priority to override the default BetterBuildersWands highlighting
 * with our custom palette-based flood matching logic.
 */
@SideOnly(Side.CLIENT)
public class PatternWandBlockEvents {

    /**
     * Handle block highlighting for Pattern Wands using palette-based matching.
     * This runs at HIGH priority and cancels the event to prevent the default
     * BetterBuildersWands handler from running.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        // Only handle if the player is holding a Pattern Wand
        if (event.currentItem == null || !(event.currentItem.getItem() instanceof IPatternWandItem)) {
            return;
        }

        if (event.target == null
            || event.target.typeOfHit != net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        // We're handling a Pattern Wand - cancel the event so the default handler doesn't run
        event.setCanceled(true);

        // Get the Pattern Wand item
        ItemPatternWand wandItem = (ItemPatternWand) event.currentItem.getItem();
        IWand wand = wandItem.getWand();

        // Set up shims for the player and world
        IPlayerShim playerShim = event.player.capabilities.isCreativeMode ? new CreativePlayerShim(event.player)
            : new BasicPlayerShim(event.player);
        IWorldShim worldShim = new BasicWorldShim(event.player.getEntityWorld());

        Point3d clickedPos = new Point3d(event.target.blockX, event.target.blockY, event.target.blockZ);

        // Get the palette and create matcher
        PatternPalette palette = wandItem.getPalette(event.currentItem);
        BlockMatcher matcher = new BlockMatcher(palette);

        // Create Pattern Wand Worker with palette-based matching
        PatternWandWorker worker = new PatternWandWorker(
            wand,
            playerShim,
            worldShim,
            palette,
            matcher,
            event.currentItem,
            clickedPos);

        // Get the proper item stack using palette matching
        ItemStack sourceItems = worker.getProperItemStack(worldShim, playerShim, clickedPos);

        if (sourceItems != null && sourceItems.getItem() instanceof ItemBlock) {
            // Calculate how many blocks can be placed
            int numBlocks;

            // Special handling for Ztones Ofanix (if that mod is loaded)
            if (Ztones.isLoaded() && sourceItems.getItem() == Item.getItemFromBlock(Blocks.cobblestone)
                && playerShim.getPlayer().inventory.hasItem(Ztones.getOfanix())) {
                numBlocks = wand.getMaxBlocks(event.currentItem);
            } else {
                numBlocks = Math.min(wand.getMaxBlocks(event.currentItem), playerShim.countItems(sourceItems, false));
            }

            // Get the list of blocks that would be placed using palette-based flood fill
            LinkedList<Point3d> blocks = worker.getBlockPositionList(
                clickedPos,
                ForgeDirection.getOrientation(event.target.sideHit),
                numBlocks,
                wandItem.getMode(event.currentItem),
                wandItem.getFaceLock(event.currentItem),
                wandItem.getFluidMode(event.currentItem),
                false); // isNBTSensitive

            // Render the highlight boxes
            if (!blocks.isEmpty()) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDepthMask(true);
                GL11.glLineWidth(2.5F);

                // Calculate player interpolation for smooth rendering
                double interpolatedX = event.player.lastTickPosX
                    + (event.player.posX - event.player.lastTickPosX) * event.partialTicks;
                double interpolatedY = event.player.lastTickPosY
                    + (event.player.posY - event.player.lastTickPosY) * event.partialTicks;
                double interpolatedZ = event.player.lastTickPosZ
                    + (event.player.posZ - event.player.lastTickPosZ) * event.partialTicks;

                // Draw highlight box for each block position
                for (Point3d block : blocks) {
                    AxisAlignedBB boundingBox = AxisAlignedBB
                        .getBoundingBox(block.x, block.y, block.z, block.x + 1, block.y + 1, block.z + 1)
                        .contract(0.005, 0.005, 0.005);

                    // Use a different color than default (0x40A040 = greenish) to distinguish Pattern Wand
                    RenderGlobal.drawOutlinedBoundingBox(
                        boundingBox.getOffsetBoundingBox(-interpolatedX, -interpolatedY, -interpolatedZ),
                        0x40A040); // Green tint to show it's using palette matching
                }

                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
            }
        }
    }
}
