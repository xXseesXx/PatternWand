-- Racing Stripes
-- Diagonal stripe pattern
--
-- Creates diagonal stripes across the placement area. The stripes run at
-- a 45-degree angle, perfect for creating dynamic racing track markings,
-- decorative floors, or angled patterns.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Calculate diagonal stripes using 4-unit tiles
    -- Diagonal stripes: sum of coordinates creates diagonal lines
    local diagonal = (relX + relZ) / 4
    
    -- Create alternating diagonal bands
    if math.floor(diagonal) % 2 == 0 then
        return 0  -- First stripe color
    else
        return 1  -- Second stripe color
    end
end

return pattern
