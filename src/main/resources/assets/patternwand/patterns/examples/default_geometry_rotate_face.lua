metadata = {
    name = "Geometry: Face Rotation",
    author = "PatternWand"
}

-- Showcases: util.rotateFace()
-- Creates a checkerboard that works on any clicked face
-- Pattern automatically orients to the surface
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Transform coordinates based on clicked face
    local coords = util.rotateFace(relX, relY, relZ, context.clickFace)
    
    -- u = horizontal, v = vertical, w = depth (away from surface)
    -- Create checkerboard on the surface (ignore w depth)
    local checker = util.mod(util.floor(coords.u / 2) + util.floor(coords.v / 2), 2)
    
    return checker
end
