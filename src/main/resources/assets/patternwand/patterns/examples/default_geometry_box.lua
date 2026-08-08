metadata = {
    name = "Geometry: Box",
    author = "PatternWand"
}

-- Showcases: util.inBox()
-- Uses the bounding box from context
-- Places blocks only if inside the box
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Shrink the box by 2 blocks on each side
    local minX = context.minX + 2
    local minY = context.minY + 2
    local minZ = context.minZ + 2
    local maxX = context.maxX - 2
    local maxY = context.maxY - 2
    local maxZ = context.maxZ - 2
    
    if util.inBox(x, y, z, minX, minY, minZ, maxX, maxY, maxZ) then
        return 0
    else
        return nil
    end
end
