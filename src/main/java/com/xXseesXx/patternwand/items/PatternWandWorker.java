package com.xXseesXx.patternwand.items;

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

import com.xXseesXx.patternwand.Config;
import com.xXseesXx.patternwand.PatternWandMod;
import com.xXseesXx.patternwand.palette.BlockMatcher;
import com.xXseesXx.patternwand.palette.PaletteEntry;
import com.xXseesXx.patternwand.palette.PatternPalette;
import com.xXseesXx.patternwand.patterns.scripted.CompiledScript;
import com.xXseesXx.patternwand.patterns.scripted.PlacementContext;
import com.xXseesXx.patternwand.patterns.scripted.ScriptExecutionException;

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

    /**
     * Create a PatternWandWorker with a custom BlockMatcher.
     * 
     * @deprecated Use constructor without matcher parameter instead
     */
    @Deprecated
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
     * Create a PatternWandWorker that uses the active pattern's metadata settings for matching.
     */
    public PatternWandWorker(IWand wand, IPlayerShim playerShim, IWorldShim worldShim, PatternPalette palette,
        ItemStack wandItem, Point3d originPos) {
        super(wand, playerShim, worldShim);
        this.palette = palette;
        this.wandItem = wandItem;
        this.originPos = originPos;
        this.worldShim = worldShim; // Store reference

        // Determine whether to ignore metadata based on active pattern's metadata
        boolean ignoreMetadata = false;
        String activePattern = getActivePattern(wandItem);
        if (activePattern != null && !activePattern.isEmpty()) {
            CompiledScript script = PatternWandMod.proxy.getScriptLoader()
                .getScript(activePattern);
            if (script != null && script.metadata != null) {
                ignoreMetadata = script.metadata.shouldIgnoreMetadata();
            }
        }

        this.matcher = new BlockMatcher(palette, ignoreMetadata);
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
     * Generate a placement plan by executing the pattern for all positions.
     * This separates Lua execution from material consumption and world modification.
     * 
     * @param blocks      List of positions to evaluate
     * @param patternName Name of the pattern script to execute
     * @param itemStack   The wand item (for seed and parameters)
     * @param clickedPos  The position that was clicked
     * @param playerShim  Player shim for context
     * @param side        The face that was clicked
     * @return PlacementPlan containing all block placements
     * @throws ScriptExecutionException if pattern execution fails
     */
    private com.xXseesXx.patternwand.patterns.PlacementPlan generatePlan(LinkedList<Point3d> blocks, String patternName,
        ItemStack itemStack, Point3d clickedPos, IPlayerShim playerShim, int side) throws ScriptExecutionException {

        com.xXseesXx.patternwand.patterns.PlacementPlan plan = new com.xXseesXx.patternwand.patterns.PlacementPlan();

        // Get compiled script
        CompiledScript script = PatternWandMod.proxy.getScriptLoader()
            .getScript(patternName);
        if (script == null) {
            throw new ScriptExecutionException(patternName, "Pattern not found");
        }

        // Convert palette to inventory for script API
        IInventory paletteInventory = paletteToInventory(palette);

        // Get seed and parameters
        long seed = getPatternSeed(itemStack);
        java.util.Map<String, Object> parameterValues = extractParameters(itemStack, script);

        // Create placement context
        PlacementContext context = createPlacementContext(clickedPos, blocks, playerShim.getPlayer(), side);

        // Get palette entries for quick lookup
        List<PaletteEntry> paletteEntries = palette.getEntries();

        // Execute pattern for each position and record results
        for (Point3d pos : blocks) {
            // Calculate relative coordinates from origin
            int relX = pos.x - originPos.x;
            int relY = pos.y - originPos.y;
            int relZ = pos.z - originPos.z;

            // Execute pattern to get palette index
            int paletteIndex = PatternWandMod.proxy.getScriptLoader()
                .getEngine()
                .executePattern(
                    script,
                    pos.x,
                    pos.y,
                    pos.z,
                    relX,
                    relY,
                    relZ,
                    paletteInventory,
                    seed,
                    parameterValues,
                    context);

            // -1 means gap (skip this position)
            if (paletteIndex == -1) {
                continue;
            }

            // Get block from palette and add to plan
            if (paletteIndex >= 0 && paletteIndex < paletteEntries.size()) {
                PaletteEntry entry = paletteEntries.get(paletteIndex);
                if (entry != null && entry.block != null) {
                    plan.addPlacement(pos, entry.block, entry.meta);
                }
            }
        }

        return plan;
    }

    /**
     * Place blocks using a scripted pattern with batched execution.
     * 
     * Execution flow (5 phases):
     * 1. Generate placement plan (all Lua execution isolated)
     * 2. Aggregate material requirements
     * 3. Validate player has sufficient materials
     * 4. Consume materials in batch
     * 5. Execute plan (place blocks in world)
     * 
     * This approach provides:
     * - Better performance (batch inventory operations)
     * - No partial builds (validate before consuming)
     * - Better error messages (report exactly what's missing)
     * - Foundation for future features (undo, preview, AE2)
     */
    private ArrayList<Point3d> placeBlocksWithPattern(ItemStack itemStack, LinkedList<Point3d> blocks,
        Point3d clickedPos, ItemStack sourceItems, IPlayerShim playerShim, int side, float hitX, float hitY, float hitZ,
        String patternName) {

        ArrayList<Point3d> placedBlocks = new ArrayList<Point3d>();

        // Start timing for debug mode - pass player for chat messages
        com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.startPatternTiming(playerShim.getPlayer());

        try {
            // === PHASE 1: Generate Placement Plan (All Lua Execution) ===
            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.startPhase1();

            com.xXseesXx.patternwand.patterns.PlacementPlan plan = generatePlan(
                blocks,
                patternName,
                itemStack,
                clickedPos,
                playerShim,
                side);

            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.endPhase1(plan.size());

            if (plan.isEmpty()) {
                PatternWandMod.LOG.debug("Pattern generated no placements (all gaps or invalid indices)");
                return placedBlocks;
            }

            PatternWandMod.LOG.debug("Generated plan with " + plan.size() + " placements");

            // === PHASE 2: Aggregate Material Requirements ===
            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.startPhase2();

            java.util.Map<String, com.xXseesXx.patternwand.patterns.PlacementPlan.MaterialRequirement> requirements = plan
                .getMaterialRequirements();

            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.endPhase2();

            PatternWandMod.LOG.debug("Plan requires " + requirements.size() + " distinct material types");

            // === PHASE 3: Validate Materials Available ===
            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.startPhase3();

            java.util.List<com.xXseesXx.patternwand.patterns.PlacementPlan.MaterialRequirement> missingMaterials = new ArrayList<com.xXseesXx.patternwand.patterns.PlacementPlan.MaterialRequirement>();

            for (com.xXseesXx.patternwand.patterns.PlacementPlan.MaterialRequirement req : requirements.values()) {
                ItemStack checkStack = req.toSingleItemStack();
                int available = playerShim.countItems(checkStack, false);

                if (available < req.quantity) {
                    missingMaterials.add(req);
                }
            }

            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.endPhase3();

            if (!missingMaterials.isEmpty()) {
                // Report missing materials and abort (Phase 4 handles reporting)
                reportMissingMaterials(missingMaterials, playerShim);
                return placedBlocks;
            }

            // === PHASE 4: Material Consumption ===
            // Note: We DON'T manually consume here - the parent's placeBlocks() handles consumption
            // We validated materials are available in Phase 3, so placements will succeed
            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.startPhase4();
            // No actual work in Phase 4 with current architecture
            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.endPhase4();

            // === PHASE 5: Execute Plan (Place Blocks in World) ===
            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.startPhase5();

            for (com.xXseesXx.patternwand.patterns.PlacementPlan.PlacementEntry entry : plan.getPlacements()) {
                ItemStack blockStack = new ItemStack(entry.block, 1, entry.metadata);

                // Use parent's single-block placement logic
                LinkedList<Point3d> singlePos = new LinkedList<Point3d>();
                singlePos.add(entry.position);

                ArrayList<Point3d> placed = super.placeBlocks(
                    itemStack,
                    singlePos,
                    entry.position,
                    blockStack,
                    playerShim,
                    side,
                    hitX,
                    hitY,
                    hitZ);

                if (!placed.isEmpty()) {
                    placedBlocks.add(entry.position);
                }
            }

            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.endPhase5(placedBlocks.size());

            // Report success
            if (placedBlocks.size() < plan.size()) {
                // Some blocks couldn't be placed (world constraints)
                playerShim.getPlayer()
                    .addChatMessage(
                        new net.minecraft.util.ChatComponentText(
                            String.format("§ePlaced %d of %d blocks", placedBlocks.size(), plan.size())));
            }

        } catch (ScriptExecutionException e) {
            PatternWandMod.LOG.error("Pattern execution failed", e);
            playerShim.getPlayer()
                .addChatMessage(
                    new net.minecraft.util.ChatComponentText("§cPattern execution failed: " + e.getMessage()));
        } catch (Exception e) {
            PatternWandMod.LOG.error("Unexpected error during batched placement", e);
            playerShim.getPlayer()
                .addChatMessage(new net.minecraft.util.ChatComponentText("§cUnexpected error: " + e.getMessage()));
        } finally {
            // Finish timing and print summary
            com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI.finishPatternTiming();
        }

        return placedBlocks;
    }

    /**
     * Report missing materials to the player with detailed information.
     * Shows up to 5 missing materials with available vs required counts.
     */
    private void reportMissingMaterials(
        java.util.List<com.xXseesXx.patternwand.patterns.PlacementPlan.MaterialRequirement> missingMaterials,
        IPlayerShim playerShim) {

        playerShim.getPlayer()
            .addChatMessage(new net.minecraft.util.ChatComponentText("§cInsufficient materials!"));

        int shown = 0;
        for (com.xXseesXx.patternwand.patterns.PlacementPlan.MaterialRequirement req : missingMaterials) {
            if (shown >= 5) {
                int remaining = missingMaterials.size() - shown;
                playerShim.getPlayer()
                    .addChatMessage(
                        new net.minecraft.util.ChatComponentText(
                            String.format("§7... and %d more material type%s", remaining, remaining == 1 ? "" : "s")));
                break;
            }

            ItemStack stack = req.toSingleItemStack();
            int available = playerShim.countItems(stack, false);
            int needed = req.quantity - available;

            playerShim.getPlayer()
                .addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        String.format(
                            "§7- Need %d more %s §7(have %d, need %d)",
                            needed,
                            stack.getDisplayName(),
                            available,
                            req.quantity)));
            shown++;
        }
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
     * Uses custom seed from NBT if set, otherwise uses configured default seed.
     * This ensures patterns are consistent across worlds unless a custom seed is set.
     */
    private long getPatternSeed(ItemStack wand) {
        // Check for custom seed in NBT
        if (wand != null && wand.hasTagCompound()) {
            NBTTagCompound tag = wand.getTagCompound();
            if (tag.hasKey("patternSeed")) {
                return tag.getLong("patternSeed");
            }
        }

        // Fall back to configured default seed
        // This ensures consistent patterns across worlds
        return Config.defaultPatternSeed;
    }

    /**
     * Convert PatternPalette to IInventory for script API.
     */
    private IInventory paletteToInventory(PatternPalette palette) {
        IInventory inventory = new InventoryBasic("Pattern Palette", false, 54);

        List<PaletteEntry> entries = palette.getEntries();
        for (int i = 0; i < Math.min(54, entries.size()); i++) {
            PaletteEntry entry = entries.get(i);
            if (entry != null && entry.block != null) {
                // Use stack size 64 for normal blocks (could be customized)
                inventory.setInventorySlotContents(i, new ItemStack(entry.block, 64, entry.meta));
            }
        }

        return inventory;
    }

    /**
     * Extract parameter values from wand NBT.
     */
    private java.util.Map<String, Object> extractParameters(ItemStack wand, CompiledScript script) {
        java.util.Map<String, Object> paramValues = new java.util.HashMap<String, Object>();

        // Start with default values
        if (script.metadata != null) {
            paramValues = script.metadata.createDefaultValues();
        }

        // Override with stored values from NBT
        if (wand != null && wand.hasTagCompound()) {
            NBTTagCompound tag = wand.getTagCompound();
            if (tag.hasKey("patternParams")) {
                NBTTagCompound paramsTag = tag.getCompoundTag("patternParams");

                // Extract each parameter
                for (Object key : paramsTag.func_150296_c()) { // getKeySet() in 1.7.10
                    String paramName = (String) key;

                    // Check parameter type from metadata
                    if (script.metadata != null) {
                        com.xXseesXx.patternwand.patterns.scripted.PatternParameter param = script.metadata
                            .getParameter(paramName);
                        if (param != null) {
                            switch (param.getType()) {
                                case INTEGER:
                                    paramValues.put(paramName, paramsTag.getInteger(paramName));
                                    break;
                                case FLOAT:
                                    paramValues.put(paramName, paramsTag.getDouble(paramName));
                                    break;
                                case BOOLEAN:
                                    paramValues.put(paramName, paramsTag.getBoolean(paramName));
                                    break;
                                case STRING:
                                    paramValues.put(paramName, paramsTag.getString(paramName));
                                    break;
                            }
                        }
                    }
                }
            }
        }

        return paramValues;
    }

    /**
     * Create placement context for pattern execution.
     */
    private PlacementContext createPlacementContext(Point3d clickedPos, LinkedList<Point3d> blocks,
        net.minecraft.entity.player.EntityPlayer player, int side) {

        // Calculate bounding box
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (Point3d pos : blocks) {
            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            minZ = Math.min(minZ, pos.z);
            maxX = Math.max(maxX, pos.x);
            maxY = Math.max(maxY, pos.y);
            maxZ = Math.max(maxZ, pos.z);
        }

        // Get player orientation
        float playerYaw = player.rotationYaw;
        float playerPitch = player.rotationPitch;

        // Get world time
        long worldTime = worldShim.getWorld()
            .getTotalWorldTime();
        long dayTime = worldShim.getWorld()
            .getWorldTime();

        return new PlacementContext(
            clickedPos.x,
            clickedPos.y,
            clickedPos.z,
            side,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            playerYaw,
            playerPitch,
            worldTime,
            dayTime);
    }
}
