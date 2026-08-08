# PatternWand Web Simulator - Design Document

## Overview

A simplified 2D web-based pattern simulator for rapid prototyping and testing of Lua patterns without launching Minecraft.

## Philosophy

- **Start minimal, grow smart** - Only show controls for what the pattern actually uses
- **Instant feedback** - Re-render on every keystroke (debounced)
- **Zero configuration** - Works in a single HTML file, no build process
- **Visual first** - Big preview, code secondary

## UI Layout

```
┌─────────────────────────────────────────────────────────┐
│  PatternWand Simulator           [Load Example ▼] [?]  │
├──────────────────┬──────────────────────────────────────┤
│                  │                                       │
│   Code Editor    │         2D Preview Canvas            │
│   (40% width)    │         (60% width)                  │
│                  │    ┌─────────────────────────┐      │
│  1 function      │    │  ████░░░░████░░░░       │      │
│  2 pattern(...)  │    │  ░░░░████░░░░████       │      │
│  3   return 0    │    │  ████░░░░████░░░░       │      │
│  4 end           │    │  ░░░░████░░░░████       │      │
│                  │    └─────────────────────────┘      │
│                  │                                       │
├──────────────────┴───────────────────────────────────┬──┤
│ Dynamic Controls (only show what's used)              │▲▼│
│  📦 Palette: [■][■][■][■][■] (5 colors)              │  │
│  📊 Pattern Parameters: brickWidth [━━━●━━] 6         │  │
│  📍 Context: seed=12345  size=32x32                   │  │
│  🐛 Debug Output: (empty)                             │  │
└───────────────────────────────────────────────────────┴──┘
```

## Core Features

### 1. Smart Context Detection

Analyze Lua code to detect what's actually used and dynamically show/hide UI controls:

```javascript
// Parse code and detect usage
const detectUsage = (code) => {
  return {
    usesRelativeCoords: /rel[XYZ]/.test(code),
    usesAbsoluteCoords: /\bx\b|\by\b|\bz\b/.test(code) && !/rel/.test(code),
    usesPalette: /palette\./.test(code),
    usesNoise: /noise\./.test(code),
    usesContext: /context\./.test(code),
    usesSeed: /\bseed\b/.test(code),
    usesParams: /\bparams\./.test(code),
    contextFields: extractContextFields(code) // e.g., ['clickedX', 'worldTime']
  };
};
```

**UI Behavior:**
- Start with ONLY code editor + preview
- As user types, dynamically show relevant controls
- Example: Type `palette.pickWeighted()` → Palette editor appears
- Example: Type `context.worldTime` → Time slider appears

### 2. Progressive Disclosure

**Level 1: Empty State**
```
┌─────────────┬─────────────┐
│ Code        │   Preview   │
│             │  [Empty]    │
└─────────────┴─────────────┘
```

**Level 2: Minimal Pattern**
```lua
function pattern(x, y, z)
  return 0
end
```
→ Shows solid color, no controls

**Level 3: Using Palette**
```lua
function pattern(x, y, z, relX, relY, relZ, palette)
  return (relX + relZ) % 2
end
```
→ Palette section appears with 27 color swatches

**Level 4: Using Parameters**
```lua
metadata = {
  parameters = {size = {type="integer", default=4}}
}
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
  return (relX // params.size + relZ // params.size) % 2
end
```
→ Slider for "size" appears above palette

## Technical Stack

**Single-file MVP:**
```html
<!DOCTYPE html>
<html>
<head>
  <title>PatternWand Simulator</title>
  <script src="https://cdn.jsdelivr.net/npm/fengari-web@0.1.4/dist/fengari-web.js"></script>
  <style>/* Inline CSS */</style>
</head>
<body>
  <div id="app"></div>
  <script>/* All JS inline */</script>
</body>
</html>
```

**Libraries:**
- **Lua Runtime:** fengari-web (Lua 5.3 in browser)
- **Code Editor:** `<textarea>` with syntax highlighting via Prism.js (lightweight)
- **Canvas:** Native HTML5 Canvas2D
- **No framework:** Vanilla JS (under 500 lines total)

## Component Architecture

