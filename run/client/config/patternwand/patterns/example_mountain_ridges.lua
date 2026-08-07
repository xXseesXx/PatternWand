-- Mountain Ridges
-- Sharp ridges and peaks using ridged multifractal noise
--
-- Ridged noise creates sharp, elevated features perfect for mountain
-- ranges, cliffs, or any terrain that needs dramatic elevation changes.
-- The inverted peaks create natural-looking ridgelines.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Ridged noise with 3 octaves for natural mountain features
    local ridges = noise.ridged(x * 0.05, z * 0.05, 3)
    
    -- Map ridge values to different elevation zones
    local paletteSize = palette.size()
    
    if ridges > 0.8 then
        return 0  -- Mountain peaks (snow-capped)
    elseif ridges > 0.6 then
        return 1  -- Upper slopes (exposed rock)
    elseif ridges > 0.4 then
        return 2  -- Mid slopes (grass/dirt)
    elseif ridges > 0.2 then
        return math.min(3, paletteSize - 1)  -- Lower slopes
    else
        return math.min(4, paletteSize - 1)  -- Valleys/flatlands
    end
end

return pattern
