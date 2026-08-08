metadata = {
    name = "Context: Player Orientation",
    author = "PatternWand"
}

-- Showcases: context.playerYaw, context.playerPitch
-- Creates stripes that follow player's looking direction
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Rotate pattern based on player yaw (horizontal rotation)
    local rotated = util.rotate2D(relX, relZ, -context.playerYaw)
    
    -- Create stripes perpendicular to view direction
    local stripe = util.floor(rotated.y / 3)
    
    return util.mod(stripe, palette.size())
end
