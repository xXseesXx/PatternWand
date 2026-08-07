-- META: name = "Waves"
-- META: description = "Sine wave pattern with adjustable frequency"
-- META: author = "PatternWand"
-- META: version = "1.0"
-- META: category = "Organic"
-- META: tags = "waves, sine, smooth, water"
-- META: palette_hint = "3+ blocks for smooth wave gradients"

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Wave parameters
    local frequency = 0.2
    local amplitude = 3.0
    
    -- Calculate wave value
    local wave = math.sin(relX * frequency) * amplitude
    
    -- Determine which palette slot based on wave height
    local paletteSize = palette:countNonEmpty()
    if paletteSize == 0 then
        return 0
    end
    
    -- Map wave position to palette slots
    local waveHeight = relZ - wave
    local index = math.floor(waveHeight / 2) % paletteSize
    
    -- Ensure positive index
    if index < 0 then
        index = index + paletteSize
    end
    
    return index
end

return pattern
