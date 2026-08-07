-- Window Frame (3x3)
-- Border/frame pattern for creating windows
--
-- Creates a repeating 3x3 pattern where the edges are one block type
-- (frame) and the center is another (glass/air). Perfect for quickly
-- building multiple windows or creating decorative bordered patterns.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Calculate position within 3x3 tile
    local tileX = relX % 3
    local tileY = relY % 3
    
    -- Check if we're on the edge of the tile
    local isEdge = (tileX == 0 or tileX == 2 or tileY == 0 or tileY == 2)
    
    if isEdge then
        return 0  -- Frame material (e.g., wood, stone)
    else
        return 1  -- Center material (e.g., glass)
    end
end

return pattern
              