### A. LuaEngine (Lua API wrapper)

```javascript
class LuaEngine {
  constructor() {
    this.L = fengari.lauxlib.luaL_newstate();
    this.injectAPIs();
  }
  
  injectAPIs() {
    // Inject: palette, noise, util, context, debug
    // Keep it simple - JavaScript implementations
  }
  
  loadPattern(code) {
    // Extract metadata if present
    // Compile pattern function
    // Return {metadata, fn, error}
  }
  
  execute(x, y, z, context, params) {
    // Call pattern(x, y, z, ...) 
    // Return palette index or null
  }
}
```

### B. Renderer (2D Canvas)

```javascript
class Renderer {
  constructor(canvas) {
    this.ctx = canvas.getContext('2d');
    this.palette = this.generateDefaultPalette();
    this.zoom = 1;
  }
  
  render(patternFn, size = 32) {
    const cellSize = Math.floor(this.canvas.width / size);
    
    for (let x = 0; x < size; x++) {
      for (let z = 0; z < size; z++) {
        const idx = patternFn(x, 0, z, x, 0, z);
        if (idx !== null) {
          this.ctx.fillStyle = this.palette[idx] || '#888';
          this.ctx.fillRect(x * cellSize, z * cellSize, cellSize, cellSize);
        }
      }
    }
  }
  
  generateDefaultPalette() {
    // 27 distinct colors (Minecraft-ish palette)
    return ['#000', '#333', '#666', '#999', '#ccc', '#fff', ...];
  }
}
```

### C. UI Controller

```javascript
class SimulatorApp {
  constructor() {
    this.engine = new LuaEngine();
    this.renderer = new Renderer(document.getElementById('canvas'));
    this.setupEditor();
    this.setupControls();
  }
  
  onCodeChange(code) {
    clearTimeout(this.debounce);
    this.debounce = setTimeout(() => {
      this.reload(code);
    }, 300);
  }
  
  reload(code) {
    const {metadata, fn, error} = this.engine.loadPattern(code);
    
    if (error) {
      this.showError(error);
      return;
    }
    
    // Update UI based on detected usage
    this.updateDynamicControls(code, metadata);
    
    // Render
    this.renderer.render((x, y, z, rx, ry, rz) => {
      return this.engine.execute(x, y, z, this.context, this.params);
    }, this.size);
  }
  
  updateDynamicControls(code, metadata) {
    const usage = detectUsage(code);
    
    // Show/hide sections
    this.elements.paletteSection.hidden = !usage.usesPalette;
    this.elements.contextSection.hidden = !usage.usesContext;
    
    // Build parameter controls from metadata
    if (metadata?.parameters) {
      this.buildParamControls(metadata.parameters);
    }
  }
}
```

## Visual Design

**Color Scheme:**
- Background: `#1e1e1e` (VS Code dark)
- Editor: `#252526`
- Canvas: `#2d2d30` with grid lines
- Accent: `#007acc` (blue)
- Success: `#4ec9b0` (teal)
- Error: `#f48771` (red)

**Typography:**
- Code: `'Fira Code', 'Consolas', monospace`
- UI: `'Segoe UI', system-ui, sans-serif`

**Grid Rendering:**
- Show grid lines for blocks when zoomed in (> 16px per block)
- Hover shows coordinates and palette index
- Click to inspect that block's evaluation

## Performance Optimizations

1. **Debounce rendering** (300ms after typing stops)
2. **Size limit** (default 32x32, max 128x128)
3. **Web Worker for Lua** (if patterns take >100ms)
4. **Canvas pooling** (reuse canvas for re-renders)
5. **Incremental updates** (only re-render changed cells if deterministic)

## File Structure

```
pattern-simulator/
├── index.html           # Main application (single file)
├── lib/
│   └── fengari-web.js  # Lua runtime (CDN fallback)
└── patterns/
    └── examples.json    # Embedded example patterns
```

## Example Patterns (Embedded)

```javascript
const examples = {
  "Checkerboard": `function pattern(x, y, z)
  return (x + z) % 2
