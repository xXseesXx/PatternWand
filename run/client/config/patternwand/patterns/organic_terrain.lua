-- META: name = "Organic Terrain"
-- META: description = "Natural-looking terrain with multiple layers of noise"
-- META: author = "PatternWand"
-- META: version = "1.0"
-- META: category = "Organic"
-- META: tags = "terrain, natural, complex, layered"
-- META: palette_hint = "4+ blocks for terrain layers (grass, dirt, stone, ore)"

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    local paletteSize = palette.countNonEmpty()
    if paletteSize == 0 then
        return 0
    end
    
    -- Multiple octaves of noise for natural variation
    local scale1 = 0.05
    local scale2 = 0.15
    local scale3 = 0.3
    
    local noise1 = noise.perlin(relX * scale1, relZ * scale1)
    local noise2 = noise.perlin(relX * scale2, relZ * scale2)
    local noise3 = noise.simplex(relX * scale3, relZ * scale3)
    
    -- Combine noise with different weights
    local combined = noise1 * 0.5 + noise2 * 0.3 + noise3 * 0.2
    
    -- Add height consideration
    local heightFactor = relY * 0.05
    combined = combined + heightFactor
    
    -- Map to palette slots based on combined noise
    if combined > 0.4 then
        return 0  -- Top layer
    elseif combined > 0.1 then
        return 1  -- Second layer
    elseif combined > -0.2 then
        return 2  -- Third layer
    else
        -- Use remaining palette slots with weighted random
        if paletteSize > 3 then
            return 3 + (util.abs(util.hash3d(relX, relY, relZ)) % (paletteSize - 3))
        else
            return paletteSize - 1
        end
    end
end

return pattern
