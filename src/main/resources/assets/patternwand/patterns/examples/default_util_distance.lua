metadata = {
    name = "Util: Distance Functions",
    author = "PatternWand",
    parameters = {
        mode = {type = "string", default = "euclidean"}
    }
}

-- Showcases: util.distance3d() and util.manhattan()
-- Creates concentric rings from center using different distance metrics
-- mode parameter: "euclidean" or "manhattan"
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local dist
    
    if params.mode == "manhattan" then
        -- Manhattan distance (taxicab distance)
        dist = util.abs(relX) + util.abs(relY) + util.abs(relZ)
    else
        -- Euclidean distance (straight line)
        dist = util.distance3d(relX, relY, relZ, 0, 0, 0)
    end
    
    -- Create rings every 3 blocks
    local ring = util.floor(dist / 3)
    return util.mod(ring, palette.size())
end
