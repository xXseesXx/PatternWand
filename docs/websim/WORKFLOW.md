# Web Simulator - Implementation Workflow

This document breaks down the implementation into tasks and maps them to relevant source files in the mod codebase for reference.

## Task Breakdown

### Task #1: Design the UI Layout and Component Structure

**What to build:**
- HTML structure with editor/preview split
- CSS grid layout (40% editor, 60% preview)
- Collapsible control panel at bottom
- Responsive design basics

**Files to read:**
- *None required* - Pure frontend work
- Reference: `docs/websim/DESIGN.md` for layout specs

**Deliverable:**
- Static HTML/CSS skeleton with placeholder elements
- No JavaScript yet, just the visual structure

---

### Task #2: Implement the Lua Runtime Wrapper with Pattern APIs

**What to build:**
- `LuaEngine` class that wraps fengari
- JavaScript implementations of: `palette`, `noise`, `util`, `context`, `debug` APIs
- Metadata extraction from Lua code
- Pattern function compilation and execution

**Files to read for API reference:**

#### Palette API Implementation
```
src/main/java/com/patternwand/api/PaletteAPI.java
```
- Methods: `size()`, `getWeight()`, `isEmpty()`, `countNonEmpty()`
- Methods: `pickWeighted()`, `pickUniform()`, `pickWeightedExcept()`, `pickWeightedRange()`
- Understand the weight-based selection algorithm

#### Noise API Implementation
```
src/main/java/com/patternwand/api/NoiseAPI.java
```
- Methods: `perlin()`, `perlin3d()`, `simplex()`, `simplex3d()`
- Note: Use existing JS noise libraries (simplex-noise.js) for implementation
- Verify output range is `[-1, 1]`

#### Util API Implementation
```
src/main/java/com/patternwand/api/UtilAPI.java
```
- All math helper functions
- Distance calculations
- Geometry helpers
- Hash functions (use simple JS hash implementation)

#### Context API Structure
```
src/main/java/com/patternwand/api/PlacementContext.java
```
- Field structure: `clickedX/Y/Z`, `minX/Y/Z`, `maxX/Y/Z`
- Time fields: `worldTime`, `dayTime`
- Player orientation: `playerYaw`, `playerPitch`

#### Metadata Parsing
```
src/main/java/com/patternwand/pattern/PatternMetadata.java
src/main/java/com/patternwand/pattern/PatternParameter.java
```
- Metadata structure: `name`, `author`, `parameters`
- Parameter types: `integer`, `float`, `boolean`, `string`
- Default values and min/max ranges

#### Pattern Execution
```
src/main/java/com/patternwand/pattern/PatternExecutor.java
src/main/java/com/patternwand/pattern/LuaPatternLoader.java
```
- How patterns are loaded and compiled
- Function signature: `pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)`
- Return value handling (palette index or nil)

**Deliverable:**
- `LuaEngine` class with all APIs injected
- Can execute: `engine.execute(x, y, z, context, params)` and return palette index

---

### Task #3: Build the 2D Canvas Renderer

**What to build:**
- `Renderer` class for Canvas2D drawing
- Grid-based block rendering with colors
- Default 27-color palette (Minecraft-inspired)
- Zoom and pan controls
- Hover tooltips showing coordinates

**Files to read:**
- *None required* - Pure visualization work
- Reference mod's palette system for color choices (but use web colors)

**Optional reference:**
```
src/main/java/com/patternwand/wand/PaletteManager.java
```
- To understand how blocks map to palette indices (0-26)

**Deliverable:**
- `Renderer` class that renders a 32x32 grid
- Each cell colored based on palette index
- Can handle `null` returns (skip rendering)

---

### Task #4: Create the Pattern Analyzer for Smart Parameter Detection

**What to build:**
- `detectUsage(code)` function using regex
- Detect which APIs are used: `palette.*`, `noise.*`, `util.*`, `context.*`, `seed`, `params.*`
- Extract specific context fields used (e.g., `context.worldTime`)
- Dynamic UI section visibility based on detection

**Files to read:**
- *None required* - Pure static analysis work

**Testing patterns:**
```lua
-- Test 1: Minimal (no controls should show)
function pattern(x, y, z)
  return 0
end

-- Test 2: Uses palette (show palette editor)
function pattern(x, y, z, relX, relY, relZ, palette)
  return palette.pickWeighted()
end

-- Test 3: Uses params (show parameter controls)
metadata = {parameters = {size = {type="integer", default=4}}}
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
  return (relX // params.size) % 2
end

-- Test 4: Uses context (show time/seed controls)
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context)
  return context.worldTime % 2 == 0 and 0 or 1
end
```

**Deliverable:**
- `detectUsage(code)` returns object with boolean flags
- UI sections show/hide dynamically as user types

---

### Task #5: Implement the Code Editor with Live Reload

**What to build:**
- Integrate Prism.js for Lua syntax highlighting
- Debounced auto-execution (300ms delay)
- Error handling with line number highlighting
- Example pattern dropdown/selector

**Files to read for example patterns:**
```
src/main/resources/assets/patternwand/patterns/examples/checkerboard.lua
src/main/resources/assets/patternwand/patterns/examples/bricks.lua
src/main/resources/assets/patternwand/patterns/examples/weighted_random.lua
src/main/resources/assets/patternwand/patterns/examples/perlin_terrain.lua
src/main/resources/assets/patternwand/patterns/examples/simplex_organic.lua
```
- Copy these as embedded example patterns in the simulator

**Deliverable:**
- Working code editor with syntax highlighting
- Auto-reload on typing (debounced)
- Example patterns can be loaded from dropdown
- Errors displayed inline

