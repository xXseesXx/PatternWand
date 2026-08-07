-- 3D Caves
-- Volumetric cave-like structures using 3D Simplex noise
--
-- Uses 3D Simplex noise (faster than Perlin3D) to create natural-looking
-- cave systems or organic 3D structures. The pattern looks consistent
-- from all directions because it uses all three spatial dimensions.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Large-scale cave structure
    local caves = noise.simplex3d(x * 0.05, y * 0.05, z * 0.05)
    
    -- Add smaller details for texture
    local detail = noise.simplex3d(x * 0.2, y * 0.2, z * 0.2) * 0.3
    local combined = caves + detail
    
    -- Create cave air pockets and stone
    if combined > 0.3 then
        return nil  -- Air pocket (gap)
    elseif combined > 0 then
        return 0  -- Cave wall stone
    elseif combined > -0.3 then
        return 1  -- Deeper stone layer
    else
        return 2  -- Deepest stone
    end
end

return pattern
