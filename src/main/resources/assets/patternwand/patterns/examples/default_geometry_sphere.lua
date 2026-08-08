metadata = {
    name = "Geometry: Sphere",
    author = "PatternWand",
    description = "Creates a perfect sphere centered on the clicked position with configurable radius",
    parameters = {
        radius = {type = "float", default = 8.0, min = 1.0, max = 50.0}
    }
}

-- Showcases: util.inSphere()
-- Creates a sphere centered on the clicked position
-- Returns true if point is inside sphere
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local centerX = context.clickedX
    local centerY = context.clickedY
    local centerZ = context.clickedZ
    
    if util.inSphere(x, y, z, centerX, centerY, centerZ, params.radius) then
        return 0
    else
        return nil  -- Skip blocks outside sphere
    end
end
