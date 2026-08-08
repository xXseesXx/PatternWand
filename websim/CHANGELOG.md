# Changelog

## [CRITICAL FIX] 2026-08-08 - Full Pattern Compatibility

### Major Fixes
- ✅ **Lua Standard Libraries** - Now properly loads ALL Lua standard libraries (math, string, table, etc.)
  - Fixed: `math.sin()`, `math.floor()`, `math.ceil()` now work
  - Fixed: `math.pi`, `math.huge` constants available
  - Fixed: String operations now work
  - Fixed: Table operations now work
- ✅ **All Example Patterns** - Added all 8 patterns from the mod:
  - Checkerboard (basic 2-color)
  - Bricks (offset pattern with mortar)
  - Configurable Bricks (with parameters)
  - Noise Terrain (Perlin noise based)
  - Gradient (smooth sine-wave transitions)
  - Ripples (circular wave pattern)
  - Random Mix (palette selection demo)
  - Spherical Dome (3D geometry)
- ✅ **String Parameters** - Added proper string parameter support
  - Dropdown selector for enum-like values (e.g., "mode")
  - Text input for freeform strings
- ✅ **Float Parameters** - Fixed precision handling
  - Proper step calculation based on min/max range
  - Display shows 2 decimal places

### What Works Now
✅ `math.sin()`, `math.cos()`, `math.tan()` - Trigonometry  
✅ `math.floor()`, `math.ceil()` - Rounding  
✅ `math.abs()`, `math.sqrt()` - Basic math  
✅ `math.min()`, `math.max()` - Comparisons  
✅ `math.random()` - Random numbers  
✅ All palette API methods  
✅ All noise API methods (Perlin + Simplex)  
✅ All util API methods  
✅ All parameter types (integer, float, boolean, string)  
✅ Debug output  
✅ Context API  
✅ Nil returns (skip blocks)  

### Pattern Compatibility
**All patterns from the mod should now work in the simulator without modification!**

Copy any `.lua` file from `src/main/resources/assets/patternwand/patterns/examples/` directly into the simulator - it will work.

Test with `websim/TESTING.md` checklist.

---

## [Fixed] 2026-08-08 - Rendering System

### Critical Fixes
- ✅ **Fixed preview rendering** - Canvas now properly renders patterns
- ✅ **World coordinates** - Uses actual Minecraft-like coordinates (Y=64 for ground level)
- ✅ **Proper coordinate system**:
  - World coords: x=0-31, y=64, z=0-31 (absolute positions)
  - Relative coords: relX=0-31, relY=0, relZ=0-31 (relative to clicked position)
- ✅ **Better error handling** - Shows errors in UI and logs details to console
- ✅ **Loading indicator** - Shows "Loading..." until fully initialized
- ✅ **Startup diagnostics** - Checks all dependencies on load
- ✅ **SimplexNoise fallback** - Falls back to Perlin if SimplexNoise CDN fails
- ✅ **Default pattern** - Simple checkerboard that works immediately
- ✅ **Render statistics** - Shows blocks rendered, time taken, and errors

### Coordinate System
The simulator emulates actual Minecraft world coordinates:
- **X-Z plane** displayed as 2D grid (top-down view)
- **Y coordinate** fixed at 64 (typical ground level)
- **Clicked position** at (0, 64, 0) as origin
- **Bounding box** from (0,64,0) to (31,64,31) by default

This matches how patterns work in-game!

---

## [Improved] 2026-08-08 - Palette Editor

### Changes
- ✅ Palette section now **always visible** (no longer hidden by default)
- ✅ Color pickers are now **clearly visible** as colored squares
- ✅ Weight controls changed from right-click to **direct number inputs** below each color
- ✅ Added **preset palettes** dropdown with 5 presets:
  - Grayscale (black to white gradient)
  - Warm Colors (reds, oranges, yellows)
  - Cool Colors (blues, teals, cyans)
  - Rainbow (full spectrum)
  - Earth Tones (browns, greens, tans)
- ✅ Added helpful instructions: "Click colors to change, adjust weights below each slot"
- ✅ Improved hover effects with glow on palette slots

**How to Use:**
1. Click any colored square to open the color picker
2. Type a number (0-64) in the box below to set weight
3. Or select a preset from the dropdown for instant themes

**Tips:**
- Weight = 0 means the slot is empty
- Higher weights = more likely to be picked by `palette.pickWeighted()`
- Use presets as starting points, then customize individual colors

---

## Testing

Run through `websim/TESTING.md` to verify all patterns work correctly.

Quick smoke test:
1. Open `websim/index.html`
2. Load each example from dropdown
3. All should render without errors
4. Try adjusting parameters
5. Check browser console (F12) - should have no errors