---

### Task #6: Add Palette Configuration UI

**What to build:**
- 27 color input swatches
- Weight/stack size sliders (1-64) for each color
- "Empty" checkbox to mark slots as empty
- Preset palettes (stone variants, wood types, etc.)
- Visual feedback showing which colors are used in current pattern

**Files to read:**
```
src/main/java/com/patternwand/wand/PaletteManager.java
```
- Understand how palette slots work
- How weights affect `pickWeighted()` selection

**Preset palette examples:**
```javascript
const presets = {
  "Stone Variants": [
    {color: "#7F7F7F", weight: 10}, // Stone
    {color: "#606060", weight: 5},  // Cobblestone
    {color: "#4A4A4A", weight: 8},  // Stone Bricks
    // ... etc
  ],
  "Wood Types": [...],
  "Terracotta": [...]
};
```

**Deliverable:**
- Palette editor UI with 27 color/weight controls
- Can load/save presets
- Changes reflect immediately in preview

---

### Task #7: Implement Debug Output and Error Handling

**What to build:**
- Console panel that captures `debug.print()` output
- Lua error display with stack traces
- Clear button for console
- Performance metrics (execution time, blocks rendered)

**Files to read:**
```
src/main/java/com/patternwand/api/DebugAPI.java
```
- How `debug.print()` is implemented in mod
- Error format and messaging

**Error handling:**
```
src/main/java/com/patternwand/pattern/PatternExecutor.java
```
- How Lua errors are caught and displayed
- Line number extraction from stack traces

**Deliverable:**
- Debug console that shows `debug.print()` output
- Lua errors displayed with line numbers and descriptions
- Performance stats (e.g., "Rendered 1024 blocks in 45ms")

---

## Quick Reference: Mod File Map

### Core Pattern System
```
src/main/java/com/patternwand/pattern/
├── PatternMetadata.java      # Metadata structure
├── PatternParameter.java     # Parameter definitions
├── PatternExecutor.java      # Pattern execution logic
└── LuaPatternLoader.java     # Loading and compiling patterns
```

### Lua APIs
```
src/main/java/com/patternwand/api/
├── PaletteAPI.java           # Palette selection methods
├── NoiseAPI.java             # Perlin/Simplex noise
├── UtilAPI.java              # Math and geometry helpers
└── DebugAPI.java             # Debug printing
```

### Placement Context
```
src/main/java/com/patternwand/api/PlacementContext.java
```

### Example Patterns
```
src/main/resources/assets/patternwand/patterns/examples/
├── checkerboard.lua
├── bricks.lua
├── weighted_random.lua
├── perlin_terrain.lua
├── simplex_organic.lua
├── configurable_bricks.lua
└── sphere.lua
```

### Commands (for understanding parameter parsing)
```
src/main/java/com/patternwand/command/PatternWandCommand.java
```
- Look at the `/patternwand set` command for parameter parsing logic

---

## Development Order

**Recommended workflow:**

1. **Task #1** → Build UI skeleton (no dependencies)
2. **Task #2** → Implement Lua runtime (core functionality)
3. **Task #3** → Add renderer (visual feedback)
4. **Task #5** → Add code editor (can now test patterns!)
5. **Task #4** → Add smart detection (UI improvements)
6. **Task #6** → Add palette editor (full palette support)
7. **Task #7** → Add debug panel (polish)

**Parallel work possible:**
- Task #1 and Task #2 can be done in parallel
- Task #3 and Task #4 are independent
- Task #6 and Task #7 are independent polish tasks

---

## Testing Strategy

### Unit Tests (per task)

**Task #2 (Lua Engine):**
```javascript
// Test palette API
assert(palette.size() === 27);
assert(palette.pickWeighted() >= 0 && palette.pickWeighted() < 27);

// Test noise API
assert(noise.perlin(0, 0) >= -1 && noise.perlin(0, 0) <= 1);

// Test util API
assert(util.clamp(5, 0, 10) === 5);
assert(util.clamp(-5, 0, 10) === 0);
```

**Task #3 (Renderer):**
```javascript
// Test rendering
renderer.render((x, y, z) => 0, 32); // Should render all blocks with color 0
renderer.render((x, y, z) => null, 32); // Should render nothing
```

**Task #4 (Detection):**
```javascript
const usage = detectUsage("function pattern(x,y,z,relX,relY,relZ,palette) return palette.pickWeighted() end");
assert(usage.usesPalette === true);
assert(usage.usesNoise === false);
```

### Integration Tests

```javascript
// Load pattern, execute, and render
const code = `function pattern(x, y, z) return (x + z) % 2 end`;
const {metadata, fn, error} = engine.loadPattern(code);
assert(error === null);

renderer.render((x, y, z) => engine.execute(x, 0, z, context, {}), 32);
// Visual inspection: should show checkerboard
```

### Example Pattern Tests

Load each example pattern from the mod and verify it renders without errors:

```javascript
const examples = [
  'checkerboard.lua',
  'bricks.lua',
  'weighted_random.lua',
  'perlin_terrain.lua'
];

examples.forEach(file => {
  const code = loadExamplePattern(file);
  const result = engine.loadPattern(code);
  assert(result.error === null, `${file} failed to load`);
});
```

---

## Performance Targets

- **Pattern compilation:** < 50ms
- **32x32 render:** < 100ms
- **64x64 render:** < 300ms
- **128x128 render:** < 1000ms
- **Debounce delay:** 300ms
- **Total page load:** < 2s (with CDN)

If any target is missed, implement optimizations:
- Web Worker for Lua execution
- Canvas pooling
- Incremental rendering
- Virtual scrolling for large grids
