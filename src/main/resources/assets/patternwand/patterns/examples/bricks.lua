--[[
    Pattern Name: Brick Wall
    Author: PatternWand
    Description: Classic brick pattern with mortar
    
    Palette Required:
        - Slot 0: Brick blocks
        - Slot 1: Stone (mortar)
]]--

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    local brickWidth = 4
    local brickHeight = 2
    
    -- Calculate position within brick
    local brickX = relX % brickWidth
    local brickY = relY % brickHeight
    
    -- Offset alternate rows for running bond pattern
    local rowOffset = math.floor(relY / brickHeight) % 2
    if rowOffset == 1 then
        brickX = (relX + 2) % brickWidth
    end
    
    -- Mortar on edges
    if brickX == 0 or brickY == 0 then
        return 1  -- Mortar (stone)
    else
        return 0  -- Brick
    end
end

return pattern
