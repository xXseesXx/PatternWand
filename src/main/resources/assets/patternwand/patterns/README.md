# Pattern Scripts

This directory contains Lua-based pattern scripts for the Pattern Wand.

## How to Use

1. Place your .lua pattern scripts in this directory
2. Scripts will be loaded automatically when the game starts
3. Use the Pattern Wand to apply patterns in-game

## Pattern Function Signature

Every pattern script must define a function with this signature:

```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Your pattern logic here
    return 0  -- Return palette index (0-26) or nil for gap
end

return pattern  -- Must return the function
```

### Parameters

- `x, y, z` - World coordinates (integers)
- `relX, relY, relZ` - Coordinates relative to pattern origin (0-based)
- `palette` - Palette API object
- `noise` - Noise generation API
- `util` - Utility functions
- `seed` - Pattern-specific seed for randomness

### Return Value

- **Number (0-26)**: Place block from this palette slot
- **nil**: Don't place a block (gap)

## Available APIs

### Noise API (`noise`)

Generate natural-looking patterns:

```lua
noise.perlin(x, z)           -- 2D Perlin noise [-1, 1]
noise.perlin3d(x, y, z)      -- 3D Perlin noise [-1, 1]
noise.simplex(x, z)          -- 2D Simplex noise [-1, 1]
noise.simplex3d(x, y, z)     -- 3D Simplex noise [-1, 1]
```

### Palette API (`palette`)

Access palette information:

```lua
palette.size()               -- Number of slots (always 27)
palette.getWeight(index)     -- Get stack size of slot
palette.isEmpty(index)       -- Check if slot is empty
palette.pickWeighted()       -- Random slot by stack size weight
palette.countNonEmpty()      -- Count non-empty slots
```

### Utility API (`util`)

Helper functions:

```lua
util.hash(x, z)              -- Deterministic hash
util.hash3d(x, y, z)         -- 3D hash
util.distance(x1, y1, x2, y2) -- Euclidean distance
util.manhattan(x1, y1, x2, y2) -- Manhattan distance
util.map(value, inMin, inMax, outMin, outMax) -- Map range
util.clamp(value, min, max)  -- Clamp to range
util.lerp(a, b, t)           -- Linear interpolation
util.floor(value)            -- Round down
util.ceil(value)             -- Round up
util.abs(value)              -- Absolute value
```

### Standard Lua Math

All standard math functions available:

```lua
math.sin(x)   math.cos(x)   math.tan(x)
math.floor(x) math.ceil(x)  math.abs(x)
math.min(x, y) math.max(x, y)
math.sqrt(x)  math.pow(x, y)
math.random() math.random(n) math.random(m, n)
math.pi
-- And more...
```

## Example Patterns

Check the `examples/` directory for sample patterns:

- `checkerboard.lua` - Simple alternating pattern
- `noise_terrain.lua` - Natural stone mix using Perlin noise
- `bricks.lua` - Classic brick wall pattern
- `ripples.lua` - Circular ripple effect
- `gradient.lua` - Smooth gradient transition

## Tips

1. **Scale coordinates** for noise: `noise.perlin(x * 0.1, z * 0.1)` gives smoother results
2. **Use modulo** for repeating patterns: `relX % 4`
3. **Combine techniques**: Mix noise with math for unique effects
4. **Test incrementally**: Start simple, add complexity gradually
5. **Use comments**: Document your palette requirements

## Troubleshooting

- **Script won't load**: Check syntax, ensure `return pattern` at end
- **Wrong blocks placed**: Verify palette indices match your slots
- **Pattern too slow**: Reduce noise calls, simplify calculations
- **Timeout error**: Script is too complex, simplify logic

## Performance

- Scripts have a 10-second timeout per block
- Keep calculations simple for large builds
- Cache repeated calculations in local variables
- Avoid nested loops when possible

---

Happy building! 🏗️
