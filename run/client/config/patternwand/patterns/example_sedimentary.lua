-- Sedimentary Rock Layers
-- Horizontal rock layers with natural variation
--
-- Simulates sedimentary rock formations with horizontal layers that have
-- slight undulation and thickness variation. Perfect for realistic cliff
-- faces, canyon walls, or underground excavations.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Use Y coordinate for horizontal layers
    -- Add noise to create natural undulation in the layers
    local noiseValue = noise.fbm(x * 0.03, z * 0.03, 2)
    
    -- Apply noise to Y to create wavy layers
    local adjustedY = y + noiseValue * 3
    
    -- Create layers of varying thickness
    local paletteSize = palette.size()
    local layerThickness = 3  -- Base layer thickness
    
    -- Map Y coordinate to palette with natural variation
    local index = math.floor(adjustedY / layerThickness) % paletteSize
    
    return index
end

return pattern
