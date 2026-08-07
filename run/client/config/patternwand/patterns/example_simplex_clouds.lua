-- Simplex Clouds
-- Fast, organic cloud-like pattern using Simplex noise
--
-- Simplex noise is faster than Perlin with fewer directional artifacts.
-- Perfect for large-scale patterns where performance matters, like
-- scattered ore deposits, cloud patterns, or terrain features.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Use simplex noise for fast, smooth variation
    -- Larger scale (0.08) creates cloud-like patterns
    local value = noise.simplex(x * 0.08, z * 0.08)
    
    -- Add a second layer for more detail
    local detail = noise.simplex(x * 0.25, z * 0.25) * 0.3
    local combined = value + detail
    
    -- Threshold-based placement creates distinct regions
    if combined > 0.4 then
        return 0  -- Dense cloud
    elseif combined > 0 then
        return 1  -- Light cloud
    elseif combined > -0.4 then
        return 2  -- Sparse cloud
    else
        return nil  -- Sky (gap)
    end
end

return pattern
