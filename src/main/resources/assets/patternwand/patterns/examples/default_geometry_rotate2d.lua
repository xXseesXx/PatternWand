metadata = {
    name = "Geometry: 2D Rotation",
    author = "PatternWand",
    parameters = {
        angle = {type = "float", default = 45.0, min = 0.0, max = 360.0}
    }
}

-- Showcases: util.rotate2D()
-- Rotates a stripe pattern around the Y axis
-- Angle in degrees
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Rotate coordinates relative to clicked position
    local rotated = util.rotate2D(relX, relZ, params.angle)
    
    -- Create stripes based on rotated X coordinate
    if util.mod(util.floor(rotated.x), 4) < 2 then
        return 0
    else
        return 1
    end
end
