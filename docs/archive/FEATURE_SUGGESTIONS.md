# PatternWand API Enhancement Research & Suggestions

## Current State Analysis

### Core Purpose
PatternWand is a Minecraft 1.7.10 mod that extends BetterBuildersWands with Lua-scriptable building patterns. It allows players to define complex procedural building patterns using Lua scripts that execute per-block during placement.

### Current Lua Pattern API

#### Function Signature
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    return paletteIndex  -- 0-26, or nil to skip
end
```

#### Parameters Exposed
1. **World Coordinates** (`x, y, z`) - Absolute block position
2. **Relative Coordinates** (`relX, relY, relZ`) - Position relative to clicked block
3. **Palette API** - Access to wand's 27-slot block palette
4. **Noise API** - Perlin and Simplex noise generators
5. **Util API** - Math helpers and hash functions
6. **Seed** - Pattern randomization seed (world or custom)

#### Current API Functions

**Palette API:**
- `size()` - Returns 27
- `getWeight(index)` - Stack size (1-64) at slot
- `isEmpty(index)` - Check if slot empty
- `pickWeighted()` - Random selection weighted by stack size
- `countNonEmpty()` - Count filled slots

**Noise API:**
- `perlin(x, z)` - 2D Perlin noise [-1, 1]
- `perlin3d(x, y, z)` - 3D Perlin noise [-1, 1]
- `simplex(x, z)` - 2D Simplex noise [-1, 1]
- `simplex3d(x, y, z)` - 3D Simplex noise [-1, 1]

**Util API:**
- `hash(x, z)` - Deterministic 2D hash
- `hash3d(x, y, z)` - Deterministic 3D hash
- `distance(x1, y1, x2, y2)` - Euclidean distance
- `manhattan(x1, y1, x2, y2)` - Manhattan distance
- `map(value, inMin, inMax, outMin, outMax)` - Range mapping
- `clamp(value, min, max)` - Value clamping
- `lerp(a, b, t)` - Linear interpolation
- `floor(value)` - Round down
- `ceil(value)` - Round up
- `abs(value)` - Absolute value

### Current Features
- Single unbreakable wand with 16384 block capacity
- 27-slot palette GUI (Shift+Right-Click)
- Pattern flood-fill matching (replaces blocks matching palette)
- Commands: reload, list, set, info, seed, clearseed
- Block preview highlighting
- Timeout protection (10 seconds per pattern execution)
- Custom seed support for deterministic patterns
- Stack-size weighted random selection

---

## Suggested Enhancements

### 1. **Context & State Information** ⭐⭐⭐

**Rationale:** Scripts currently lack awareness of the build context, limiting ability to create adaptive patterns.

#### Add to Pattern Parameters:

**Build Context:**
```lua
context = {
    -- Placement info
    clickedX, clickedY, clickedZ,     -- Original clicked position !!
    clickFace,                         -- Face clicked (0=down, 1=up, 2=north, 3=south, 4=west, 5=east) !!!
    totalBlocks,                       -- Total blocks in current fill area ! MAYBE ALSO MODIFY THE NUMBER OF BLOCKS THE PATTERN ITSELF PLACES OP TO WAND MAX OFCOURSE !!
    blockIndex,                        -- Current block index in placement order (0-based) !
    
    -- Area bounds (for flood-fill area)
    minX, minY, minZ,
    maxX, maxY, maxZ,
    
    -- Player info
    playerYaw,                         -- Player facing direction (degrees) !!
    playerPitch,                       -- Player pitch angle !!
    playerX, playerY, playerZ,         -- Player position !!
    
    -- Time info
    worldTime,                         -- Current world time (ticks) !
    dayTime,                          -- Time of day (0-24000) !!
}
```

**Use Cases:**
- Create patterns that fade at edges based on distance from center
- Different patterns based on placement direction
- Time-dependent patterns (day/night variations)
- Progressive patterns based on block placement order
- Player-relative directional patterns

---

### 2. **World Inspection API** ⭐⭐⭐

**Rationale:** Patterns cannot react to existing world blocks, limiting context-aware building.

#### Add World API:
```lua
world = {
    -- Block inspection (within reasonable range)
    getBlock(x, y, z),                 -- Get block ID at position !
    getMeta(x, y, z),                  -- Get metadata/damage value
    isAir(x, y, z),                    -- Check if air
    isSolid(x, y, z),                  -- Check if solid block
    isLiquid(x, y, z),                 -- Check if liquid
    
    -- Light levels
    getLight(x, y, z),                 -- Combined light (0-15) !
    getSkyLight(x, y, z),              -- Sky light level !
    getBlockLight(x, y, z),            -- Block light level !
    
    -- Biome info
    getBiomeId(x, z),                  -- Biome ID at position !!
    getBiomeTemp(x, z),                -- Biome temperature !!
    getBiomeRainfall(x, z),            -- Biome rainfall !!
    
    -- Height queries
    getTopY(x, z),                     -- Highest non-air block !
    canSeeSky(x, y, z),                -- Check if exposed to sky !
}
```

**Safety Considerations:**
- Restrict queries to placement area ± small buffer (e.g., ±16 blocks)
- Cache world queries to prevent performance issues
- Read-only access (no world modification outside pattern placement)

**Use Cases:**
- Adaptive patterns that blend with existing terrain
- Light-level based block selection (torches in dark areas)
- Biome-specific patterns
- Depth-based patterns (cave vs surface)
- Patterns that react to nearby structures

---

### 3. **Advanced Noise Functions** ⭐⭐

**Rationale:** Current noise is basic; many procedural patterns need advanced noise features.

#### Extend Noise API:
```lua
noise = {
    -- Existing
    perlin(x, z), perlin3d(x, y, z),
    simplex(x, z), simplex3d(x, y, z),
    
    -- Fractional Brownian Motion (fBm) for natural terrain !!
    fbm(x, z, octaves, lacunarity, gain),
    fbm3d(x, y, z, octaves, lacunarity, gain),
    
    -- Ridged noise (for mountains, veins) !!
    ridged(x, z, octaves),
    ridged3d(x, y, z, octaves),
    
    -- Turbulence (for chaotic patterns) !!
    turbulence(x, z, size),
    turbulence3d(x, y, z, size),
    
    -- Cellular/Worley noise (for organic patterns, tiles) !!
    cellular(x, z),
    cellular3d(x, y, z),
    
    -- Value noise (simpler, blockier than Perlin) !!
    value(x, z),
    value3d(x, y, z),
}
```

**Use Cases:**
- Natural terrain blending with fBm
- Mountain/ridge patterns
- Organic cell-like structures
- Ore vein patterns
- Multi-octave complexity

---

### 4. **Pattern Metadata & Variables** ⭐⭐⭐

**Rationale:** Patterns are currently static; no way to customize behavior without editing Lua.

#### Add Pattern Metadata System: !!!
```lua
-- In pattern file
metadata = {
    name = "Brick Wall",
    author = "PatternWand",
    description = "Classic brick pattern with mortar",
    version = "1.0",
    
    -- User-configurable parameters !!!
    parameters = {
        {name = "brickWidth", type = "number", default = 4, min = 2, max = 8},
        {name = "brickHeight", type = "number", default = 2, min = 1, max = 4},
        {name = "mortarThickness", type = "number", default = 1, min = 0, max = 2},
        {name = "useRunningBond", type = "boolean", default = true},
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
    local brickWidth = params.brickWidth
    local brickHeight = params.brickHeight
    -- Use parameters...
end
```

#### Commands to Set Parameters: !YES BUT INCLUDE IT IN THE SET COMMAND!!!
```
/patternwand param <key> <value>
/patternwand params                 -- List current params
/patternwand params reset           -- Reset to defaults
```

**Use Cases:**
- Customizable pattern scales without editing Lua
- Boolean toggles for pattern variations
- User-friendly parameter adjustment
- Pattern presets/configurations

---

### 5. **Palette Enhancements** ⭐⭐

**Rationale:** Current palette is basic; patterns lack fine control over block selection.

#### Extend Palette API:
```lua
palette = {
    -- Existing
    size(), getWeight(index), isEmpty(index), 
    pickWeighted(), countNonEmpty(),
    
    -- Enhanced selection
    pickWeightedExcept(excludeIndices),    -- Pick weighted, excluding indices !
    pickWeightedRange(minIdx, maxIdx),     -- Pick from range !
    pickUniform(),                          -- Random ignoring weights !
    
    -- Grouping (via GUI or convention)
    getGroup(groupName),                    -- Get indices in named group !
    pickFromGroup(groupName),               -- Random from group !
    
    -- Block info (requires world query capability)
    getBlockId(index),                      -- Get block ID at palette slot !
    getBlockMeta(index),                    -- Get metadata !
    getBlockName(index),                    -- Get unlocalized name !
}
```

#### Palette Groups (Convention):
Organize palette by leaving empty slots as separators:
```
Slots 0-8:   Primary blocks
Slot 9:      Empty (separator)
Slots 10-17: Accent blocks
Slot 18:     Empty (separator)
Slots 19-26: Detail blocks
```

**Use Cases:**
- Exclude specific blocks from random selection
- Semantic grouping (structure, detail, accent)
- More flexible randomization strategies

---

### 6. **Vector & Geometry Utilities** ⭐⭐

**Rationale:** Many patterns need spatial calculations; currently requires manual math.

#### Extend Util API:
```lua
util = {
    -- Existing math
    hash, hash3d, distance, manhattan, 
    map, clamp, lerp, floor, ceil, abs,
    
    -- Vector operations
    dot(x1, y1, z1, x2, y2, z2),       -- Dot product
    cross(x1, y1, z1, x2, y2, z2),     -- Cross product (returns 3 values)
    length(x, y, z),                    -- Vector magnitude
    normalize(x, y, z),                 -- Normalize vector (returns 3 values)
    
    -- Angles & rotations
    angleTo(x1, y1, x2, y2),           -- Angle between 2D points
    rotate2D(x, y, angle),              -- Rotate 2D point (returns 2 values)
    
    -- Geometry
    inBox(x, y, z, minX, minY, minZ, maxX, maxY, maxZ),
    inSphere(x, y, z, centerX, centerY, centerZ, radius),
    inCylinder(x, y, z, centerX, centerZ, radius),
    
    -- Advanced distance
    distance3d(x1, y1, z1, x2, y2, z2),
    manhattan3d(x1, y1, z1, x2, y2, z2),
    chebyshev(x1, y1, x2, y2),         -- Max of absolute differences
    
    -- Range helpers
    mod(a, b),                          -- Proper modulo (handles negatives)
    sign(value),                        -- -1, 0, or 1
    step(edge, value),                  -- 0 if value < edge, else 1
    smoothstep(edge0, edge1, value),    -- Smooth interpolation
}
```

**Use Cases:**
- Spherical/cylindrical patterns
- Rotational patterns
- Direction-based patterns
- Complex geometric shapes

---

### 7. **Multi-Pass Patterns** ⭐

**Rationale:** Some patterns need multiple layers (structure then details).

#### Add Multi-Pass System:
```lua
-- Pattern can define multiple passes
passes = {
    {name = "foundation", priority = 0},
    {name = "structure", priority = 1},
    {name = "details", priority = 2},
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, context, pass)
    if pass.name == "foundation" then
        -- Place foundation blocks
        return 0
    elseif pass.name == "structure" then
        -- Place main structure
        return 1
    elseif pass.name == "details" then
        -- Add decorative details
        if noise.simplex(x, z) > 0.5 then
            return 2
        end
    end
    return nil
end
```

**Implementation:**
- Execute pattern multiple times with different pass info
- Each pass can inspect blocks placed by previous passes
- Optional: only place if block wasn't placed in earlier pass

**Use Cases:**
- Layered construction (foundation → walls → details)
- Structure + decoration separation
- Conditional details based on main structure

---

### 8. **Pattern State & Memory** ⭐

**Rationale:** Complex patterns might need to maintain state across blocks.

#### Add Shared State Table:
```lua
state = {
    -- Persistent table shared across all blocks in current placement
    -- Reset at start of each wand use
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, context, state)
    -- Initialize counter on first block
    if state.blockCount == nil then
        state.blockCount = 0
    end
    
    state.blockCount = state.blockCount + 1
    
    -- Use state for patterns that need counters, sequences, etc.
    if state.blockCount % 10 == 0 then
        return 1  -- Every 10th block is different
    end
    return 0
end
```

**Safety:**
- Limit state table size (max entries, max memory)
- Clear state between placements
- Read-only access to other blocks' execution state

**Use Cases:**
- Sequential patterns
- Counting-based patterns
- Pattern memory/context
- Wave propagation patterns

---

### 9. **Debugging & Development Tools** ⭐⭐

**Rationale:** Debugging Lua patterns is currently difficult.

#### Add Debug API:
```lua
debug = {
    print(...),                         -- Log to console/chat
    mark(x, y, z, color),              -- Visualize point (client-side particle)
    time(),                             -- Pattern execution time (ms)
}
```

#### Commands:
```
/patternwand debug <on|off>            -- Enable verbose debug output
/patternwand profile <pattern>         -- Show performance stats
/patternwand test <pattern> <x> <y> <z> -- Test single block without placing
```

**Features:**
- Console logging for print statements
- Performance profiling per pattern
- Test execution without world modification
- Visual debugging with particles

---

### 10. **Performance & Optimization** ⭐⭐

**Rationale:** Large patterns can be slow; need optimization features.

#### Add Performance Features:

**Caching:**
```lua
cache = {
    -- Memoize expensive calculations
    remember(key, computeFunc),         -- Cache function result
    clear(),                            -- Clear cache
}
```

**Batching:**
- Pre-calculate noise samples for entire area
- Batch world queries
- Parallel script execution where possible

**Configuration:**
```
config/patternwand/config.cfg:
- maxExecutionTime (default 10s)
- maxBlocksPerPattern (default 16384)
- enableWorldQueries (default true)
- worldQueryRadius (default 16)
- enableCaching (default true)
- maxCacheSize (default 10000)
```

---

## Priority Implementation Order

### High Priority (Core Functionality)
1. ⭐⭐⭐ **Context & State Information** - Essential for adaptive patterns
2. ⭐⭐⭐ **World Inspection API** - Enables context-aware building
3. ⭐⭐⭐ **Pattern Metadata & Variables** - User-friendly customization

### Medium Priority (Enhancement)
4. ⭐⭐ **Palette Enhancements** - Better block selection control
5. ⭐⭐ **Vector & Geometry Utilities** - Common pattern needs
6. ⭐⭐ **Advanced Noise Functions** - Better procedural generation
7. ⭐⭐ **Debugging & Development Tools** - Improves pattern creation

### Low Priority (Advanced)
8. ⭐ **Multi-Pass Patterns** - Complex but niche use case
9. ⭐ **Pattern State & Memory** - Advanced patterns only
10. ⭐⭐ **Performance & Optimization** - Important but can be incremental

---

## Example Enhanced Pattern

```lua
-- Adaptive Brick Wall Pattern (using suggested features)

metadata = {
    name = "Adaptive Brick Wall",
    author = "PatternWand",
    description = "Brick pattern that adapts to light and terrain",
    parameters = {
        {name = "brickWidth", type = "number", default = 4, min = 2, max = 8},
        {name = "brickHeight", type = "number", default = 2, min = 1, max = 4},
        {name = "weathered", type = "boolean", default = true},
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, world)
    local brickWidth = params.brickWidth
    local brickHeight = params.brickHeight
    
    -- Calculate position within brick
    local brickX = relX % brickWidth
    local brickY = relY % brickHeight
    
    -- Offset alternate rows for running bond
    local rowOffset = math.floor(relY / brickHeight) % 2
    if rowOffset == 1 then
        brickX = (relX + math.floor(brickWidth / 2)) % brickWidth
    end
    
    -- Mortar on edges
    if brickX == 0 or brickY == 0 then
        return 1  -- Mortar
    end
    
    -- Main brick block
    local baseBlock = 0
    
    -- Add weathering based on light level (if world queries enabled)
    if params.weathered and world then
        local light = world.getLight(x, y, z)
        if light < 8 then
            -- Darker areas get mossy/cracked bricks
            if noise.simplex(x * 0.3, z * 0.3) > 0.3 then
                baseBlock = 2  -- Weathered brick variant
            end
        end
    end
    
    -- Occasional variation using noise
    if noise.simplex(x * 0.5, y * 0.5, z * 0.5) > 0.7 then
        return palette.pickWeightedRange(0, 3)  -- Pick from first few slots
    end
    
    return baseBlock
end
```

---

## Implementation Notes

### Backward Compatibility
- All existing patterns should continue working
- New parameters optional with sensible defaults
- Feature flags for expensive operations

### Security Considerations
- Sandbox Lua execution (already done)
- Limit world query range
- Timeout protection (already implemented)
- Resource limits on cache/state

### Performance Considerations
- Lazy evaluation where possible
- Caching for expensive operations
- Batch world queries
- Profile and optimize hot paths

### Documentation Needs
- API reference for all functions
- Example patterns for each feature
- Migration guide for existing patterns
- Performance best practices

---

## Conclusion

The current PatternWand API provides a solid foundation for scriptable building patterns. The suggested enhancements focus on:

1. **Context Awareness** - Patterns can react to placement context, world state, and player actions
2. **Flexibility** - User-configurable parameters without Lua editing
3. **Power** - Advanced noise, geometry, and world inspection capabilities
4. **Usability** - Better debugging tools and development experience
5. **Performance** - Optimization features for complex patterns

Priority should be given to features that maximize pattern capabilities while maintaining performance and security.
