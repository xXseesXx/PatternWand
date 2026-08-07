-- Floor Tiles (2x2 Checkerboard Pattern)
-- A simple alternating pattern perfect for tiled floors
--
-- This creates a 2x2 checkerboard tile pattern where blocks alternate
-- between two palette slots. Great for creating classic floor tiles.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Calculate which tile we're in (2x2 tiles)
    local tileX = math.floor(relX / 2)
    local tileZ = math.floor(relZ / 2)
    
    -- Checkerboard: alternate based on tile position
    if (tileX + tileZ) % 2 == 0 then
        return 0  -- First block type
    else
        return 1  -- Second block type
    end
end

return pattern
