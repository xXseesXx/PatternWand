# Pattern Wand Highlighting Fix

## Problem
The Pattern Wand's block highlighting was not correctly showing which blocks would be placed based on the palette-based flood matching. Instead, it was using the default BetterBuildersWands exact-match logic, which only highlights blocks that exactly match the clicked block.

## Root Cause
The `BlockEvents.java` handler in BetterBuildersWands was rendering highlights for all wand items, including the Pattern Wand. However, it was using the base `WandWorker` class which only does exact block matching, not the custom `PatternWandWorker` which implements palette-based flood matching.

## Solution
Created a custom client-side event handler `PatternWandBlockEvents.java` that:

1. **Runs at HIGH priority** - This ensures it executes before the default BetterBuildersWands handler
2. **Cancels the event** - When a Pattern Wand is detected, it cancels the event to prevent the default handler from running
3. **Uses PatternWandWorker** - Creates a proper `PatternWandWorker` instance with the wand's palette and matcher
4. **Renders with palette logic** - The flood fill and highlighting now correctly uses the palette-based matching

## Changes Made

### New File: `src/main/java/com/patternwand/client/PatternWandBlockEvents.java`
- Client-side event handler for Pattern Wand highlighting
- Subscribes to `DrawBlockHighlightEvent` with HIGH priority
- Detects Pattern Wand items and uses custom rendering logic
- Creates `PatternWandWorker` with palette/matcher for accurate flood fill preview
- Renders highlights with a greenish tint (0x40A040) to distinguish from default wands

### Modified: `src/main/java/com/patternwand/ClientProxy.java`
- Registered the `PatternWandBlockEvents` handler in the `init()` method
- This ensures the custom highlighting is active on the client side

## How It Works

1. **Event Priority**: The event handler runs at `EventPriority.HIGH`, which means it executes before the default BetterBuildersWands handler (which runs at NORMAL priority)

2. **Detection**: When the player is holding an item that implements `IPatternWandItem`, the handler takes over

3. **Cancellation**: The event is canceled, preventing the default handler from drawing incorrect highlights

4. **Custom Rendering**: 
   - Gets the palette from the wand's NBT
   - Creates a `BlockMatcher` with the palette
   - Instantiates `PatternWandWorker` with palette-based matching
   - Calls `getBlockPositionList()` which uses the custom flood matching algorithm
   - Renders bounding boxes for all matched positions

5. **Visual Distinction**: Uses green-tinted highlights (0x40A040) instead of the default gray (0xC0C0C0), making it clear that palette matching is active

## Testing
To verify the fix:
1. Create a Pattern Wand
2. Add multiple different blocks to the palette (e.g., stone, cobblestone, andesite)
3. Build a structure mixing these blocks
4. Hover over any block in the structure
5. The highlighting should now show ALL connected blocks that are in the palette, not just exact matches

## Technical Details
- **Event Bus**: Uses Forge's `MinecraftForge.EVENT_BUS`
- **Side**: Client-only (`@SideOnly(Side.CLIENT)`)
- **Priority**: HIGH to override default behavior
- **Cancellation**: Prevents event propagation to lower-priority handlers
