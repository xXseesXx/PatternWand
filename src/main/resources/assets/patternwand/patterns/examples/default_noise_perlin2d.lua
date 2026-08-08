metadata = {
    name = "Noise: 2D Perlin",
    author = "PatternWand",
    parameters = {
        scale = {type = "float", default = 0.1, min = 0.01, max = 1.0}
    }
}

-- Showcases: noise.perlin()
-- 2D Perlin noise for natural-looking patterns
-- Returns values in range [-1, 1]
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local n = noise.perlin(x * params.scale, z * params.scale)
    
    -- Map noise [-1, 1] to palette indices [0, palette.size()-1]
    local index = util.floor((n + 1) * 0.5 * (palette.size() - 1))
    return index
end
