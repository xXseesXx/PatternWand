package com.xXseesXx.patternwand.palette;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;

/**
 * Matches blocks based on a palette. Used during flood-fill to determine which blocks should be replaced.
 */
public class BlockMatcher {

    private final Set<BlockKey> matchSet;
    private final boolean ignoreMetadata;

    /**
     * Create a BlockMatcher that matches based on block type only (ignores metadata/rotation).
     * 
     * @param palette        The palette containing blocks to match
     * @param ignoreMetadata If true, only block type is matched (metadata/rotation is ignored).
     *                       If false, both block type and metadata must match exactly.
     */
    public BlockMatcher(PatternPalette palette, boolean ignoreMetadata) {
        this.matchSet = new HashSet<>();
        this.ignoreMetadata = ignoreMetadata;

        // Build match set from palette entries
        for (PaletteEntry entry : palette.getEntries()) {
            if (ignoreMetadata) {
                // Only store block type, ignore metadata
                matchSet.add(new BlockKey(entry.block, 0));
            } else {
                // Store both block type and metadata
                matchSet.add(new BlockKey(entry.block, entry.meta));
            }
        }
    }

    /**
     * Legacy constructor for backward compatibility. Uses metadata-matching mode.
     * 
     * @deprecated Use {@link #BlockMatcher(PatternPalette, boolean)} instead
     */
    @Deprecated
    public BlockMatcher(PatternPalette palette) {
        this(palette, false);
    }

    /**
     * Check if the given block/metadata matches this palette.
     */
    public boolean matches(Block block, int meta) {
        if (ignoreMetadata) {
            // Only check block type, ignore metadata
            return matchSet.contains(new BlockKey(block, 0));
        } else {
            // Check both block type and metadata
            return matchSet.contains(new BlockKey(block, meta));
        }
    }

    /**
     * Simple key for block+metadata matching.
     */
    private static class BlockKey {

        private final Block block;
        private final int meta;

        public BlockKey(Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BlockKey blockKey = (BlockKey) o;
            return meta == blockKey.meta && block.equals(blockKey.block);
        }

        @Override
        public int hashCode() {
            int result = block.hashCode();
            result = 31 * result + meta;
            return result;
        }
    }
}
