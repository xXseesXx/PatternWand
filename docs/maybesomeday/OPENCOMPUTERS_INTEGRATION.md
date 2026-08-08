# OpenComputers Integration Design

## Overview

Integration with OpenComputers (OC) to enable:
1. **Pattern Generation** - OC programs can generate pattern Lua files
2. **Pattern Preview** - Virtual preview of patterns before placement
3. **Pattern Library Management** - List, query, and manage patterns from OC
4. **Wand Control** - Trigger pattern placement from OC robots/computers

## Use Cases

### 1. Pattern Generation from OC Programs

Players can write OC programs that procedurally generate pattern files:

```lua
-- On OpenComputers computer
local patternwand = require("component").patternwand_manager
local fs = require("filesystem")

-- Generate a parametric pattern based on user input
local function generateSpiral(density, height)
    local code = [[
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local angle = util.atan2(relX, relZ)
    local radius = util.distance(relX, relZ, 0, 0)
    local spiral = (angle + radius * ]] .. density .. [[) % ]] .. height .. [[
    return util.floor(spiral)
end
return pattern
]]
    return code
end

-- Save pattern to file
local pattern = generateSpiral(0.5, 5)
local file = fs.open("/mnt/xxx/config/patternwand/patterns/generated_spiral.lua", "w")
file:write(pattern)
file:close()

-- Reload patterns in PatternWand
patternwand.reload()
print("Pattern generated and loaded!")
```

### 2. Pattern Preview System

Before placing blocks, preview patterns in a virtual 3D grid:

```lua
-- On OpenComputers computer with screen
local patternwand = require("component").patternwand_manager
local gpu = require("component").gpu

-- Create preview context
local preview = patternwand.createPreview("checkerboard", 10, 10, 1)

-- Render preview slice by slice
for y = 0, 9 do
    for x = 0, 9 do
        local paletteIndex = preview.getBlock(x, y, 0)
        if paletteIndex >= 0 then
            local color = preview.getPaletteColor(paletteIndex)
            gpu.setBackground(color)
            gpu.set(x + 1, y + 1, " ")
        end
    end
end

print("Preview: " .. preview.blockCount .. " blocks")
preview.close()
```

### 3. Pattern Library Browser

Build an OC program to browse and search available patterns:

```lua
-- Pattern library browser GUI
local patternwand = require("component").patternwand_manager

-- List all patterns
local patterns = patternwand.listPatterns()
for i, pattern in ipairs(patterns) do
    print(i .. ". " .. pattern.name)
    if pattern.metadata.author then
        print("   Author: " .. pattern.metadata.author)
    end
    if pattern.metadata.description then
        print("   " .. pattern.metadata.description)
    end
    print()
end

-- Get pattern details
local info = patternwand.getPatternInfo("checkerboard")
print("Parameters:")
for name, param in pairs(info.parameters) do
    print("  " .. name .. " (" .. param.type .. "): " .. param.default)
end
```

### 4. Robot Automation

OC robots can use pattern wands programmatically:

```lua
-- On OpenComputers robot with pattern wand in tool slot
local robot = require("robot")
local patternwand = require("component").patternwand_controller

-- Set pattern and parameters
patternwand.setPattern("bricks", {brickWidth = 4, weathered = true})
patternwand.setSeed(12345)

-- Robot navigates and places pattern
robot.forward()
robot.down()
robot.use() -- Uses pattern wand (standard robot API)

-- Query wand state
local info = patternwand.getWandInfo()
print("Blocks remaining: " .. info.blocksRemaining)
print("Active pattern: " .. info.activePattern)
```

## Component Architecture

### Component 1: `patternwand_manager` (Server-Side)

**Purpose:** Pattern file management and preview generation

**Methods:**

