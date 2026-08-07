package com.xXseesXx.patternwand.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import com.xXseesXx.patternwand.PatternWandMod;
import com.xXseesXx.patternwand.items.IPatternWandItem;
import com.xXseesXx.patternwand.network.PacketSyncPalette;

/**
 * Container for the pattern wand GUI. Handles palette customization and synchronization.
 */
public class ContainerPatternWand extends Container {

    private final ItemStack wandStack;
    private final IPatternWandItem wandItem;
    private final IInventory paletteInventory;
    private final EntityPlayer player;

    public ContainerPatternWand(InventoryPlayer playerInventory, ItemStack wandStack) {
        this.wandStack = wandStack;
        this.wandItem = (IPatternWandItem) wandStack.getItem();
        this.player = playerInventory.player;

        // Create inventory for 27 palette slots (3x9 grid)
        this.paletteInventory = new InventoryBasic("Pattern Palette", false, 27);

        // Load palette from wand NBT (if any)
        loadPaletteFromWand();

        // Add palette slots (3 rows x 9 columns)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                addSlotToContainer(new PaletteSlot(paletteInventory, slotIndex, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory (standard layout, moved down)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 86 + i * 18));
            }
        }

        // Add player hotbar
        for (int i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(playerInventory, i, 8 + i * 18, 144));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public ItemStack slotClick(int slotIndex, int button, int modifier, EntityPlayer player) {
        // Handle palette slot clicks specially (ghost slot behavior)
        if (slotIndex >= 0 && slotIndex < paletteInventory.getSizeInventory()) {
            ItemStack cursorStack = player.inventory.getItemStack();
            ItemStack slotStack = paletteInventory.getStackInSlot(slotIndex);

            // Left click with item: set ghost slot to this item
            if (button == 0 && cursorStack != null && modifier != 1) {
                net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromItem(cursorStack.getItem());
                if (block != null && block != net.minecraft.init.Blocks.air) {
                    ItemStack ghostStack = cursorStack.copy();
                    ghostStack.stackSize = cursorStack.stackSize; // Copy the stacksize from cursor
                    paletteInventory.setInventorySlotContents(slotIndex, ghostStack);
                }
                return cursorStack; // Don't consume cursor item
            }

            // Shift + Left click: increase stacksize
            if (button == 0 && modifier == 1 && slotStack != null) {
                ItemStack newStack = slotStack.copy();
                newStack.stackSize = Math.min(newStack.stackSize + 1, 64);
                paletteInventory.setInventorySlotContents(slotIndex, newStack);
                return cursorStack;
            }

            // Right click with item: set ghost slot with stacksize 1
            if (button == 1 && cursorStack != null) {
                net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromItem(cursorStack.getItem());
                if (block != null && block != net.minecraft.init.Blocks.air) {
                    ItemStack ghostStack = cursorStack.copy();
                    ghostStack.stackSize = 1;
                    paletteInventory.setInventorySlotContents(slotIndex, ghostStack);
                }
                return cursorStack;
            }

            // Right click on slot (no cursor item): decrease stacksize or clear
            if (button == 1 && cursorStack == null && slotStack != null) {
                if (slotStack.stackSize > 1) {
                    ItemStack newStack = slotStack.copy();
                    newStack.stackSize--;
                    paletteInventory.setInventorySlotContents(slotIndex, newStack);
                } else {
                    paletteInventory.setInventorySlotContents(slotIndex, null);
                }
                return null;
            }

            // Middle click: pick up ghost item
            if (button == 2 && slotStack != null && player.capabilities.isCreativeMode) {
                ItemStack pickStack = slotStack.copy();
                pickStack.stackSize = slotStack.getMaxStackSize();
                return pickStack;
            }

            return cursorStack;
        }

        // Normal slot behavior for player inventory
        return super.slotClick(slotIndex, button, modifier, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        // Shift-clicking: copy from player inventory to palette as ghost item
        Slot slot = (Slot) this.inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack()) {
            int paletteSlots = paletteInventory.getSizeInventory();

            // Only handle shift-click FROM player inventory TO palette
            if (slotIndex >= paletteSlots) {
                ItemStack sourceStack = slot.getStack();
                net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromItem(sourceStack.getItem());

                // Only accept block items
                if (block != null && block != net.minecraft.init.Blocks.air) {
                    // Find first empty palette slot
                    for (int i = 0; i < paletteSlots; i++) {
                        if (paletteInventory.getStackInSlot(i) == null) {
                            // Create a copy with stack size 1 as ghost item
                            ItemStack ghostStack = sourceStack.copy();
                            ghostStack.stackSize = 1;
                            paletteInventory.setInventorySlotContents(i, ghostStack);
                            break;
                        }
                    }
                }
            }
        }

        // Never actually consume the source item
        return null;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);

        // Client side: send packet to server with palette data
        if (player.worldObj.isRemote) {
            NBTTagCompound paletteData = new NBTTagCompound();

            // Build palette list
            net.minecraft.nbt.NBTTagList paletteList = new net.minecraft.nbt.NBTTagList();
            for (int i = 0; i < 27; i++) {
                ItemStack stack = paletteInventory.getStackInSlot(i);
                NBTTagCompound slotTag = new NBTTagCompound();

                if (stack != null) {
                    String itemName = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
                    if (itemName != null) {
                        slotTag.setString("id", itemName);
                        slotTag.setShort("Damage", (short) stack.getItemDamage());
                        slotTag.setByte("Count", (byte) stack.stackSize);
                    }
                }

                paletteList.appendTag(slotTag);
            }

            paletteData.setTag("palette", paletteList);

            // Send packet to server
            PatternWandMod.networkWrapper.sendToServer(new PacketSyncPalette(paletteData));
        }
        // Server side: save directly
        else {
            savePaletteToWand();
        }
    }

    /**
     * Load the palette from the wand's NBT into the inventory.
     */
    private void loadPaletteFromWand() {
        if (wandStack != null && wandStack.hasTagCompound()) {
            NBTTagCompound itemNBT = wandStack.getTagCompound();
            if (itemNBT.hasKey("bbw", Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound bbwNBT = itemNBT.getCompoundTag("bbw");
                if (bbwNBT.hasKey("palette", Constants.NBT.TAG_LIST)) {
                    net.minecraft.nbt.NBTTagList paletteList = bbwNBT.getTagList("palette", Constants.NBT.TAG_COMPOUND);
                    for (int i = 0; i < paletteList.tagCount() && i < 27; i++) {
                        NBTTagCompound slotTag = paletteList.getCompoundTagAt(i);
                        if (slotTag.hasKey("id")) {
                            String blockName = slotTag.getString("id");
                            int damage = slotTag.hasKey("Damage") ? slotTag.getShort("Damage") : 0;
                            int count = slotTag.hasKey("Count") ? slotTag.getByte("Count") : 1;

                            // Recreate the item from block name, damage, and count
                            net.minecraft.item.Item item = (net.minecraft.item.Item) net.minecraft.item.Item.itemRegistry
                                .getObject(blockName);
                            if (item != null) {
                                ItemStack stack = new ItemStack(item, count, damage);
                                paletteInventory.setInventorySlotContents(i, stack);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Save the current palette configuration to the wand's NBT. Saves block type, metadata, and stacksize. Only called
     * on server side.
     */
    private void savePaletteToWand() {
        NBTTagCompound itemNBT = wandStack.hasTagCompound() ? wandStack.getTagCompound() : new NBTTagCompound();
        NBTTagCompound bbwNBT = itemNBT.hasKey("bbw", Constants.NBT.TAG_COMPOUND) ? itemNBT.getCompoundTag("bbw")
            : new NBTTagCompound();

        // Save all 27 slots (block ID, metadata, and stacksize)
        net.minecraft.nbt.NBTTagList paletteList = new net.minecraft.nbt.NBTTagList();
        for (int i = 0; i < 27; i++) {
            ItemStack stack = paletteInventory.getStackInSlot(i);
            NBTTagCompound slotTag = new NBTTagCompound();

            if (stack != null) {
                // Save item ID, damage value (metadata), and stacksize
                String itemName = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
                if (itemName != null) {
                    slotTag.setString("id", itemName);
                    slotTag.setShort("Damage", (short) stack.getItemDamage());
                    slotTag.setByte("Count", (byte) stack.stackSize);
                }
            }

            paletteList.appendTag(slotTag);
        }

        bbwNBT.setTag("palette", paletteList);

        // Increment palette version for TOCTOU protection
        short currentVersion = bbwNBT.hasKey("paletteVersion") ? bbwNBT.getShort("paletteVersion") : 0;
        bbwNBT.setShort("paletteVersion", (short) (currentVersion + 1));

        itemNBT.setTag("bbw", bbwNBT);
        wandStack.setTagCompound(itemNBT);
    }

    /**
     * Get the palette inventory for rendering.
     */
    public IInventory getPaletteInventory() {
        return paletteInventory;
    }

    public ItemStack getWandStack() {
        return wandStack;
    }

    /**
     * Ghost slot for palette entries. Displays items but doesn't consume them.
     */
    private static class PaletteSlot extends Slot {

        public PaletteSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            // Ghost slots: can't take items out normally
            return false;
        }

        @Override
        public int getSlotStackLimit() {
            return 64; // Allow any stacksize for display
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            // Ghost slots: don't accept items through normal means
            return false;
        }

        @Override
        public void putStack(ItemStack stack) {
            // Allow any stacksize through inventory.setInventorySlotContents
            super.putStack(stack);
        }
    }
}
