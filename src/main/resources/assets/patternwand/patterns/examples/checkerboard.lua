--[[
    Pattern Name: Checkerboard
    Author: PatternWand
    Description: Simple alternating checkerboard pattern
    
    Palette Required:
        - Slot 0: First block type (e.g., Stone)
        - Slot 1: Second block type (e.g., Cobblestone)
]]--

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Alternate between two blocks based on coordinates
    if (relX + relZ) % 2 == 0 then
        return 0  -- First palette slot
    else
        return 1  -- Second palette slot
    end
end

return pattern
