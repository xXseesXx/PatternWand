# Lua API Wrapper System

## Overview

The Pattern Wand mod exposes Java functionality to Lua scripts through a custom wrapper system. This document explains how the Java-to-Lua bridge works and how to use the exposed APIs.

## Architecture

### The Problem

LuaJ (the Lua interpreter for Java) needs Java objects to be properly exposed to Lua scripts. Simply passing Java objects doesn't always work because:

1. **Method Resolution**: Lua doesn't understand Java method overloading
2. **Type Coercion**: Java types need explicit conversion to Lua types
3. **Performance**: Direct Java method calls from Lua have overhead
4. **Error Handling**: Java exceptions need to be caught and translated to Lua errors

### The Solution

We use **wrapper classes** that create Lua tables with functions that call the underlying Java API:

```
Java API ──> Wrapper ──> Lua Table ──> Lua Script
(NoiseAPI)   (LuaNoiseWrapper)  (noise.perlin)  (script calls)
```

## Wrapper Classes

### 1. `LuaNoiseWrapper`

Wraps `NoiseAPI` to expose noise generation functions.

**Java Implementation:**
```java
public class LuaNoiseWrapper {
    public static LuaTable wrap(final NoiseAPI api) {
        LuaTable table = new LuaTable();
        
        table.set("perlin", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue x, LuaValue z) {
                return LuaValue.valueOf(
                    api.perlin(x.checkdouble(), z.checkdouble())
                );
            }
        });
        // ... more functions
        return table;
    }
}
```

**Lua Usage:**
```lua
-- All functions available on the noise object
local value = noise.perlin(x * 0.05, z * 0.05)
local value3d = noise.perlin3d(x, y, z)
local s = noise.simplex(x, z)
local s3d = noise.simplex3d(x, y, z)
```

**Available Functions:**
- `perlin(x, z)` - 2D Perlin noise, returns [-1, 1]
- `perlin3d(x, y, z)` - 3D Perlin noise, returns [-1, 1]
- `simplex(x, z)` - 2D Simplex noise, returns [-1, 1]
- `simplex3d(x, y, z)` - 3D Simplex noise, returns [-1, 1]

---

### 2. `LuaPaletteWrapper`

Wraps `PaletteAPI` to expose palette inventory functions.

**Important:** Palette indices are **0-based** (0-26), matching Java conventions.

**Java Implementation:**
```java
public class LuaPaletteWrapper {
    public static LuaTable wrap(final PaletteAPI api) {
        LuaTable table = new LuaTable();
        
        table.set("size", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(api.size());
            }
        });
        // ... more functions
        return table;
    }
}
```

**Lua Usage:**
```lua
-- Check how many slots exist (always 27)
local total = palette.size()

-- Get weight (stack size) of slot 0
local weight = palette.getWeight(0)

-- Check if a slot is empty
if palette.isEmpty(5) then
    -- slot 5 is empty
end

-- Pick a random slot weighted by stack size
local index = palette.pickWeighted()

-- Count non-empty slots
local count = palette.countNonEmpty()
```

**Available Functions:**
- `size()` - Returns 27 (number of palette slots)
- `getWeight(index)` - Returns stack size (1-64) or 0 if empty
- `isEmpty(index)` - Returns true if slot is empty
- `pickWeighted()` - Returns random index weighted by stack sizes
- `countNonEmpty()` - Returns count of non-empty slots

---

### 3. `LuaUtilWrapper`

Wraps `UtilAPI` to expose utility/math functions.

**Java Implementation:**
```java
public class LuaUtilWrapper {
    public static LuaTable wrap(final UtilAPI api) {
        LuaTable table = new LuaTable();
        
        table.set("hash", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue x, LuaValue z) {
                return LuaValue.valueOf(
                    api.hash(x.checkdouble(), z.checkdouble())
                );
            }
        });
        // ... more functions
        return table;
    }
}
```

**Lua Usage:**
```lua
-- Deterministic hash (pseudorandom without noise)
local h = util.hash(x, z)
local h3 = util.hash3d(x, y, z)

-- Distance functions
local dist = util.distance(x1, y1, x2, y2)
local manhattan = util.manhattan(x1, y1, x2, y2)

-- Mapping and clamping
local mapped = util.map(value, -1, 1, 0, 10)  -- map [-1,1] to [0,10]
local clamped = util.clamp(value, 0, 1)       -- clamp to [0,1]

-- Interpolation
local interpolated = util.lerp(a, b, 0.5)     -- 50% between a and b

-- Rounding
local down = util.floor(3.7)  -- 3
local up = util.ceil(3.2)     -- 4

-- Absolute value
local positive = util.abs(-5)  -- 5
```

