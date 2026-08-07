package com.patternwand.palette;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;

/**
 * Matches blocks based on a palette. Used during flood-fill to determine which blocks should be replaced.
 */
public class BlockMatcher {

    private final Set<BlockKey> matchSet;

    public BlockMatcher(PatternPalette palette) {
        this.matchSet = new HashSet<>();

        // Build match set from palette entries
        for (PaletteEntry entry : palette.getEntries()) {
            matchSet.add(new BlockKey(entry.block, entry.meta));
        }
    }

    /**
     * Check if the given block/metadata matches this palette.
     */
    public boolean matches(Block block, int meta) {
        return matchSet.contains(new BlockKey(block, meta));
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
