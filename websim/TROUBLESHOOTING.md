# Troubleshooting Guide

## Nothing renders / Blank canvas

### Step 1: Run Diagnostics
Open `test.html` in your browser to verify all components load correctly.

### Step 2: Check Browser Console
1. Press `F12` to open Developer Tools
2. Go to "Console" tab
3. Look for errors (red text)

**Common errors:**
- `fengari is not defined` → CDN failed, check internet connection
- `pattern function not defined` → Lua syntax error
- `SimplexNoise is not defined` → CDN issue, but should work anyway

### Step 3: Verify Pattern Syntax
Make sure your pattern has the correct function signature:

```lua
function pattern(x, y, z, relX, relY, relZ)
    return 0  -- Must return a number 0-26
end
```

### Step 4: Check Return Values
Pattern must return:
- A number between 0 and 26 (palette index)
- OR `nil` to skip placing a block

**BAD:**
```lua
return  -- Returns nil, nothing renders
```

**GOOD:**
```lua
return 0  -- Returns palette index 0
```

## Errors in console

### "pattern function not defined"
Your Lua code has a syntax error. Check:
- Missing `end` statements
- Typos in function name
- Invalid Lua syntax

### "attempt to call a nil value"
You're calling an API function that doesn't exist. Check:
- Function name spelling
- API is available (e.g., `palette.`, `noise.`, `util.`)

### "bad argument"
You're passing wrong type to a function:
```lua
-- BAD
util.abs("hello")  -- Can't abs a string

-- GOOD
util.abs(-5)  -- OK
```

## Preview shows wrong colors

### Check palette setup
1. Scroll down to "📦 Palette" section
2. Verify colors are what you expect
3. Pattern returns indices 0-26
4. Index must match your palette slot

### Debug palette indices
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local idx = (x + z) % 2
    debug.print("Position", x, z, "returns", idx)
    return idx
end
```

Then check the debug console.

## Pattern works but looks wrong

### World coordinates vs Relative
The simulator uses:
- **World coordinates**: `x, y, z` (absolute, starts at 0,0,64)
- **Relative coordinates**: `relX, relY, relZ` (relative to clicked position 0,0,0)

```lua
-- Using world coordinates
function pattern(x, y, z)
    return (x + z) % 2  -- Fixed pattern based on world position
end

-- Using relative coordinates  
function pattern(x, y, z, relX, relY, relZ)
    return (relX + relZ) % 2  -- Pattern relative to click position
end
```

For most patterns, use **relative coordinates** (`relX`, `relZ`).

### 2D vs 3D
The simulator shows a 2D slice at Y=64. If your pattern depends on Y coordinate:

```lua
-- This won't show variation in 2D
function pattern(x, y, z)
    return y % 3  -- All blocks are at Y=64, so always same result
end

-- Use X and Z instead
function pattern(x, y, z)
    return (x + z) % 3  -- Works in 2D
end
```

## Noise doesn't work

### Check noise function usage
```lua
-- Correct
local n = noise.perlin(x * 0.1, z * 0.1)

-- Wrong - too many arguments for 2D
local n = noise.perlin(x, y, z)  -- Use perlin3d instead
```

### Scale your coordinates
Noise needs small values for smooth patterns:

```lua
-- Bad - too noisy
local n = noise.perlin(x, z)  -- Values too large

-- Good - smooth
local n = noise.perlin(x * 0.05, z * 0.05)  -- Scale down
```

### Map noise to palette indices
Noise returns [-1, 1], map to [0, 26]:

```lua
local n = noise.perlin(x * 0.1, z * 0.1)

-- Map to 0-26
local idx = math.floor((n + 1) * 13.5)
return idx
```

## Parameters don't appear

### Check metadata syntax
```lua
-- Correct
metadata = {
    parameters = {
        size = {type = "integer", default = 4, min = 2, max = 8}
    }
}

-- Wrong - typo
metadata = {
    paramters = {  -- Typo!
        size = {type = "integer", default = 4}
    }
}
```

### Use params in function
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
    local s = params.size or 4  -- Use params.parameterName
    return (relX // s) % 2
end
```

## Performance issues / Slow rendering

### Reduce grid size
- Use size slider to reduce from 128 to 32 or 16
- Smaller grids render faster

### Simplify pattern logic
```lua
-- Slow - calls function 1000 times
function pattern(x, y, z)
    for i = 1, 1000 do
        -- expensive computation
    end
    return 0
end

-- Fast - minimal computation
function pattern(x, y, z)
    return (x + z) % 2
end
```

## Palette weights don't work

### Check you're using weighted functions
```lua
-- Uses weights
return palette.pickWeighted()

-- Ignores weights (equal probability)
return palette.pickUniform()

-- Direct index (ignores weights)
return 0
```

### Set weights properly
- Click the number below each color
- Enter value 0-64
- 0 = empty slot
- Higher = more likely to be picked

## Can't see debug output

### Use debug.print() correctly
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    if relX == 0 and relZ == 0 then
        debug.print("Center block!")
    end
    return 0
end
```

Debug console appears automatically when `debug.print()` is used.

## SimplexNoise error

SimplexNoise may fail to load from CDN. The simulator automatically falls back to Perlin noise:

```lua
-- Both work even if SimplexNoise fails
local n1 = noise.simplex(x * 0.1, z * 0.1)  -- Falls back to perlin
local n2 = noise.perlin(x * 0.1, z * 0.1)   -- Always works
```

## Still not working?

### Create minimal test case
```lua
function pattern(x, y, z)
    return 0
end
```

If this doesn't show all black blocks, there's a fundamental issue.

### Check test.html
Open `test.html` to run full diagnostics. All tests should pass.

### Browser compatibility
- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Edge 90+
- ✅ Safari 14+
- ❌ Internet Explorer (not supported)

### Clear browser cache
- Chrome: `Ctrl+Shift+Delete`
- Firefox: `Ctrl+Shift+Delete`
- Clear "Cached images and files"

### Try local server
```bash
cd websim
python3 -m http.server 8000
# Open http://localhost:8000
```

## Getting Help

If none of the above helps:
1. Open `test.html` and screenshot results
2. Open browser console (F12) and screenshot errors
3. Copy your pattern code
4. Report issue with all screenshots + code
