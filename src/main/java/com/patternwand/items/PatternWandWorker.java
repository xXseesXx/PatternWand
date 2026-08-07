package com.patternwand.items;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidBlock;

import com.patternwand.PatternWandMod;
import com.patternwand.palette.BlockMatcher;
import com.patternwand.palette.PaletteEntry;
import com.patternwand.palette.PatternPalette;
import com.patternwand.patterns.scripted.CompiledScript;
import com.patternwand.patterns.scripted.ScriptExecutionException;

import portablejim.bbw.basics.EnumFluidLock;
import portablejim.bbw.basics.EnumLock;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.core.WandWorker;
import portablejim.bbw.core.wands.IWand;
import portablejim.bbw.shims.IPlayerShim;
import portablejim.bbw.shims.IWorldShim;

/**
 * Extended WandWorker that uses palette-based block matching and scripted patterns.
 */
public class PatternWandWorker extends WandWorker {

    private final PatternPalette palette;
    private final BlockMatcher matcher;
    private final ItemStack wandItem;
    private final Point3d originPos;
    private final IWorldShim worldShim; // Store our own reference since parent's is private
    private final java.util.HashSet<Point3d> paletteAllCandidates = new java.util.HashSet<>(); // Track visited
                                                                                               // positions

    public PatternWandWorker(IWand wand, IPlayerShim playerShim, IWorldShim worldShim, PatternPalette palette,
        BlockMatcher matcher, ItemStack wandItem, Point3d originPos) {
        super(wand, playerShim, worldShim);
        this.palette = palette;
        this.matcher = matcher;
        this.wandItem = wandItem;
        this.originPos = originPos;
        this.worldShim = worldShim; // Store reference
    }

    /**
     * Override getProperItemStack to use palette matching.
     */
    @Override
    public ItemStack getProperItemStack(IWorldShim worldShim, IPlayerShim playerShim, Point3d clickedPos) {
        Block worldBlock = worldShim.getBlock(clickedPos);
        int worldMeta = worldShim.getMetadata(clickedPos);

        // Check if clicked block is in palette
        if (matcher.matches(worldBlock, worldMeta)) {
            return new ItemStack(worldBlock, 1, worldMeta);
        }

        return null;
    }

