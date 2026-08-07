--[[
    Pattern Name: Natural Stone Mix
    Author: PatternWand
    Description: Natural-looking stone pattern using Perlin noise
    
    Palette Required:
        - Slot 0: Stone
        - Slot 1: Cobblestone
        - Slot 2: Andesite
        - Slot 3: Gravel (optional)
]]--

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Generate noise value (scale coordinates for smoother noise)
    local value = noise.perlin(x * 0.05, z * 0.05)
    
    -- Map noise to different stone types
    if value > 0.3 then
        return 0  -- Stone (high areas)
    elseif value > 0 then
        return 1  -- Cobblestone (medium areas)
    elseif value > -0.3 then
        return 2  -- Andesite (low areas)
    else
        -- Use weighted random for lowest areas
        if palette.countNonEmpty() > 3 then
            return palette.pickWeighted()
        else
            return 3  -- Gravel or fallback to slot 3
        end
    end
end

return pattern
