-- Spherical Dome Pattern
-- Demonstrates the new geometry utility functions

metadata = {
    name = "Spherical Dome",
    author = "PatternWand",
    parameters = {
        radius = {type = "float", default = 10, min = 3, max = 50},
        hollow = {type = "boolean", default = false}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local radius = params.radius or 10
    local hollow = params.hollow or false
    
    -- Center the sphere at clicked position
    local centerX = context.clickedX
    local centerY = context.clickedY
    local centerZ = context.clickedZ
    
    -- Check if point is in sphere
    if not util.inSphere(x, y, z, centerX, centerY, centerZ, radius) then
        return nil  -- Outside sphere, skip block
    end
    
    -- Only place upper hemisphere (dome)
    if y < centerY then
        return nil
    end
    
    -- If hollow, skip interior
    if hollow then
        local dist = util.distance3d(x, y, z, centerX, centerY, centerZ)
        if dist < radius - 1 then
            return nil
        end
    end
    
    -- Calculate height-based coloring
    local heightRatio = (y - centerY) / radius
    heightRatio = util.clamp(heightRatio, 0, 1)
    
    -- Use palette based on height
    local paletteCount = palette.countNonEmpty()
    if paletteCount == 0 then
        return 0
    end
    
    local index = util.floor(heightRatio * (paletteCount - 1))
    return util.clamp(index, 0, paletteCount - 1)
end