**Available Functions:**
- `hash(x, z)` - Deterministic 2D hash
- `hash3d(x, y, z)` - Deterministic 3D hash
- `distance(x1, y1, x2, y2)` - Euclidean distance
- `manhattan(x1, y1, x2, y2)` - Manhattan distance
- `map(value, inMin, inMax, outMin, outMax)` - Map value between ranges
- `clamp(value, min, max)` - Clamp value to range
- `lerp(a, b, t)` - Linear interpolation (t in [0,1])
- `floor(value)` - Round down
- `ceil(value)` - Round up
- `abs(value)` - Absolute value

---

## How the Script Engine Uses Wrappers

The `ScriptEngine` creates the API wrappers when executing a pattern:

```java
public int executePattern(...) {
    // 1. Create Java API objects
    NoiseAPI noise = new NoiseAPI(seed);
    PaletteAPI palette = new PaletteAPI(paletteInventory, seed);
    UtilAPI util = new UtilAPI();
    
    // 2. Wrap them in Lua-friendly tables
    LuaTable luaNoise = LuaNoiseWrapper.wrap(noise);
    LuaTable luaPalette = LuaPaletteWrapper.wrap(palette);
    LuaTable luaUtil = LuaUtilWrapper.wrap(util);
    
    // 3. Pass them to the Lua pattern function
    LuaValue result = script.function.invoke(
        new LuaValue[] {
            LuaValue.valueOf(x),
            LuaValue.valueOf(y),
            LuaValue.valueOf(z),
            LuaValue.valueOf(relX),
            LuaValue.valueOf(relY),
            LuaValue.valueOf(relZ),
            luaPalette,     // Lua table
            luaNoise,       // Lua table
            luaUtil,        // Lua table
            LuaValue.valueOf(seed)
        }
    ).arg1();
    
    return result.toint();
}
```

## Pattern Function Signature

Every pattern script must return a function with this signature:

```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- x, y, z: world coordinates (numbers)
    -- relX, relY, relZ: relative coordinates from pattern origin (numbers)
    -- palette: table with palette functions
    -- noise: table with noise functions
    -- util: table with utility functions
    -- seed: random seed (number)
    
    return 0  -- palette index (0-26) or nil for gap
end

return pattern
```

## Example Patterns

### Simple Checkerboard
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    if (relX + relZ) % 2 == 0 then
        return 0
    else
        return 1
    end
end
return pattern
```

### Noise-Based Terrain
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    local value = noise.perlin(x * 0.05, z * 0.05)
    
    if value > 0.3 then
        return 0  -- Stone
    elseif value > 0 then
        return 1  -- Cobblestone
    else
        return palette.pickWeighted()  -- Random from palette
    end
end
return pattern
```

### Distance-Based Spiral
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    local dist = util.distance(relX, relZ, 0, 0)
    local angle = math.atan2(relZ, relX)
    
    local spiral = (dist + angle * 3) % palette.countNonEmpty()
    return util.floor(spiral)
end
return pattern
```

## Benefits of This Approach

1. **Type Safety**: Wrapper functions handle type conversion explicitly
2. **Performance**: LuaJ can optimize table lookups better than Java reflection
3. **Error Messages**: Better error messages when Lua calls fail
4. **Flexibility**: Easy to add new functions without changing Java APIs
5. **Documentation**: Wrapper code serves as implementation documentation
6. **Compatibility**: Existing scripts continue to work without changes

## Adding New API Functions

To add a new function to an API:

1. **Add method to Java API class:**
   ```java
   // In NoiseAPI.java
   public double fbm(double x, double z, int octaves) {
       // Implementation
   }
   ```

2. **Add wrapper in wrapper class:**
   ```java
   // In LuaNoiseWrapper.java
   table.set("fbm", new ThreeArgFunction() {
       @Override
       public LuaValue call(LuaValue x, LuaValue z, LuaValue octaves) {
           return LuaValue.valueOf(
               api.fbm(
                   x.checkdouble(),
                   z.checkdouble(),
                   octaves.checkint()
               )
           );
       }
   });
   ```

3. **Use in Lua:**
   ```lua
   local value = noise.fbm(x, z, 4)
   ```

## LuaJ Function Types

The wrappers use different LuaJ function base classes depending on argument count:

- `ZeroArgFunction` - No arguments (e.g., `palette.size()`)
- `OneArgFunction` - 1 argument (e.g., `util.abs(x)`)
- `TwoArgFunction` - 2 arguments (e.g., `noise.perlin(x, z)`)
- `ThreeArgFunction` - 3 arguments (e.g., `noise.perlin3d(x, y, z)`)
- `VarArgFunction` - Variable arguments (e.g., `util.map(...)`)

## Summary

The Lua API wrapper system provides a clean, performant bridge between Java and Lua. The three wrapper classes (`LuaNoiseWrapper`, `LuaPaletteWrapper`, `LuaUtilWrapper`) expose all necessary functionality for pattern scripts while maintaining type safety and good error handling.

All existing Lua pattern scripts work without modification, and the system is easily extensible for new features.
