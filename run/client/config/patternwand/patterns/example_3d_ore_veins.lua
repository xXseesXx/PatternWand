-- 3D Ore Veins
-- Realistic ore veins using 3D Perlin noise
--
-- Uses all three dimensions (x, y, z) to create true 3D structures
-- that look natural from any angle. Perfect for ore deposits, caves,
-- or any structure that needs to feel volumetric.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- 3D noise creates structures that extend in all directions
    local density = noise.perlin3d(x * 0.1, y * 0.1, z * 0.1)
    
    -- Add some variation with a higher frequency layer
    local variation = noise.perlin3d(x * 0.3, y * 0.3, z * 0.3) * 0.3
    local combined = density + variation
    
    -- Dense veins in high-value regions
    if combined > 0.6 then
        return 0  -- Rich ore
    elseif combined > 0.3 then
        return 1  -- Sparse ore
    else
        return 2  -- Stone matrix
    end
end

return pattern