end`,

  "Bricks": `metadata = {
  parameters = {width = {type="integer", default=4, min=2, max=8}}
}
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
  local offset = math.floor(relZ / 2) % 2 == 0 and 0 or params.width // 2
  return (math.floor(relX + offset) // params.width + relZ) % 2
end`,

  "Perlin Noise": `function pattern(x, y, z, relX, relY, relZ, palette, noise)
  local n = noise.perlin(relX * 0.1, relZ * 0.1)
  if n > 0 then return 0 else return 1 end
end`,

  "Weighted Random": `function pattern(x, y, z, relX, relY, relZ, palette)
  return palette.pickWeighted()
end`
};
```

## Development Phases

**Phase 1: Core (Day 1)**
- Single HTML file
- Fengari integration
- Basic canvas renderer
- Textarea code editor
- Execute on button click

**Phase 2: Live Editing (Day 2)**
- Auto-reload on typing
- Syntax highlighting (Prism.js)
- Error display
- Basic palette colors

**Phase 3: Smart UI (Day 3)**
- Usage detection
- Dynamic control generation
- Parameter controls from metadata
- Palette editor

**Phase 4: Polish (Day 4)**
- Example patterns dropdown
- Zoom/pan canvas
- Export pattern code
- Hover tooltips
- Mobile responsive

## Success Metrics

- **Cold start:** Opens and renders example in < 2 seconds
- **Hot reload:** Pattern changes reflect in < 200ms
- **File size:** Entire simulator < 100KB (without Fengari)
- **Learning curve:** New user creates custom pattern in < 5 minutes

## API Parity with Mod

The simulator must implement the exact same Lua API as the mod:

### Palette API
- `palette.size()` - Returns 27
- `palette.getWeight(index)` - Get stack size (1-64, or 0 if empty)
- `palette.isEmpty(index)` - Check if slot is empty
- `palette.countNonEmpty()` - Count non-empty slots
- `palette.pickWeighted()` - Random selection by weight
- `palette.pickUniform()` - Random selection (equal probability)
- `palette.pickWeightedExcept(indices)` - Weighted excluding indices
- `palette.pickWeightedRange(min, max)` - Weighted from range

### Noise API
All functions return `[-1, 1]`:
- `noise.perlin(x, z)` - 2D Perlin noise
- `noise.perlin3d(x, y, z)` - 3D Perlin noise
- `noise.simplex(x, z)` - 2D Simplex noise
- `noise.simplex3d(x, y, z)` - 3D Simplex noise

### Util API
- `util.abs(value)`, `util.floor(value)`, `util.ceil(value)`
- `util.mod(a, b)`, `util.sign(value)`
- `util.clamp(value, min, max)`
- `util.lerp(a, b, t)`, `util.smoothstep(edge0, edge1, x)`
- `util.map(value, inMin, inMax, outMin, outMax)`
- `util.distance(x1, y1, x2, y2)`, `util.distance3d(x1, y1, z1, x2, y2, z2)`
- `util.manhattan(x1, y1, x2, y2)`
- `util.inSphere(x, y, z, centerX, centerY, centerZ, radius)`
- `util.inBox(x, y, z, minX, minY, minZ, maxX, maxY, maxZ)`
- `util.rotate2D(x, y, angle)` - Returns table `{x, y}`
- `util.hash(x, z)`, `util.hash3d(x, y, z)`

### Context API
- `context.clickedX, clickedY, clickedZ` - Click position
- `context.clickFace` - Face clicked (0-5)
- `context.minX, minY, minZ` - Bounding box min
- `context.maxX, maxY, maxZ` - Bounding box max
- `context.playerYaw, playerPitch` - Player rotation
- `context.worldTime` - Total world time in ticks
- `context.dayTime` - Day time (0-24000)

### Debug API
- `debug.print(...)` - Print to console

## Simplified Context for 2D

Only show context values when actually used in the pattern:
- **Always available:** `x, z, relX, relZ` (2D coordinates)
- **Hidden by default:** `y, relY, context.*, playerYaw, playerPitch`
- **Show on detection:** If pattern uses `context.worldTime`, show time slider
- **Show on detection:** If pattern uses `seed`, show seed input
