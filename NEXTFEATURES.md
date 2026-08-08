# Next Features (High ROI)

This document outlines the next API improvements for PatternWand. The focus is on features that provide the largest increase in scripting power while remaining simple, safe, and fully backward compatible.

---

## 1. Pattern Metadata & Parameters ⭐⭐⭐⭐⭐

Allow patterns to expose configurable values instead of requiring users to edit Lua files.

### Example

```lua
metadata = {
    name = "Brick Wall",
    author = "PatternWand",
    parameters = {
        {name="brickWidth", type="number", default=4, min=2, max=8},
        {name="weathered", type="boolean", default=true}
    }
}
```

Pattern function:

```lua
function pattern(..., params)
    local width = params.brickWidth
end
```

### Benefits

- No Lua editing for common customization
- One pattern can support many variations
- Easier sharing
- Fully backward compatible

---

## 2. Placement Context ⭐⭐⭐⭐⭐

Expose existing placement information so patterns can react to how and where they are used.

### Context

```lua
context = {
    clickedX, clickedY, clickedZ,
    clickFace,

    minX, minY, minZ,
    maxX, maxY, maxZ,

    playerYaw,
    playerPitch,

    worldTime,
    dayTime
}
```

### Benefits

- Direction-aware patterns
- Centered gradients
- Automatic orientation
- Time-based variations

No new systems are required—only exposing data already available during placement.

---

## 3. Palette API Improvements ⭐⭐⭐⭐☆

Expand the existing palette helper functions.

### New Functions

```lua
palette.pickUniform()
palette.pickWeightedExcept(indices)
palette.pickWeightedRange(min, max)
```

### Benefits

- Less repetitive Lua
- Better random selection
- More flexible block choice
- Tiny implementation cost

---

## 4. Geometry & Math Utilities ⭐⭐⭐⭐☆

Provide common spatial helper functions.

### New Functions

```lua
util.distance3d(...)
util.inSphere(...)
util.inBox(...)
util.rotate2D(...)
util.mod(...)
util.sign(...)
util.smoothstep(...)
```

### Benefits

- Simpler scripts
- Less duplicated math
- Easier creation of geometric patterns
- Zero gameplay impact

---

## 5. Debug & Development Tools ⭐⭐⭐⭐☆

Improve the scripting workflow.

### API

```lua
debug.print(...)
```

### Commands

```
/patternwand debug on
/patternwand debug off
/patternwand profile
```

### Benefits

- Easier debugging
- Performance profiling
- Better development experience
- No impact on normal gameplay

---

# Design Goals

- Fully backward compatible
- Keep patterns deterministic
- Minimize API complexity
- Expose existing engine data where possible
- Prioritize usability over niche power features