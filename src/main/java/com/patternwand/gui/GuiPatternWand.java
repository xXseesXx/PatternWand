package com.patternwand.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.patternwand.items.IPatternWandItem;

/**
 * GUI screen for the pattern wand. Displays pattern information and palette with ghost items.
 */
public class GuiPatternWand extends GuiContainer {

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
        this.ySize = 168; // Increased height for 3 rows of palette
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Draw title
        String title = "Pattern Wand";
        fontRendererObj.drawString(title, xSize / 2 - fontRendererObj.getStringWidth(title) / 2, 6, 0x404040);

        // Draw inventory label
        fontRendererObj.drawString(I18n.format("container.inventory"), 8, 74, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw background
        drawDefaultBackground();

        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        // Draw main background
        drawRect(x, y, x + xSize, y + ySize, 0xFFC6C6C6);

        // Draw palette area background (3 rows x 9 columns)
        drawRect(x + 7, y + 17, x + 169, y + 71, 0xFF8B8B8B);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