    /**
     * Get block positions using palette-based matching instead of exact block matching.
     * This overrides the parent's exact-match logic to match any block in the palette.
     */
    @Override
    public LinkedList<Point3d> getBlockPositionList(Point3d blockLookedAt, ForgeDirection placeDirection, int maxBlocks,
        EnumLock directionLock, EnumLock faceLock, EnumFluidLock fluidLock, boolean isNBTSensitive) {

        LinkedList<Point3d> candidates = new LinkedList<>();
        LinkedList<Point3d> toPlace = new LinkedList<>();

        Block targetBlock = worldShim.getBlock(blockLookedAt);
        int targetMetadata = worldShim.getMetadata(blockLookedAt);
        Point3d startingPoint = blockLookedAt.move(placeDirection);

        int directionMaskInt = directionLock.mask;
        int faceMaskInt = faceLock.mask;

        if (((directionLock != EnumLock.HORIZONTAL && directionLock != EnumLock.VERTICAL)
            || (placeDirection != ForgeDirection.UP && placeDirection != ForgeDirection.DOWN))
            && (directionLock != EnumLock.NORTHSOUTH
                || (placeDirection != ForgeDirection.NORTH && placeDirection != ForgeDirection.SOUTH))
            && (directionLock != EnumLock.EASTWEST
                || (placeDirection != ForgeDirection.EAST && placeDirection != ForgeDirection.WEST))) {
            candidates.add(startingPoint);
        }

        AxisAlignedBB blockBB = targetBlock
            .getCollisionBoundingBoxFromPool(worldShim.getWorld(), blockLookedAt.x, blockLookedAt.y, blockLookedAt.z);

        while (!candidates.isEmpty() && toPlace.size() < maxBlocks) {
            Point3d currentCandidate = candidates.removeFirst();

            Point3d supportingPoint = currentCandidate.move(placeDirection.getOpposite());
            Block candidateSupportingBlock = worldShim.getBlock(supportingPoint);
            int candidateSupportingMeta = worldShim.getMetadata(supportingPoint);

            AxisAlignedBB candidateBB = blockBB;
            if (candidateBB != null) {
                candidateBB = candidateBB.copy()
                    .offset(
                        currentCandidate.x - blockLookedAt.x,
                        currentCandidate.y - blockLookedAt.y,
                        currentCandidate.z - blockLookedAt.z);
            }

            // Modified shouldContinue logic using palette matching
            if (shouldContinueWithPalette(
                currentCandidate,
                targetBlock,
                targetMetadata,
                candidateSupportingBlock,
                candidateSupportingMeta,
                candidateBB,
                fluidLock) && paletteAllCandidates.add(currentCandidate)) {
                toPlace.add(currentCandidate);

                switch (placeDirection) {
                    case DOWN:
                    case UP:
                        if ((faceMaskInt & EnumLock.UP_DOWN_MASK) > 0) {
                            if ((directionMaskInt & EnumLock.NORTH_SOUTH_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.NORTH));
                            if ((directionMaskInt & EnumLock.EAST_WEST_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.EAST));
                            if ((directionMaskInt & EnumLock.NORTH_SOUTH_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.SOUTH));
                            if ((directionMaskInt & EnumLock.EAST_WEST_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.WEST));
                            if ((directionMaskInt & EnumLock.NORTH_SOUTH_MASK) > 0
                                && (directionMaskInt & EnumLock.EAST_WEST_MASK) > 0) {
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.NORTH)
                                        .move(ForgeDirection.EAST));
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.NORTH)
                                        .move(ForgeDirection.WEST));
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.SOUTH)
                                        .move(ForgeDirection.EAST));
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.SOUTH)
                                        .move(ForgeDirection.WEST));
                            }
                        }
                        break;
                    case NORTH:
                    case SOUTH:
                        if ((faceMaskInt & EnumLock.NORTH_SOUTH_MASK) > 0) {
                            if ((directionMaskInt & EnumLock.UP_DOWN_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.UP));
                            if ((directionMaskInt & EnumLock.EAST_WEST_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.EAST));
                            if ((directionMaskInt & EnumLock.UP_DOWN_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.DOWN));
                            if ((directionMaskInt & EnumLock.EAST_WEST_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.WEST));
                            if ((directionMaskInt & EnumLock.UP_DOWN_MASK) > 0
                                && (directionMaskInt & EnumLock.EAST_WEST_MASK) > 0) {
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.UP)
                                        .move(ForgeDirection.EAST));
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.UP)
                                        .move(ForgeDirection.WEST));
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.DOWN)
                                        .move(ForgeDirection.EAST));
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.DOWN)
                                        .move(ForgeDirection.WEST));
                            }
                        }
                        break;
                    case WEST:
                    case EAST:
                        if ((faceMaskInt & EnumLock.EAST_WEST_MASK) > 0) {
                            if ((directionMaskInt & EnumLock.UP_DOWN_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.UP));
                            if ((directionMaskInt & EnumLock.NORTH_SOUTH_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.NORTH));
                            if ((directionMaskInt & EnumLock.UP_DOWN_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.DOWN));
                            if ((directionMaskInt & EnumLock.NORTH_SOUTH_MASK) > 0)
                                candidates.add(currentCandidate.move(ForgeDirection.SOUTH));
                            if ((directionMaskInt & EnumLock.UP_DOWN_MASK) > 0
                                && (directionMaskInt & EnumLock.NORTH_SOUTH_MASK) > 0) {
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.UP)
                                        .move(ForgeDirection.NORTH));
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.UP)
                                        .move(ForgeDirection.SOUTH));
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.DOWN)
                                        .move(ForgeDirection.NORTH));
                                candidates.add(
                                    currentCandidate.move(ForgeDirection.DOWN)
                                        .move(ForgeDirection.SOUTH));
                            }
                        }
                }
            }
        }
        return toPlace;
    }

    /**
     * Check if we should continue flood-filling from this position.
     * Uses PALETTE-BASED matching instead of exact block matching.
     */
    private boolean shouldContinueWithPalette(Point3d currentCandidate, Block targetBlock, int targetMetadata,
        Block candidateSupportingBlock, int candidateSupportingMeta, AxisAlignedBB blockBB, EnumFluidLock fluidLock) {

        // Check if the current candidate position is air or can be replaced
        if (!worldShim.blockIsAir(currentCandidate)) {
            Block currentCandidateBlock = worldShim.getBlock(currentCandidate);
            if (!(fluidLock == EnumFluidLock.IGNORE
                && (currentCandidateBlock instanceof IFluidBlock || currentCandidateBlock instanceof BlockLiquid)))
                return false;
        }

        // Height limit
        if (currentCandidate.y >= 255) return false;

        // KEY CHANGE: Use palette matcher instead of exact block/metadata match
        // The supporting block (adjacent to where we want to place) must be in the palette
        if (!matcher.matches(candidateSupportingBlock, candidateSupportingMeta)) {
            return false;
        }

        // Check if the target block can be placed at this position
        if (!targetBlock
            .canPlaceBlockAt(worldShim.getWorld(), currentCandidate.x, currentCandidate.y, currentCandidate.z))
            return false;

        if (!targetBlock.canBlockStay(worldShim.getWorld(), currentCandidate.x, currentCandidate.y, currentCandidate.z))
            return false;

        if (!targetBlock.canReplace(
            worldShim.getWorld(),
            currentCandidate.x,
            currentCandidate.y,
            currentCandidate.z,
            targetMetadata,
            new ItemStack(candidateSupportingBlock, 1, candidateSupportingMeta))) return false;

        // Check for entity collisions
        return !worldShim.entitiesInBox(blockBB);
    }

    /**
     * Place blocks at the given positions using pattern script if active.
     */
    @Override
    public ArrayList<Point3d> placeBlocks(ItemStack itemStack, LinkedList<Point3d> blocks, Point3d clickedPos,
        ItemStack sourceItems, IPlayerShim playerShim, int side, float hitX, float hitY, float hitZ) {

        // Check if a pattern is active - read from the parameter, not the field
        String activePattern = getActivePattern(itemStack);

        if (activePattern != null && !activePattern.isEmpty()) {
            // Use pattern-based placement
            return placeBlocksWithPattern(
                itemStack,
                blocks,
                clickedPos,
                sourceItems,
                playerShim,
                side,
                hitX,
                hitY,
                hitZ,
                activePattern);
        } else {
            // Use default palette-based placement
            return super.placeBlocks(itemStack, blocks, clickedPos, sourceItems, playerShim, side, hitX, hitY, hitZ);
        }
    }

    /**
     * Place blocks using a scripted pattern.
     */
    private ArrayList<Point3d> placeBlocksWithPattern(ItemStack itemStack, LinkedList<Point3d> blocks,
        Point3d clickedPos, ItemStack sourceItems, IPlayerShim playerShim, int side, float hitX, float hitY, float hitZ,
        String patternName) {

        ArrayList<Point3d> placedBlocks = new ArrayList<>();

        // Get the compiled pattern script from proxy
        CompiledScript script = PatternWandMod.proxy.getScriptLoader()
            .getScript(patternName);
        if (script == null) {
            PatternWandMod.LOG.warn("Pattern script not found: " + patternName);
            return placedBlocks;
        }

        // Convert palette to IInventory for script API
        IInventory paletteInventory = paletteToInventory(palette);

        // Generate seed for pattern
        // Priority: custom seed from NBT > world seed
        long seed = getPatternSeed(itemStack);

        // Get palette entries for quick lookup
        List<PaletteEntry> paletteEntries = palette.getEntries();

        // Place each block according to pattern
        for (Point3d pos : blocks) {
            try {
                // Calculate relative coordinates from origin
                int relX = pos.x - originPos.x;
                int relY = pos.y - originPos.y;
                int relZ = pos.z - originPos.z;

                // Execute pattern to get palette index
                int paletteIndex = PatternWandMod.proxy.getScriptLoader()
                    .getEngine()
                    .executePattern(script, pos.x, pos.y, pos.z, relX, relY, relZ, paletteInventory, seed);

                // -1 means gap (don't place)
                if (paletteIndex == -1) {
                    continue;
                }

                // Get block from palette at this index
                if (paletteIndex >= 0 && paletteIndex < paletteEntries.size()) {
                    PaletteEntry entry = paletteEntries.get(paletteIndex);
                    if (entry != null && entry.block != null) {
                        ItemStack blockToPlace = new ItemStack(entry.block, 1, entry.meta);

                        // Place block at position using PlayerShim
                        int itemsAvailable = playerShim.countItems(blockToPlace, false);
                        if (itemsAvailable > 0) {
                            // Call placeBlocks on a single-item list using parent
                            LinkedList<Point3d> singleBlock = new LinkedList<>();
                            singleBlock.add(pos);
                            ArrayList<Point3d> placed = super.placeBlocks(
                                itemStack,
                                singleBlock,
                                pos,
                                blockToPlace,
                                playerShim,
                                side,
                                hitX,
                                hitY,
                                hitZ);
                            if (!placed.isEmpty()) {
                                placedBlocks.add(pos);
                            }
                        }
                    }
                }
            } catch (ScriptExecutionException e) {
                PatternWandMod.LOG.error("Pattern execution failed at " + pos, e);
                // Continue with other blocks
            }
        }

        return placedBlocks;
    }

    /**
     * Get active pattern name from wand NBT.
     */
    private String getActivePattern(ItemStack wand) {
        if (wand == null || !wand.hasTagCompound()) {
            return null;
        }

        NBTTagCompound tag = wand.getTagCompound();
        if (tag.hasKey("activePattern")) {
            return tag.getString("activePattern");
        }

        return null;
    }

    /**
     * Get pattern seed for deterministic noise generation.
     * Uses custom seed from NBT if set, otherwise uses world seed.
     * This ensures patterns are consistent across the world regardless of click position.
     */
    private long getPatternSeed(ItemStack wand) {
        // Check for custom seed in NBT
        if (wand != null && wand.hasTagCompound()) {
            NBTTagCompound tag = wand.getTagCompound();
            if (tag.hasKey("patternSeed")) {
                return tag.getLong("patternSeed");
            }
        }

        // Fall back to world seed for deterministic patterns
        // This ensures the same coordinates always produce the same noise
        return worldShim.getWorld()
            .getSeed();
    }

    /**
     * Convert PatternPalette to IInventory for script API.
     */
    private IInventory paletteToInventory(PatternPalette palette) {
        IInventory inventory = new InventoryBasic("Pattern Palette", false, 27);

        List<PaletteEntry> entries = palette.getEntries();
        for (int i = 0; i < Math.min(27, entries.size()); i++) {
            PaletteEntry entry = entries.get(i);
            if (entry != null && entry.block != null) {
                // Use stack size 64 for normal blocks (could be customized)
                inventory.setInventorySlotContents(i, new ItemStack(entry.block, 64, entry.meta));
            }
        }

        return inventory;
    }
}
