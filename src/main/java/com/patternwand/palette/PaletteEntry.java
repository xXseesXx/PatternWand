package com.patternwand.palette;

import net.minecraft.block.Block;

/**
 * Represents one entry in a pattern palette.
 */
public class PaletteEntry {

    public final Block block;
    public final int meta;

    public PaletteEntry(Block block, int meta) {
        this.block = block;
        this.meta = meta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaletteEntry that = (PaletteEntry) o;
        return meta == that.meta && block.equals(that.block);
    }

    @Override
    public int hashCode() {
        int result = block.hashCode();
        result = 31 * result + meta;
        return result;
    }

    @Override
    public String toString() {
        return String.format("PaletteEntry{%s:%d}", Block.blockRegistry.getNameForObject(block), meta);
    }
}
