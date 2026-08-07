-- Cracked Stone
-- Cell borders with Worley noise creating cracked appearance
--
-- Worley noise returns distance to the nearest cell center, which lets
-- us create visible cell boundaries. Perfect for cracked earth, broken
-- stone, or stained glass with leading lines.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Get distance to nearest cell center
    local dist = noise.worley(x * 0.15, z * 0.15)
    
    -- Also get the cell ID for interior variation
    local cellId = noise.voronoi(x * 0.15, z * 0.15)
    
    -- Create visible cracks at cell boundaries
    if dist < 0.08 then
        return 0  -- Crack/border (darker stone)
    else
        -- Use cell ID to vary the interior blocks
        local paletteSize = palette.size()
        if paletteSize > 2 then
            -- Multiple interior variations based on cell
            local hash = util.hash(cellId, 0)
            return 1 + (math.abs(hash) % (paletteSize - 1))
        else
            return 1  -- Simple two-tone if only 2 blocks
        end
    end
end

return pattern
