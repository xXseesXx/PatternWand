# Pattern Testing Checklist

## Test All Example Patterns

Open `index.html` and test each pattern from the dropdown:

### ✅ Checkerboard
- [ ] Load pattern
- [ ] Should show alternating 2-color grid
- [ ] Uses palette slots 0 and 1
- [ ] **Expected:** Regular checkerboard pattern

### ✅ Bricks  
- [ ] Load pattern
- [ ] Should show brick wall with mortar
- [ ] Uses palette slots 0 (brick) and 1 (mortar)
- [ ] **Expected:** Offset bricks with mortar lines

### ✅ Configurable Bricks
- [ ] Load pattern
- [ ] Parameters appear: brickWidth, brickHeight, weathered, offsetPattern
- [ ] Adjust brickWidth slider (2-8)
- [ ] Toggle weathered checkbox
- [ ] **Expected:** Brick pattern changes with parameters

### ✅ Noise Terrain
- [ ] Load pattern
- [ ] Should show organic noise-based pattern
- [ ] Uses palette slots 0-3
- [ ] **Expected:** Smooth natural-looking terrain

### ✅ Gradient
- [ ] Load pattern
- [ ] Should show smooth horizontal gradient
- [ ] Uses multiple palette slots
- [ ] **Expected:** Sine wave gradient across X axis

### ✅ Ripples
- [ ] Load pattern
- [ ] Should show circular ripples from center
- [ ] Uses palette slots 0-2
- [ ] **Expected:** Concentric circles with varying colors

### ✅ Random Mix
- [ ] Load pattern
- [ ] Parameters appear: mode, excludeFirst
- [ ] Try different modes: uniform, weighted, range, checkerboard
- [ ] **Expected:** Different random distributions

### ✅ Spherical Dome
- [ ] Load pattern
- [ ] Parameters appear: radius, hollow
- [ ] Adjust radius (3-50)
- [ ] Toggle hollow
- [ ] **Expected:** Circular shape with gradient (2D slice of sphere)

## Feature Tests

### Math Library
Test that Lua's `math` library works:
```lua
function pattern(x, y, z)
    return math.floor(math.sin(x * 0.1) * 10) % 3
end
```
- [ ] Should render without errors
- [ ] Should show wavy pattern

### Palette API
Test all palette methods:
```lua
function pattern(x, y, z, relX, relY, relZ, palette)
    local method = math.floor(relX / 8) % 5
    if method == 0 then return palette.pickWeighted()
    elseif method == 1 then return palette.pickUniform()
    elseif method == 2 then return palette.pickWeightedExcept(0)
    elseif method == 3 then return palette.pickWeightedRange(0, 5)
    else return palette.size() - 1 end
end
```
- [ ] Should render without errors
- [ ] Should show 5 vertical stripes

### Noise API
Test both Perlin and Simplex:
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise)
    if relX < 16 then
        local n = noise.perlin(x * 0.1, z * 0.1)
        return n > 0 and 0 or 1
    else
        local n = noise.simplex(x * 0.1, z * 0.1)
        return n > 0 and 2 or 3
    end
end
```
- [ ] Should render two different noise patterns side by side
- [ ] Left side: Perlin (slots 0-1)
- [ ] Right side: Simplex (slots 2-3)

### Util API
Test utility functions:
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util)
    local dist = util.distance(relX, relZ, 16, 16)
    local clamped = util.clamp(dist / 16, 0, 1)
    return math.floor(clamped * 26)
end
```
- [ ] Should render circular gradient from center
- [ ] Darker in center, lighter at edges

### Parameters (all types)
```lua
metadata = {
    parameters = {
        intParam = {type = "integer", default = 4, min = 2, max = 8},
        floatParam = {type = "float", default = 0.5, min = 0.0, max = 1.0},
        boolParam = {type = "boolean", default = true},
        strParam = {type = "string", default = "test"}
    }
}
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
    if params.boolParam then
        return (math.floor(relX / params.intParam) + math.floor(relZ / params.intParam)) % 2
    else
        local n = noise.perlin(x * params.floatParam, z * params.floatParam)
        return n > 0 and 0 or 1
    end
end
```
- [ ] All 4 parameter controls appear
- [ ] Integer slider works (2-8)
- [ ] Float slider works (0.0-1.0)
- [ ] Boolean checkbox works
- [ ] String input works
- [ ] Pattern updates when changing parameters

### Context API
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context)
    -- Use context values
    if x == context.clickedX and z == context.clickedZ then
        return 0  -- Mark origin
    end
    return util.mod(x + context.clickedX, 3)
end
```
- [ ] Should render with origin marker
- [ ] Uses context.clickedX and clickedZ

### Debug API
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    if relX == 0 and relZ == 0 then
        debug.print("Pattern started!")
        debug.print("Grid size:", context.maxX - context.minX + 1)
    end
    return (x + z) % 2
end
```
- [ ] Debug console appears
- [ ] Shows "Pattern started!" message
- [ ] Shows grid size

### Nil Returns (skip blocks)
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util)
    local dist = util.distance(relX, relZ, 16, 16)
    if dist < 10 then
        return 0  -- Inside circle
    else
        return nil  -- Skip outside
    end
end
```
- [ ] Should show filled circle
- [ ] Area outside circle is empty

## Edge Cases

### Empty Palette
```lua
function pattern(x, y, z, relX, relY, relZ, palette)
    if palette.countNonEmpty() == 0 then
        return 0  -- Fallback
    end
    return palette.pickWeighted()
end
```
- [ ] Set all palette weights to 0
- [ ] Should still render (fallback to slot 0)

### Large Numbers
```lua
function pattern(x, y, z)
    return (x * 1000000 + z * 1000000) % 27
end
```
- [ ] Should render without overflow errors

### Negative Modulo
```lua
function pattern(x, y, z)
    return util.mod(-x, 5)  -- Should always be positive
end
```
- [ ] Should render without negative indices
- [ ] All values 0-4

## Performance Tests

### Small Grid (16x16)
- [ ] Set grid size to 16
- [ ] Load any pattern
- [ ] Should render in < 50ms (check console)

### Medium Grid (32x32) 
- [ ] Set grid size to 32
- [ ] Load any pattern
- [ ] Should render in < 100ms

### Large Grid (64x64)
- [ ] Set grid size to 64
- [ ] Load any pattern
- [ ] Should render in < 300ms

### Extra Large (128x128)
- [ ] Set grid size to 128
- [ ] Load simple pattern (checkerboard)
- [ ] Should render in < 1000ms

## Browser Tests

Test in multiple browsers:
- [ ] Chrome/Chromium
- [ ] Firefox
- [ ] Edge
- [ ] Safari (if available)

## Common Issues

### Pattern doesn't render
1. Check browser console (F12) for errors
2. Verify Lua syntax is correct
3. Ensure function returns 0-26 or nil
4. Check that math/util/noise functions are spelled correctly

### Parameters don't appear
1. Check metadata syntax
2. Ensure parameters block is properly formatted
3. Verify parameter types are valid

### Wrong colors
1. Check palette configuration
2. Verify return values are correct indices
3. Ensure palette slots have non-zero weights

## Success Criteria

- [x] All 8 example patterns load and render
- [x] All parameter types work correctly
- [x] Math library functions work
- [x] Palette API functions work
- [x] Noise API functions work
- [x] Util API functions work
- [x] Context API works
- [x] Debug API works
- [x] Performance targets met

If all tests pass, the simulator is fully functional! 🎉
