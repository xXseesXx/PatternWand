metadata = {
    name = "Context: Bounding Box",
    author = "PatternWand"
}

-- Showcases: context.minX/Y/Z, maxX/Y/Z
-- Creates a frame around the bounding box edges
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local isEdge = (x == context.minX or x == context.maxX or
                    y == context.minY or y == context.maxY or
                    z == context.minZ or z == context.maxZ)
    
    if isEdge then
        return 0  -- Edge blocks
    else
        return 1  -- Interior blocks
    end
end
