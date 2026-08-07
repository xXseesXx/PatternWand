package com.xXseesXx.patternwand.palette;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * A palette of blocks for pattern-based placement. Stores block types that should be matched during flood-fill.
 */
public class PatternPalette {

    private final List<PaletteEntry> entries;

    public PatternPalette(List<PaletteEntry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    /**
     * Create empty palette.
     */
    public PatternPalette() {
        this.entries = new ArrayList<>();
    }

    /**
     * Create palette from NBT data.
     */
    public static PatternPalette fromNBT(NBTTagList paletteList) {
        List<PaletteEntry> entries = new ArrayList<>();

        for (int i = 0; i < paletteList.tagCount(); i++) {
            NBTTagCompound entryTag = paletteList.getCompoundTagAt(i);

            // Check if this slot has data
            if (!entryTag.hasKey("id")) {
                continue;
            }

            String blockName = entryTag.getString("id");
            int meta = entryTag.hasKey("Damage") ? entryTag.getShort("Damage") : 0;

            // Convert item name to block
            net.minecraft.item.Item item = (net.minecraft.item.Item) net.minecraft.item.Item.itemRegistry
                .getObject(blockName);
            if (item != null) {
                Block block = Block.getBlockFromItem(item);
                if (block != null && block != Blocks.air) {
                    entries.add(new PaletteEntry(block, meta));
                }
            }
        }

        return new PatternPalette(entries);
    }

    /**
     * Save palette to NBT.
     */
    public NBTTagList toNBT() {
        NBTTagList list = new NBTTagList();

        for (PaletteEntry entry : entries) {
            NBTTagCompound entryTag = new NBTTagCompound();
            entryTag.setString("block", Block.blockRegistry.getNameForObject(entry.block));
            entryTag.setInteger("meta", entry.meta);
            list.appendTag(entryTag);
        }

        return list;
    }

    /**
     * Create palette from inventory (27 slots).
     */
    public static PatternPalette fromInventory(net.minecraft.inventory.IInventory inventory) {
        List<PaletteEntry> entries = new ArrayList<>();

        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null) {
                Block block = Block.getBlockFromItem(stack.getItem());
                if (block != null && block != Blocks.air) {
                    entries.add(new PaletteEntry(block, stack.getItemDamage()));
                }
            }
        }

        return new PatternPalette(entries);
    }

    /**
     * Check if this palette contains the given block/metadata.
     */
    public boolean contains(Block block, int meta) {
        for (PaletteEntry entry : entries) {
            if (entry.block == block && entry.meta == meta) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the number of entries in this palette.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Get all entries.
     */
    public List<PaletteEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    @Override
    public String toString() {
        return String.format("PatternPalette{%d entries}", entries.size());
    }
}
