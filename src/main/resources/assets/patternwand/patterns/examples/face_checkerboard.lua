-- Face-Oriented Checkerboard
-- Demonstrates util.rotateFace() for surface-relative patterns
-- The pattern adapts to the clicked face automatically

metadata = {
    name = "Face Checkerboard",
    author = "PatternWand",
    ignoreMetadata = true,
    parameters = {
        size = {type = "integer", default = 2, min = 1, max = 10}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Transform coordinates based on the clicked face
    -- After transformation, u/v are the 2D surface coordinates, w is depth
    local coords = util.rotateFace(relX, relY, relZ, context.clickFace)
    local u = coords[1]
    local v = coords[2]
    local w = coords[3]
    
    -- Create checkerboard pattern on the surface (u/v plane)
    local size = params.size or 2
    local checkU = util.floor(u / size)
    local checkV = util.floor(v / size)
    
    -- Alternate between two palette slots
    if util.mod(checkU + checkV, 2) == 0 then
        return 0  -- First palette slot
    else
        return 1  -- Second palette slot
    end
end
