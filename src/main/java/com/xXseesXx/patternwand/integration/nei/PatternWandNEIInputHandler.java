package com.xXseesXx.patternwand.integration.nei;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import codechicken.nei.ItemPanels;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.guihook.IContainerInputHandler;

/**
 * NEI input handler that allows dragging bookmark groups into PatternWand GUIs.
 * Based on AE2-Unofficial's storage bus implementation (PR #1487).
 */
public class PatternWandNEIInputHandler implements IContainerInputHandler {

    private List<ItemStack> draggedBookmarkGroup;
    private static Field guiLeftField;
    private static Field guiTopField;

    static {
        try {
            guiLeftField = GuiContainer.class.getDeclaredField("field_147003_i"); // guiLeft
            guiLeftField.setAccessible(true);
            guiTopField = GuiContainer.class.getDeclaredField("field_147009_r"); // guiTop
            guiTopField.setAccessible(true);
        } catch (Exception e) {
            try {
                guiLeftField = GuiContainer.class.getDeclaredField("guiLeft");
                guiLeftField.setAccessible(true);
                guiTopField = GuiContainer.class.getDeclaredField("guiTop");
                guiTopField.setAccessible(true);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to find GuiContainer fields", ex);
            }
        }
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}

    @Override
    public boolean mouseClicked(GuiContainer gui, int mouseX, int mouseY, int button) {
        return false;
    }

    @Override
    public void onMouseClicked(GuiContainer gui, int mouseX, int mouseY, int button) {}

    @Override
    public void onMouseUp(GuiContainer gui, int mouseX, int mouseY, int button) {
        // Only handle left mouse button release
        if (button != 0) {
            return;
        }

        // If we have a dragged bookmark group and the GUI can receive it
        if (gui instanceof INEIBookmarkGroupReceiver && this.draggedBookmarkGroup != null) {
            INEIBookmarkGroupReceiver receiver = (INEIBookmarkGroupReceiver) gui;

            try {
                // Get GUI position using reflection
                int guiLeft = guiLeftField.getInt(gui);
                int guiTop = guiTopField.getInt(gui);

                receiver.handleBookmarkGroupDrop(mouseX - guiLeft, mouseY - guiTop, this.draggedBookmarkGroup);
            } catch (Exception e) {
                // Failed to access GUI position, skip
            }
        }

        // Always clear the dragged group on mouse release
        this.draggedBookmarkGroup = null;
    }

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mouseX, int mouseY, int scrolled) {
        return false;
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int mouseX, int mouseY, int scrolled) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int mouseX, int mouseY, int button, long heldTime) {
        // Get the currently dragged NEI bookmark group
        if (ItemPanels.bookmarkPanel == null || ItemPanels.bookmarkPanel.sortableGroup == null) {
            return;
        }

        // Capture the bookmark group only once per drag operation
        if (gui instanceof INEIBookmarkGroupReceiver && this.draggedBookmarkGroup == null) {
            this.draggedBookmarkGroup = ItemPanels.bookmarkPanel.sortableGroup.getBookmarkItems()
                .stream()
                .map(BookmarkItem::getItemStack)
                .collect(Collectors.toList());
        }
    }
}
