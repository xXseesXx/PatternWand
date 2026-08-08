metadata = {
    name = "Noise: 3D Perlin",
    author = "PatternWand",
    parameters = {
        scale = {type = "float", default = 0.15, min = 0.01, max = 1.0}
    }
}

-- Showcases: noise.perlin3d()
-- 3D Perlin noise for volumetric patterns
-- Useful for caves, veins, or 3D textures
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local n = noise.perlin3d(x * params.scale, y * params.scale, z * params.scale)
    
    -- Map noise to palette
    local index = util.floor((n + 1) * 0.5 * (palette.size() - 1))
    return index
end