```lua
-- Pattern Management
listPatterns(): table
    -- Returns: {{name="checkerboard", metadata={...}}, ...}

getPatternInfo(name: string): table
    -- Returns: {name, metadata, parameters, source}

reload(): boolean
    -- Reloads all patterns from disk
    -- Returns: success

validatePattern(luaCode: string): boolean, string
    -- Validates pattern code without saving
    -- Returns: success, errorMessage

-- Pattern Preview (Virtual Execution)
createPreview(patternName: string, width: int, height: int, depth: int[, parameters: table]): table
    -- Creates preview context
    -- Returns: preview handle with methods:
    --   .getBlock(x, y, z): int  -- Returns palette index or -1
    --   .blockCount(): int
    --   .getPaletteColor(index): int  -- For visualization
    --   .close()

-- File Operations (Sandboxed to patterns directory)
writePattern(name: string, luaCode: string): boolean, string
    -- Writes pattern to config/patternwand/patterns/
    -- Returns: success, errorMessage

deletePattern(name: string): boolean
    -- Only deletes user-created patterns, not examples
```

**Energy Cost:** Medium (pattern compilation, preview generation)

### Component 2: `patternwand_controller` (Per-Wand NBT Access)

**Purpose:** Control specific pattern wands (in inventory or adjacent)

**Methods:**

```lua
-- Wand State
getWandInfo(): table
    -- Returns: {activePattern, seed, blocksRemaining, blocksCapacity, palette}

setPattern(patternName: string[, parameters: table]): boolean
    -- Sets active pattern with optional parameters

setSeed(seed: number): boolean
    -- Sets custom seed

clearSeed(): boolean
    -- Clears custom seed (use world seed)

-- Palette Management
getPalette(): table
    -- Returns: {{slotIndex=0, blockId="minecraft:stone", stackSize=64}, ...}

setPaletteSlot(index: int, side: int, slot: int): boolean
    -- Pulls item from adjacent inventory into wand palette
    -- Returns: success

-- Debug
enableDebug(enabled: boolean): boolean
setDebugMode(enabled: boolean): boolean
```

**Energy Cost:** Low (NBT read/write)

### Component 3: `patternwand_visualizer` (Client-Only, Optional)

**Purpose:** Enhanced visualization and hologram integration

**Methods:**

```lua
-- Hologram Integration
projectPreview(patternName: string, width: int, height: int, depth: int): boolean
    -- Projects preview to connected hologram projector

-- Color Mapping
getBlockColorMap(): table
    -- Returns: {["minecraft:stone"] = 0xAAAAAA, ...}
```

## Implementation Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    OpenComputers Computer                    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Lua Program (User-Written)                        │    │
│  │  - Pattern generator                               │    │
│  │  - Preview renderer                                │    │
│  │  - Library browser                                 │    │
│  └───────────────────┬────────────────────────────────┘    │
│                      │                                      │
│                      ▼                                      │
│  ┌────────────────────────────────────────────────────┐    │
│  │  component.patternwand_manager                     │    │
│  │  component.patternwand_controller                  │    │
│  └───────────────────┬────────────────────────────────┘    │
└────────────────────────┼───────────────────────────────────┘
                         │
                         │ (Component Calls via OC API)
                         │
┌────────────────────────┼───────────────────────────────────┐
│                        ▼                                    │
│              PatternWand Mod                                │
│                                                             │
│  ┌──────────────────────────────────────────────────┐     │
│  │  OCIntegration.java (if OC present)              │     │
│  │  - Registers components                          │     │
│  │  - Implements component callbacks                │     │
│  │  - Security/sandboxing                           │     │
│  └────────────┬────────────┬──────────────┬─────────┘     │
│               │            │              │               │
│               ▼            ▼              ▼               │
│  ┌─────────────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │ PatternManager  │ │WandNBT   │ │PreviewGenerator  │   │
│  │ Component       │ │Component │ │                  │   │
│  │ - listPatterns()│ │- getInfo │ │- Virtual ScriptEng│  │
│  │ - reload()      │ │- setPatt │ │- Color mapping   │   │
│  │ - validate()    │ └──────────┘ └──────────────────┘   │
│  └─────────────────┘                                      │
│               │                                            │
│               ▼                                            │
│  ┌───────────────────────────────────────────────────┐   │
│  │  Existing PatternWand Core                        │   │
│  │  - ScriptEngine                                   │   │
│  │  - PatternScriptLoader                            │   │
│  │  - ItemPatternWand                                │   │
│  └───────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Security Considerations

1. **Sandboxed File Access**
   - OC can only read/write to `config/patternwand/patterns/`
   - Cannot read example patterns source (read-only)
   - Cannot access arbitrary file system

