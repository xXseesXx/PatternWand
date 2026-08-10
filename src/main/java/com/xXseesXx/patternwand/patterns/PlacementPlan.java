package com.xXseesXx.patternwand.patterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import portablejim.bbw.basics.Point3d;

/**
 * A plan for placing blocks, generated from pattern execution.
 * Separates pattern generation (Lua) from material consumption (Minecraft).
 * 
 * This enables:
 * - Batch material aggregation and validation
 * - Better error reporting before consumption
 * - Future features: preview, undo, async execution, AE2 integration
 */
public class PlacementPlan {

    /**
     * A single block placement in the plan.
     */
    public static class PlacementEntry {

        public final Point3d position;
        public final Block block;
        public final int metadata;

        public PlacementEntry(Point3d position, Block block, int metadata) {
            this.position = position;
            this.block = block;
            this.metadata = metadata;
        }

        @Override
        public String toString() {
            return String.format(
                "PlacementEntry{pos=(%d,%d,%d), block=%s, meta=%d}",
                position.x,
                position.y,
                position.z,
                block.getUnlocalizedName(),
                metadata);
        }
    }

    /**
     * Aggregated requirement for a specific material.
     */
    public static class MaterialRequirement {

        public final Block block;
        public final int metadata;
        public int quantity = 0;

        public MaterialRequirement(Block block, int metadata) {
            this.block = block;
            this.metadata = metadata;
        }

        /**
         * Create an ItemStack representing this requirement.
         * Note: Stack size is set to quantity for convenience, but this may exceed 64.
         */
        public ItemStack toItemStack() {
            return new ItemStack(block, quantity, metadata);
        }

        /**
         * Create a single-item ItemStack for comparison/matching.
         */
        public ItemStack toSingleItemStack() {
            return new ItemStack(block, 1, metadata);
        }

        @Override
        public String toString() {
            return String.format(
                "MaterialRequirement{block=%s, meta=%d, quantity=%d}",
                block.getUnlocalizedName(),
                metadata,
                quantity);
        }
    }

    private final List<PlacementEntry> placements = new ArrayList<PlacementEntry>();

    /**
     * Add a block placement to this plan.
     */
    public void addPlacement(Point3d pos, Block block, int metadata) {
        placements.add(new PlacementEntry(pos, block, metadata));
    }

    /**
     * Get all placements in this plan.
     */
    public List<PlacementEntry> getPlacements() {
        return placements;
    }

    /**
     * Get the number of blocks to place.
     */
    public int size() {
        return placements.size();
    }

    /**
     * Check if this plan is empty.
     */
    public boolean isEmpty() {
        return placements.isEmpty();
    }

    /**
     * Aggregate material requirements from all placements.
     * 
     * Returns a map of material key → requirement, where each requirement
     * contains the total quantity needed for that block+metadata combination.
     * 
     * For a 10,000 block pattern using 5 different block types, this returns
     * a map with 5 entries instead of iterating materials 10,000 times later.
     */
    public Map<String, MaterialRequirement> getMaterialRequirements() {
        Map<String, MaterialRequirement> requirements = new HashMap<String, MaterialRequirement>();

        for (PlacementEntry entry : placements) {
            // Create unique key for block+metadata combination
            String key = createMaterialKey(entry.block, entry.metadata);

            MaterialRequirement req = requirements.get(key);
            if (req == null) {
                req = new MaterialRequirement(entry.block, entry.metadata);
                requirements.put(key, req);
            }
            req.quantity++;
        }

        return requirements;
    }

    /**
     * Create a unique key for a block+metadata combination.
     * Used for aggregating materials.
     */
    private static String createMaterialKey(Block block, int metadata) {
        // Use block registry name + metadata as key
        // This handles cases where unlocalized name might not be unique
        return Block.blockRegistry.getNameForObject(block) + ":" + metadata;
    }

    @Override
    public String toString() {
        return String.format("PlacementPlan{placements=%d}", placements.size());
    }
}
