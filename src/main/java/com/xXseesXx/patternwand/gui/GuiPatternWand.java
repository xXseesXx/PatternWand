package com.xXseesXx.patternwand.gui;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.xXseesXx.patternwand.integration.nei.INEIBookmarkGroupReceiver;
import com.xXseesXx.patternwand.items.IPatternWandItem;

/**
 * GUI screen for the pattern wand. Displays pattern information and palette with ghost items.
 */
public class GuiPatternWand extends GuiContainer implements INEIBookmarkGroupReceiver {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        "patternwand",
        "textures/gui/pattern_wand.png");

    private final ContainerPatternWand container;
    private final ItemStack wandStack;
    private final IPatternWandItem wandItem;

    public GuiPatternWand(InventoryPlayer playerInventory, ItemStack wandStack) {
        super(new ContainerPatternWand(playerInventory, wandStack));
        this.container = (ContainerPatternWand) inventorySlots;
        this.wandStack = wandStack;
        this.wandItem = (IPatternWandItem) wandStack.getItem();

        this.xSize = 176;
        this.ySize = 222; // 6 rows of palette (108px) + player inventory (114px)
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Draw title
        String title = "Pattern Wand";
        fontRendererObj.drawString(title, xSize / 2 - fontRendererObj.getStringWidth(title) / 2, 6, 0x404040);

        // Draw inventory label (below the 6-row palette)
        fontRendererObj.drawString(I18n.format("container.inventory"), 8, 128, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);

        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        // Draw the full GUI texture (palette + inventory)
        this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean handleBookmarkGroupDrop(int mouseX, int mouseY, List<ItemStack> stacks) {
        // Find the slot under the mouse cursor
        Slot targetSlot = findSlotAt(mouseX, mouseY);

        if (targetSlot == null) {
            return false;
        }

        // Only handle drops on palette slots (first 54 slots in the container)
        int slotIndex = targetSlot.slotNumber;
        if (slotIndex < 0 || slotIndex >= 54) {
            return false;
        }

        // Fill consecutive palette slots starting from the target slot
        int paletteSlotIndex = slotIndex;
        for (ItemStack stack : stacks) {
            if (paletteSlotIndex >= 54) {
                break; // No more palette slots available
            }

            // Only place block items (skip tools, food, etc.)
            net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromItem(stack.getItem());
            if (block != null && block != net.minecraft.init.Blocks.air) {
                // Create a ghost item with stack size 1
                ItemStack ghostStack = stack.copy();
                ghostStack.stackSize = 1;

                // Set the palette slot contents
                container.getPaletteInventory()
                    .setInventorySlotContents(paletteSlotIndex, ghostStack);
            }

            paletteSlotIndex++;
        }

        return true;
    }

    /**
     * Find the slot at the given GUI-relative coordinates.
     */
    private Slot findSlotAt(int x, int y) {
        for (Object obj : this.inventorySlots.inventorySlots) {
            Slot slot = (Slot) obj;

            // Check if the coordinates are within this slot's bounds
            if (x >= slot.xDisplayPosition - 1 && x < slot.xDisplayPosition + 16 + 1
                && y >= slot.yDisplayPosition - 1
                && y < slot.yDisplayPosition + 16 + 1) {
                return slot;
            }
        }

        return null;
    }
}
