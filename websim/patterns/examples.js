// Simple test patterns - each tests ONE feature at a time
const EXAMPLES = {
    test_basic: `-- Test 1: Basic Pattern (no APIs)
-- Should show solid color
function pattern(x, y, z)
    return 0
end`,

    test_coordinates: `-- Test 2: World Coordinates
-- Should show vertical stripes
function pattern(x, y, z)
    return x % 3
end`,

    test_relative: `-- Test 3: Relative Coordinates
-- Should show vertical stripes starting from 0
function pattern(x, y, z, relX, relY, relZ)
    return relX % 3
end`,

    test_math: `-- Test 4: Lua Math Library
-- Should show diagonal pattern
function pattern(x, y, z)
    return math.floor(x / 4) % 2
end`,

    test_math_mod: `-- Test 5: Lua Modulo
-- Should show checkerboard
function pattern(x, y, z)
    return (x % 2) + (z % 2) * 2
end`,

    test_palette_size: `-- Test 6: Palette API - size()
-- Should show number (27)
function pattern(x, y, z, relX, relY, relZ, palette)
    if relX == 0 and relZ == 0 then
        return palette.size() % 27
    end
    return 0
end`,

    test_palette_count: `-- Test 7: Palette API - countNonEmpty()
-- First block shows count, rest black
function pattern(x, y, z, relX, relY, relZ, palette)
    if relX == 0 and relZ == 0 then
        return palette.countNonEmpty() % 27
    end
    return 0
end`,

    test_palette_weighted: `-- Test 8: Palette API - pickWeighted()
-- Should show random colors based on weights
function pattern(x, y, z, relX, relY, relZ, palette)
    return palette.pickWeighted()
end`,

    test_palette_uniform: `-- Test 9: Palette API - pickUniform()
-- Should show random colors (equal probability)
function pattern(x, y, z, relX, relY, relZ, palette)
    return palette.pickUniform()
end`,

    test_util_floor: `-- Test 10: Util API - floor()
-- Should show horizontal stripes
function pattern(x, y, z, relX, relY, relZ, palette, noise, util)
    return util.floor(relZ / 4) % 3
end`,

    test_util_mod: `-- Test 11: Util API - mod()
-- Should show checkerboard
function pattern(x, y, z, relX, relY, relZ, palette, noise, util)
    return util.mod(relX + relZ, 2)
end`,

    test_util_clamp: `-- Test 12: Util API - clamp()
-- Should show gradient that stops at edges
function pattern(x, y, z, relX, relY, relZ, palette, noise, util)
    local value = relX - 10
    return util.clamp(value, 0, 10)
end`,

    test_util_distance: `-- Test 13: Util API - distance()
-- Should show circular gradient from center
function pattern(x, y, z, relX, relY, relZ, palette, noise, util)
    local dist = util.distance(relX, relZ, 16, 16)
    return util.floor(dist) % 10
end`,

    test_noise_perlin: `-- Test 14: Noise API - perlin()
-- Should show organic Perlin noise pattern
function pattern(x, y, z, relX, relY, relZ, palette, noise)
    local n = noise.perlin(x * 0.1, z * 0.1)
    -- Map from [-1, 1] to [0, 2]
    if n > 0.3 then
        return 0
    elseif n > -0.3 then
        return 1
    else
        return 2
    end
end`,

    test_noise_simplex: `-- Test 15: Noise API - simplex()
-- Should show organic Simplex noise pattern
function pattern(x, y, z, relX, relY, relZ, palette, noise)
    local n = noise.simplex(x * 0.1, z * 0.1)
    -- Map from [-1, 1] to [0, 2]
    if n > 0.3 then
        return 0
    elseif n > -0.3 then
        return 1
    else
        return 2
    end
end`,

    test_param_integer: `-- Test 16: Parameters - Integer
metadata = {
    parameters = {
        stripeWidth = {type = "integer", default = 4, min = 2, max = 8}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
    local width = params.stripeWidth or 4
    return util.floor(relX / width) % 2
end`,

    test_param_float: `-- Test 17: Parameters - Float
metadata = {
    parameters = {
        scale = {type = "float", default = 0.1, min = 0.01, max = 1.0}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
    local scale = params.scale or 0.1
    local value = relX * scale
    return util.floor(value) % 3
end`,

    test_param_boolean: `-- Test 18: Parameters - Boolean
metadata = {
    parameters = {
        inverted = {type = "boolean", default = false}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
    local result = util.mod(relX + relZ, 2)
    if params.inverted then
        result = 1 - result
    end
    return result
end`,

    test_debug: `-- Test 19: Debug API
-- Check console for debug output
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    if relX == 0 and relZ == 0 then
        debug.print("=== Pattern Debug Test ===")
        debug.print("Position:", x, y, z)
        debug.print("Palette size:", palette.size())
        debug.print("Test successful!")
    end
    return util.mod(x + z, 2)
end`,

    test_context: `-- Test 20: Context API
-- Shows clicked position info
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    if relX == 0 and relZ == 0 then
        debug.print("Clicked at:", context.clickedX, context.clickedY, context.clickedZ)
        debug.print("Grid bounds:", context.minX, context.minZ, "to", context.maxX, context.maxZ)
    end
    -- Mark origin with different color
    if x == context.clickedX and z == context.clickedZ then
        return 1
    end
    return 0
end`
};
