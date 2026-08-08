metadata = {
    name = "Debug: Print Function",
    author = "PatternWand"
}

-- Showcases: debug.print()
-- Prints debug information to console
-- Enable with: /patternwand debug on
-- This pattern prints info for blocks near the center
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local dist = util.distance3d(relX, relY, relZ, 0, 0, 0)
    
    -- Only print for blocks close to center (avoid spam)
    if dist < 3 then
        debug.print("Block at rel(" .. relX .. "," .. relY .. "," .. relZ .. ") dist=" .. dist)
    end
    
    -- Create simple gradient
    local index = util.floor(util.clamp(dist / 10.0, 0, 1) * (palette.size() - 1))
    return index
end
