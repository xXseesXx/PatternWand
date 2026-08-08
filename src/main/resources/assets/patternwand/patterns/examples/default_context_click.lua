metadata = {
    name = "Context: Click Position",
    author = "PatternWand"
}

-- Showcases: context.clickedX, clickedY, clickedZ, clickFace
-- Creates a target pattern centered on clicked position
-- Shows which face was clicked
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local dist = util.distance3d(relX, relY, relZ, 0, 0, 0)
    
    -- Concentric rings from click point
    local ring = util.floor(dist / 2)
    
    -- Center block uses clickFace as palette index
    if dist < 1 then
        return context.clickFace
    end
    
    -- Rings alternate colors
    return util.mod(ring, 2)
end
