metadata = {
    name = "Util: Math Functions",
    author = "PatternWand"
}

-- Showcases: util.clamp(), util.lerp(), util.map()
-- Creates a gradient using various math utilities
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Distance from center
    local dist = util.distance3d(relX, relY, relZ, 0, 0, 0)
    
    -- Map distance to palette range
    -- Clamp to reasonable max distance
    local maxDist = 20.0
    local clampedDist = util.clamp(dist, 0, maxDist)
    
    -- Map from [0, maxDist] to palette indices
    local index = util.map(clampedDist, 0, maxDist, 0, palette.size() - 1)
    
    return util.floor(index)
end
