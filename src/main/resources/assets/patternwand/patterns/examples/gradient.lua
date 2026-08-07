--[[
    Pattern Name: Gradient
    Author: PatternWand
    Description: Smooth gradient transition between blocks
    
    Palette Required:
        - Fill palette with blocks from dark to light
        - More filled slots = smoother gradient
]]--

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    local paletteSize = palette.countNonEmpty()
    
    if paletteSize == 0 then
        return 0  -- Fallback if palette is empty
    end
    
    -- Create horizontal gradient based on X position
    -- Scale factor determines gradient width
    local scaleFactor = 0.1
    local gradientValue = math.sin(relX * scaleFactor)
    
    -- Map gradient [-1, 1] to palette indices [0, paletteSize-1]
    local index = util.map(gradientValue, -1, 1, 0, paletteSize - 1)
    
    -- Round to nearest integer and clamp
    index = util.clamp(math.floor(index + 0.5), 0, paletteSize - 1)
    
    return index
end

return pattern
