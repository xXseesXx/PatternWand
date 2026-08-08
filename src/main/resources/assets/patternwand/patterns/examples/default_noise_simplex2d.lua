metadata = {
    name = "Noise: 2D Simplex",
    author = "PatternWand",
    description = "Generates organic patterns using 2D Simplex noise, faster than Perlin with fewer artifacts",
    parameters = {
        scale = {type = "float", default = 0.1, min = 0.01, max = 1.0}
    }
}

-- Showcases: noise.simplex()
-- 2D Simplex noise - faster than Perlin with fewer artifacts
-- Better for performance-critical patterns
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local n = noise.simplex(x * params.scale, z * params.scale)
    
    -- Map noise to palette
    local index = util.floor((n + 1) * 0.5 * (palette.size() - 1))
    return index
end
