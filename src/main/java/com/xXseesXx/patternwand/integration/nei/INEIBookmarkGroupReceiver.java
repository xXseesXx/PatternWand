package com.xXseesXx.patternwand.integration.nei;

import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * Interface for GUIs that can receive NEI bookmark group drops.
 * Implement this interface to support dragging NEI bookmark groups into your GUI.
 */
public interface INEIBookmarkGroupReceiver {

    /**
     * Handle a NEI bookmark group being dropped on this GUI.
     *
     * @param mouseX Mouse X coordinate relative to the GUI's left edge
     * @param mouseY Mouse Y coordinate relative to the GUI's top edge
     * @param stacks List of ItemStacks from the bookmark group
     * @return true if the drop was handled successfully, false otherwise
     */
    boolean handleBookmarkGroupDrop(int mouseX, int mouseY, List<ItemStack> stacks);
}