2. **Pattern Validation**
   - All generated patterns must pass ScriptEngine compilation
   - Timeout protection applies to preview generation
   - Resource limits on preview size (max 64x64x64)

3. **Energy Costs**
   - Pattern preview generation: 1000 energy per 1000 blocks
   - Pattern reload: 500 energy
   - File write: 100 energy
   - NBT read/write: 50 energy

4. **Permission System** (Optional, via config)
   - `patternwand.oc.allowFileWrite` (default: true)
   - `patternwand.oc.allowPreview` (default: true)
   - `patternwand.oc.maxPreviewVolume` (default: 262144 = 64^3)

## Preview System Details

### Color Mapping Strategy

Since OC screens are limited to 16 colors (T2) or 256 colors (T3), we need intelligent color mapping:

```java
// In PreviewGenerator.java
public class PreviewGenerator {
    public int getBlockColor(ItemStack blockStack) {
        // 1. Check cache first
        // 2. Get block's map color (vanilla system)
        // 3. Convert to RGB
        // 4. Quantize to nearest OC color palette
        // 5. Return 0xRRGGBB format
    }
}
```

### Preview Rendering Example

```lua
-- 2D slice viewer with color
local patternwand = require("component").patternwand_manager
local gpu = require("component").gpu

local preview = patternwand.createPreview("noise_terrain", 16, 16, 1, {})

-- Get all unique colors in preview
local colorMap = {}
for y = 0, 15 do
    for x = 0, 15 do
        local idx = preview.getBlock(x, y, 0)
        if idx >= 0 then
            colorMap[idx] = preview.getPaletteColor(idx)
        end
    end
end

-- Render with colors
for y = 0, 15 do
    for x = 0, 15 do
        local idx = preview.getBlock(x, y, 0)
        if idx >= 0 then
            gpu.setBackground(colorMap[idx])
            gpu.set(x + 1, y + 1, " ")
        else
            gpu.setBackground(0x000000)
            gpu.set(x + 1, y + 1, " ")
        end
    end
end

preview.close()
```

## Implementation Phases

### Phase 1: Core Component Framework (Foundation)
- Add OC as optional dependency
- Create `OCIntegration.java` with soft dependency loading
- Implement basic component registration
- Add `patternwand_manager` with `listPatterns()` and `reload()`

### Phase 2: Pattern Management
- Implement `getPatternInfo()`, `validatePattern()`
- Add sandboxed file operations
- Implement pattern metadata extraction

### Phase 3: Preview System
- Create `PreviewGenerator.java`
- Implement virtual pattern execution (no world modification)
- Add color mapping system
- Implement `createPreview()` API

### Phase 4: Wand Control
- Add `patternwand_controller` component
- Implement NBT access for wand state
- Add `setPattern()`, `setSeed()` methods

### Phase 5: Documentation & Examples
- Create example OC programs
- Write OC manual pages
- Create tutorial patterns

## Configuration

```groovy
// In Config.java
public static boolean ocIntegrationEnabled = true;
public static boolean ocAllowFileWrite = true;
public static boolean ocAllowPreview = true;
public static int ocMaxPreviewVolume = 262144; // 64^3
public static int ocPreviewEnergyCost = 1; // per 1000 blocks
```

## Benefits

1. **Pattern Development** - Use OC's powerful editor for complex patterns
2. **Automation** - Robots can use pattern wands programmatically
3. **Visualization** - Preview patterns before placing
4. **Integration** - Combine with other OC-controlled systems
5. **Education** - Learn Lua programming through pattern creation

## Limitations

1. **Performance** - Preview generation for large volumes may be slow
2. **Color Accuracy** - OC screen colors are approximations
3. **No Direct Placement** - OC can't place blocks directly (by design)
4. **Mod Dependency** - Requires OpenComputers to be installed

## Example Programs

See `/docs/opencomputers/` for example programs:
- `pattern_generator.lua` - Interactive pattern builder
- `pattern_browser.lua` - GUI pattern library
- `preview_renderer.lua` - 3D pattern visualizer
- `robot_builder.lua` - Automated building with robots
