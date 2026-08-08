-- Centered Gradient Pattern
-- Demonstrates the new placement context feature

metadata = {
    name = "Centered Gradient",
    author = "PatternWand",
    parameters = {}
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Get the center of the placement area from context
    local centerX = (context.minX + context.maxX) / 2
    local centerY = (context.minY + context.maxY) / 2
    local centerZ = (context.minZ + context.maxZ) / 2
    
    -- Calculate distance from center
    local dist = util.distance3d(x, y, z, centerX, centerY, centerZ)
    
    -- Get maximum distance (half diagonal of bounding box)
    local sizeX = context.maxX - context.minX
    local sizeY = context.maxY - context.minY
    local sizeZ = context.maxZ - context.minZ
    local maxDist = util.distance3d(0, 0, 0, sizeX/2, sizeY/2, sizeZ/2)
    
    -- Normalize distance to 0-1 range
    local normalized = util.clamp(dist / maxDist, 0, 1)
    
    -- Use smoothstep for smoother transition
    normalized = util.smoothstep(0, 1, normalized)
    
    -- Map to palette indices (assuming palette has 5+ blocks)
    local paletteCount = palette.countNonEmpty()
    if paletteCount == 0 then
        return 0
    end
    
    local index = util.floor(normalized * (paletteCount - 1))
    return util.clamp(index, 0, paletteCount - 1)
end
