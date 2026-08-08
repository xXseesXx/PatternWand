# Quick Reference Card

## Opening the Simulator
1. Open `websim/index.html` in your browser
2. Wait for "Loading..." to disappear
3. Start coding!

## Basic Pattern Structure
```lua
function pattern(x, y, z, relX, relY, relZ)
    return 0  -- Palette index 0-26, or nil to skip
end
```

## Coordinate System
- **World coords**: `x, y, z` - Absolute positions (x=0-31, y=64, z=0-31)
- **Relative coords**: `relX, relY, relZ` - Relative to origin (0-31)
- **Use relative** for most patterns (position-independent)

## Return Values
```lua
return 0        -- Place block from palette slot 0
return 5        -- Place block from palette slot 5
return nil      -- Skip this position (no block)
```

## Using Palette
```lua
return palette.pickWeighted()     -- Random based on weights
return palette.pickUniform()      -- Random, ignore weights
return (x + z) % palette.size()   -- Cycle through palette
```

## Using Noise
```lua
-- Scale coordinates for smooth noise
local n = noise.perlin(x * 0.05, z * 0.05)  -- Returns -1 to 1
local n = noise.simplex(x * 0.1, z * 0.1)   -- Returns -1 to 1

-- Map to palette indices
local idx = math.floor((n + 1) * 13)  -- Maps -1..1 to 0..26
return idx
```

## Using Parameters
```lua
metadata = {
    parameters = {
        size = {type = "integer", default = 4, min = 2, max = 8}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
    return (relX // params.size) % 2
end
```

## Math Helpers
```lua
util.abs(-5)              -- Absolute value: 5
util.floor(3.7)           -- Round down: 3
util.ceil(3.2)            -- Round up: 4
util.mod(-1, 5)           -- Modulo (always positive): 4
util.clamp(10, 0, 5)      -- Clamp to range: 5

-- Distances
util.distance(x1, z1, x2, z2)           -- 2D distance
util.distance3d(x1, y1, z1, x2, y2, z2) -- 3D distance

-- Interpolation
util.lerp(0, 10, 0.5)            -- Linear interpolation: 5
util.smoothstep(0, 10, 5)        -- Smooth interpolation: 0.5
```

## Debugging
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    debug.print("Position:", x, z, "Value:", (x + z) % 2)
    return (x + z) % 2
end
```

## Common Patterns

### Checkerboard
```lua
function pattern(x, y, z)
    return (x + z) % 2
end
```

### Stripes
```lua
function pattern(x, y, z)
    return x % 3
end
```

### Random
```lua
function pattern(x, y, z, relX, relY, relZ, palette)
    return palette.pickWeighted()
end
```

### Noise Terrain
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise)
    local n = noise.perlin(x * 0.05, z * 0.05)
    return math.floor((n + 1) * 13)
end
```

### Circle
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util)
    local centerX = 16
    local centerZ = 16
    local radius = 10
    
    if util.distance(x, z, centerX, centerZ) < radius then
        return 0
    else
        return nil  -- Skip blocks outside circle
    end
end
```

### Gradient
```lua
function pattern(x, y, z, relX, relY, relZ)
    return relX % 27  -- Gradient across X axis
end
```

## UI Controls

### Palette
- Click colored squares to pick colors
- Type numbers below to set weights (0-64)
- Select preset from dropdown

### Parameters
- Sliders appear automatically from metadata
- Adjust in real-time
- Pattern updates instantly

### Canvas
- Hover to see coordinates and palette index
- Adjust grid size with slider (16-128)
- Smaller = faster rendering

### Debug Console
- Appears when using `debug.print()`
- Click "Clear" to reset
- Auto-scrolls to latest

## Tips
- Use **relative coordinates** (`relX`, `relZ`) for portable patterns
- **Scale noise** coordinates (multiply by 0.01-0.1)
- **Map noise** from [-1,1] to [0,26] for palette indices
- **Test small** grids first (16x16), then increase size
- **Use presets** as starting point for palettes
- **Check console** (F12) if something doesn't work

## Keyboard Shortcuts
- `Ctrl+A` in editor - Select all code
- `Ctrl+Z` - Undo
- `F12` - Open browser console for debugging

## Files
- `index.html` - Main simulator
- `test.html` - Run diagnostics
- `README.md` - Full documentation
- `TROUBLESHOOTING.md` - Problem solving
- `CHANGELOG.md` - Recent changes
