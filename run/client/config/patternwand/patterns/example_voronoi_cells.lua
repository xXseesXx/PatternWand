-- Voronoi Cells
-- Organic cellular pattern using Voronoi noise
--
-- Creates distinct cells like dragon scales, tiles, or organic tissue.
-- Each cell has a unique ID, making it easy to assign different blocks
-- to different cells. Perfect for creating segmented patterns.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Get the cell ID for this position
    local cellId = noise.voronoi(x * 0.1, z * 0.1)
    
    -- Use the cell ID to pick a palette slot
    -- Each cell will have a consistent block type
    local paletteSize = palette.size()
    
    -- Hash the cell ID to get better distribution across palette
    local index = util.hash(cellId, 0) % paletteSize
    
    -- Take absolute value and ensure it's in valid range
    return math.abs(index) % paletteSize
end

return pattern
