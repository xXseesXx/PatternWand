# Pattern API Signature Fix

## Issue
Old patterns (fiber3.lua, fiber32.lua) were failing with error:
```
bad argument: userdata expected, got number
```

## Cause
The old patterns used an outdated function signature:
```lua
function pattern(x,y,z,relX,relY,relZ,palette,noise,seed)
```

But the current ScriptEngine passes arguments in this order:
```lua
function pattern(x,y,z,relX,relY,relZ,palette,noise,util,seed)
```

The `util` parameter was added to provide hash and distance functions, but old patterns didn't have it. This caused all arguments after `noise` to be shifted:
- What the script thought was `palette` was actually `noise`
- What the script thought was `noise` was actually `util`  
- What the script thought was `seed` was actually the real `seed`

When the pattern called `palette.size()`, it was actually calling `noise.size()`, which doesn't exist, causing the error.

## Fix
Updated both fiber patterns to include the `util` parameter in their function signature:

**Before:**
```lua
function pattern(x,y,z,relX,relY,relZ,palette,noise,seed)
```

**After:**
```lua
function pattern(x,y,z,relX,relY,relZ,palette,noise,util,seed)
```

## Standard Pattern Function Signature
All patterns must use this signature:

```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- x, y, z: World coordinates
    -- relX, relY, relZ: Relative coordinates from click origin
    -- palette: PaletteAPI object
    -- noise: NoiseAPI object  
    -- util: UtilAPI object
    -- seed: Random seed (long)
    
    return 0  -- Return palette index (0-26) or nil for gap
end

return pattern
```

## Testing
After fix, patterns should work correctly:
1. `/patternwand set fiber3` or `/patternwand set checkerboard`
2. Use the wand to place blocks
3. Pattern should execute without errors

All 10 new example patterns already use the correct signature.
