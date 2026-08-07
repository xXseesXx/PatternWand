-- Natural Stone Wall
-- Realistic stone texture using Perlin noise
--
-- Creates a natural-looking stone pattern that mimics how different types
-- of stone blend together in nature. Perfect for walls, paths, or terrain.
-- Uses 3 octaves of noise for detail.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Use world coordinates for consistent pattern across placements
    -- Scale factor 0.2 controls the size of stone patches
    local value = noise.fbm(x * 0.2, z * 0.2, 3)
    
    -- Map noise value [-1, 1] to palette indices
    local paletteSize = palette.size()
    
    -- Convert noise to index range [0, paletteSize-1]
    local index = math.floor((value + 1) * 0.5 * paletteSize)
    
    -- Clamp to valid range
    return math.max(0, math.min(paletteSize - 1, index))
end

return pattern
