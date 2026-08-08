# OpenComputers Integration - Quick Reference

## Component Summary

### `patternwand_manager` (Pattern Management)
```lua
local pm = require("component").patternwand_manager

-- List all patterns
local patterns = pm.listPatterns()
-- Returns: {{name="checkerboard", author="...", ...}, ...}

-- Get pattern details
local info = pm.getPatternInfo("checkerboard")
-- Returns: {name, metadata, parameters={...}}

-- Reload patterns from disk
local success = pm.reload()

-- Validate pattern code without saving
local ok, err = pm.validatePattern(luaCode)

-- Write pattern to file
local ok, err = pm.writePattern("mypattern", luaCode)

-- Delete pattern
local ok = pm.deletePattern("mypattern")

-- Create preview
local preview = pm.createPreview("checkerboard", 16, 16, 1, {})
local index = preview.getBlock(0, 0, 0)  -- Returns palette index or -1
local color = preview.getPaletteColor(index)  -- Returns 0xRRGGBB
preview.close()
```

### `patternwand_controller` (Wand Control)
```lua
local pw = require("component").patternwand_controller

-- Get wand info (from inventory or adjacent)
local info = pw.getWandInfo()
-- Returns: {activePattern, seed, blocksRemaining, blocksCapacity, palette}

-- Set active pattern with parameters
local ok = pw.setPattern("bricks", {brickWidth=4, weathered=true})

-- Set custom seed
local ok = pw.setSeed(12345)

-- Clear seed (use world seed)
local ok = pw.clearSeed()

-- Get palette
local palette = pw.getPalette()
-- Returns: {{slotIndex=0, blockId="minecraft:stone", count=64}, ...}
```

## Example Use Cases

### 1. Pattern Browser
```lua
local pm = require("component").patternwand_manager
local patterns = pm.listPatterns()

print("Available Patterns:")
for i, p in ipairs(patterns) do
    print(string.format("%d. %s by %s", i, p.name, p.metadata.author or "Unknown"))
end
```

### 2. Preview Renderer (2D)
```lua
local pm = require("component").patternwand_manager
local gpu = require("component").gpu

local preview = pm.createPreview("noise_terrain", 32, 32, 1)

for y = 0, 31 do
    for x = 0, 31 do
        local idx = preview.getBlock(x, y, 0)
        if idx >= 0 then
            local color = preview.getPaletteColor(idx)
            gpu.setBackground(color)
            gpu.set(x + 1, y + 1, " ")
        end
    end
end

preview.close()
```

### 3. Procedural Pattern Generator
```lua
local pm = require("component").patternwand_manager

local function generateCheckerboard(size)
    return string.format([[
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    if (relX + relZ) %% %d == 0 then
        return 0
    else
        return 1
    end
end
return pattern
]], size * 2)
end

local code = generateCheckerboard(4)
local ok, err = pm.validatePattern(code)
if ok then
    pm.writePattern("auto_checkerboard", code)
    pm.reload()
    print("Pattern created!")
else
    print("Error: " .. err)
end
```

### 4. Robot Builder
```lua
local robot = require("robot")
local pw = require("component").patternwand_controller

-- Set pattern
pw.setPattern("bricks", {brickWidth=3})
pw.setSeed(999)

-- Build structure
for i = 1, 5 do
    robot.forward()
    robot.use()  -- Use pattern wand
    robot.turnRight()
end
```

## Energy Costs

| Operation | Energy Cost |
|-----------|-------------|
| listPatterns | 50 |
| getPatternInfo | 100 |
| reload | 500 |
| validatePattern | 200 |
| writePattern | 100 |
| deletePattern | 50 |
| createPreview | 1000 + (volume / 1000) |
| preview.getBlock | 1 |
| preview.getPaletteColor | 1 |
| getWandInfo | 50 |
| setPattern | 100 |
| setSeed | 50 |

## Configuration

In `config/patternwand.cfg`:

```
opencomputers {
    # Enable OpenComputers integration
    B:enabled=true
    
    # Allow OC programs to write pattern files
    B:allowFileWrite=true
    
    # Allow pattern preview generation
    B:allowPreview=true
    
    # Maximum preview volume (width * height * depth)
    I:maxPreviewVolume=262144
    
    # Energy cost multiplier
    D:energyCostMultiplier=1.0
}
```

## Security Notes

1. **File Access**: OC can only access `config/patternwand/patterns/`
2. **Example Protection**: Example patterns cannot be overwritten or deleted
3. **Path Validation**: All paths are sanitized to prevent directory traversal
4. **Resource Limits**: Preview size is limited to prevent memory exhaustion
5. **Energy Costs**: All operations consume energy to prevent abuse

## Common Issues

### "Component not available"
- OpenComputers is not installed
- Component is not adjacent to computer
- Component is out of range

### "Permission denied"
- File write is disabled in config
- Attempting to modify example pattern
- Invalid file path

### "Preview too large"
- Volume exceeds `maxPreviewVolume` config
- Reduce dimensions or adjust config

### "Insufficient energy"
- Computer/robot doesn't have enough energy
- Increase power supply or reduce operations

## Implementation Status

- [x] Design completed
- [x] Dependencies added
- [ ] Component framework (Phase 1)
- [ ] Pattern management (Phase 2)
- [ ] File operations (Phase 3)
- [ ] Preview system (Phase 4)
- [ ] Wand control (Phase 5)
- [ ] Configuration (Phase 6)
- [ ] Documentation (Phase 7)

Current version: **0.1.0-alpha** (design phase)
Target version: **1.0.0** (full implementation)

## Contributing

To contribute to OC integration:
1. Review design doc: `docs/OPENCOMPUTERS_INTEGRATION.md`
2. Check task list: `docs/OPENCOMPUTERS_TASKS.md`
3. Follow implementation phases
4. Add tests for new features
5. Update documentation

## License

OpenComputers integration follows PatternWand's LGPL-3.0 license.
OpenComputers is licensed under the MIT license.
