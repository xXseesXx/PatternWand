# Pattern Wand Implementation Summary

## Features Implemented

### 1. Palette-Based Block Matching
Copied from your BetterBuildersWands fork:

- **PatternPalette**: Stores a list of blocks (Block + metadata) that define which blocks should be matched during flood-fill
- **PaletteEntry**: Represents a single block type in the palette
- **BlockMatcher**: Uses a HashSet for fast O(1) block matching during flood-fill operations

### 2. Pattern Wand GUI
Implemented a container-based GUI system:

- **GuiPatternWand**: Client-side GUI screen with 27 palette slots (3x9 grid)
- **ContainerPatternWand**: Container that handles ghost item behavior in palette slots
- **PatternWandGuiHandler**: Registers the GUI and handles opening it

### 3. Ghost Item Behavior
Palette slots use "ghost items" - they appear in the GUI but don't consume items from inventory:

- Left-click with item: Set palette slot to that block
- Right-click: Clear palette slot
- Items cannot be taken out of palette slots
- Palette is saved to wand NBT on GUI close

### 4. Enhanced Wand Functionality
Extended ItemBasicWand with:

- **Palette storage in NBT**: Saved/loaded automatically
- **GUI opening**: Shift+right-click opens palette configuration GUI
- **Palette-based flood-fill**: Uses BlockMatcher instead of exact block matching
- **PatternWandWorker**: Custom worker that overrides block matching logic

### 5. Block Matching Logic
When you use the wand:

1. Reads palette from wand NBT (or uses default if empty)
2. Creates BlockMatcher from palette
3. Flood-fill expands through ANY block in the palette (not just the clicked block)
4. Places blocks from inventory that match palette entries

## Key Differences from Standard BetterBuildersWands

### Standard Wands:
- Match only the exact block you click
- Place that same block everywhere

### Pattern Wand:
- Matches ANY block in your configured palette
- Allows multi-block patterns (e.g., stone + cobblestone + stone bricks all treated as "matching")
- Perfect for replacing mixed materials or creating varied textures

## Files Created/Modified

### New Classes:
- `palette.com.xXseesXx.patternwand.PatternPalette` - Palette data structure
- `palette.com.xXseesXx.patternwand.PaletteEntry` - Single palette entry
- `palette.com.xXseesXx.patternwand.BlockMatcher` - Fast block matching
- `items.com.xXseesXx.patternwand.PatternWandWorker` - Custom WandWorker
- `gui.com.xXseesXx.patternwand.GuiPatternWand` - GUI screen
- `gui.com.xXseesXx.patternwand.ContainerPatternWand` - GUI container
- `gui.com.xXseesXx.patternwand.PatternWandGuiHandler` - GUI handler

### Modified Classes:
- `ItemPatternWand.java` - Added palette support and GUI opening
- `CommonProxy.java` - Registered GUI handler
- `en_US.lang` - Added localization

## What Was NOT Implemented (As Requested)

- Lua scripting features
- Scripted patterns
- Pattern registry system
- Noise patterns
- Complex pattern generation

## Usage

1. Craft/obtain a Pattern Wand
2. Shift+right-click to open the palette GUI
3. Add blocks to the 27 palette slots (ghost items - won't consume from inventory)
4. Close GUI to save palette
5. Right-click on any block in your palette to start flood-fill
6. The wand will match and replace ALL blocks in your palette, not just one type

## Example Use Case

**Palette contains**: Stone, Cobblestone, Stone Bricks
**World has**: Mixed stone/cobblestone/stone brick wall
**You click**: Any of those blocks
**Result**: The entire mixed wall is selected and can be replaced, because all three block types are in your palette

This allows for much more flexible building workflows compared to standard wands!
