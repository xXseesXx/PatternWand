--[[
    Pattern Name: Ripples
    Author: PatternWand
    Description: Circular ripple pattern radiating from center
    
    Palette Required:
        - Slot 0: Center/peak blocks
        - Slot 1: Mid-tone blocks
        - Slot 2: Outer blocks
]]--

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Calculate distance from center (origin of pattern)
    local dist = util.distance(relX, relZ, 0, 0)
    
    -- Create sine wave based on distance
    local wave = math.sin(dist * 0.3)
    
    -- Map wave to palette slots
    if wave > 0.5 then
        return 0  -- Peak of wave
    elseif wave > 0 then
        return 1  -- Rising/falling
    else
        return 2  -- Trough
    end
end

return pattern
