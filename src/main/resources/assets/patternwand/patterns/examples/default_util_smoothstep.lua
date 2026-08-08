metadata = {
    name = "Util: Smoothstep",
    author = "PatternWand",
    parameters = {
        innerRadius = {type = "float", default = 5.0, min = 1.0, max = 20.0},
        outerRadius = {type = "float", default = 15.0, min = 5.0, max = 50.0}
    }
}

-- Showcases: util.smoothstep()
-- Creates smooth gradient transition between two radii
-- Smoothstep provides better-looking gradients than linear interpolation
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local dist = util.distance3d(relX, relY, relZ, 0, 0, 0)
    
    -- Smooth transition from inner to outer radius
    local t = util.smoothstep(params.innerRadius, params.outerRadius, dist)
    
    -- Map to palette
    local index = util.floor(t * (palette.size() - 1))
    return index
end
