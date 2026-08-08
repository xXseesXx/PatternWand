# Pattern Integration Fix

## Problem
Patterns could be selected with `/patternwand set <pattern>` command but weren't actually being used during block placement. The wand was just using vanilla BetterBuildersWands palette-based placement instead of executing the Lua pattern scripts.

## Root Cause
The `PatternWandWorker` class was only implementing palette-based block matching (checking if blocks are in the palette) but never actually executing the pattern scripts to determine which blocks to place at each position.

The pattern name was stored in the wand's NBT data but never retrieved or used during block placement.

## Solution

### 1. Enhanced `PatternWandWorker`
**File**: `src/main/java/com/patternwand/items/PatternWandWorker.java`

**Changes**:
- Added constructor parameters for `wandItem` (ItemStack) and `originPos` (Point3d)
- Overrode `placeBlocks()` method to check for active pattern
- Added `placeBlocksWithPattern()` method that:
  - Retrieves the active pattern from wand NBT
  - Gets the compiled Lua script from the script loader
  - Converts the palette to IInventory for the script API
  - For each block position:
    - Calculates relative coordinates from origin
    - Executes the pattern script with world and relative coordinates
    - Gets back a palette index (0-26) or -1 for gap
    - Places the corresponding block from the palette
- Added `getActivePattern()` helper to read pattern name from NBT
- Added `paletteToInventory()` helper to convert PatternPalette to IInventory

### 2. Updated `ItemPatternWand`
**File**: `src/main/java/com/patternwand/items/ItemPatternWand.java`

**Changes**:
- Modified `PatternWandWorker` instantiation to pass `itemstack` and `clickedPos` parameters
- This allows the worker to access the active pattern from NBT and calculate relative coordinates

### 3. Cleaned Up `PatternWandCommand`
**File**: `src/main/java/com/patternwand/commands/PatternWandCommand.java`

**Changes**:
- Removed incomplete "test" command that was causing compilation errors

## How It Works Now

### Pattern Selection
```bash
/patternwand set <pattern_name>
```
This stores the pattern name in the wand's NBT under the key `activePattern`.

### Pattern Execution
When the wand is used:

1. **Block Position Discovery**: Uses BetterBuildersWands' flood-fill algorithm with palette matching to find all blocks to place

2. **Pattern Application**: For each position found:
   - Calculates relative coordinates (relX, relY, relZ) from the click origin
   - Calls the Lua pattern function:
     ```lua
     pattern(worldX, worldY, worldZ, relX, relY, relZ, palette, noise, util, seed)
     ```
   - Gets back a palette index or nil (gap)
   - Places the block from that palette slot

3. **Coordinate Systems**:
   - **World coordinates** (x, y, z): Actual Minecraft world position
   - **Relative coordinates** (relX, relY, relZ): Position relative to where you clicked
   - This allows patterns to be position-independent

### Example Pattern Execution
Given a checkerboard pattern and clicking at (100, 64, 200):
- Block at (102, 64, 202) → relX=2, relY=0, relZ=2
- Pattern executes: `(2 + 2) % 2 == 0` → returns palette index 0
- Places block from first palette slot

## Testing

### In-Game Testing
1. Open the Pattern Wand GUI (shift + right-click)
2. Fill the palette with at least 2 different blocks
3. Use `/patternwand list` to see available patterns
4. Set a pattern: `/patternwand set checkerboard`
5. Right-click on a block to place with the pattern

### Expected Behavior
- Without a pattern: Places blocks matching the palette in a flood-fill
- With a pattern: Places blocks according to the pattern script, cycling through palette slots

## Pattern Examples
The following patterns are included and ready to use:
- `checkerboard` - Simple alternating pattern
- `stripes` - Horizontal stripes
- `diagonal` - 45-degree diagonal stripes  
- `concentric_circles` - Rings radiating from center
- `spiral` - Logarithmic spiral
- `waves` - Sine wave pattern
- `honeycomb` - Hexagonal pattern
- `voronoi` - Organic cellular pattern
- `maze` - Procedural maze-like pattern
- `random_scatter` - Random scattered blocks
- `organic_terrain` - Multi-layered natural terrain

## API Available to Patterns

### Parameters
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
```

### Noise API
- `noise.perlin(x, z)` - 2D Perlin noise
- `noise.perlin3d(x, y, z)` - 3D Perlin noise
- `noise.simplex(x, z)` - 2D Simplex noise
- `noise.simplex3d(x, y, z)` - 3D Simplex noise

### Util API
- `util.hash(x, z)` - Deterministic 2D hash
- `util.hash3d(x, y, z)` - Deterministic 3D hash
- `util.distance(x1, y1, x2, y2)` - Euclidean distance
- `util.manhattan(x1, y1, x2, y2)` - Manhattan distance
- `util.map(value, inMin, inMax, outMin, outMax)` - Range mapping
- `util.clamp(value, min, max)` - Value clamping
- `util.lerp(a, b, t)` - Linear interpolation

### Palette API
- `palette.size()` - Number of slots (27)
- `palette.getWeight(index)` - Stack size at slot
- `palette.isEmpty(index)` - Check if empty
- `palette.countNonEmpty()` - Count filled slots
- `palette.pickWeighted()` - Random weighted selection

### Return Values
- `0-26`: Index of palette slot to use
- `nil`: Gap (don't place any block)

## Build Commands
```bash
# Format code
.\gradlew.bat spotlessApply

# Build without tests
.\gradlew.bat build -x test

# Build with tests
.\gradlew.bat build
```

## Future Improvements
- Add pattern parameters (e.g., stripe width, wave frequency)
- Pattern preview mode
- Pattern rotation/mirroring
- Save/load pattern presets with wand
- Pattern categories and filtering in GUI
