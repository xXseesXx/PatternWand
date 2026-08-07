-- META: name = "Medium Noise"
-- META: description = "Balanced noise pattern for natural variation"
-- META: author = "BetterBuildersWands"
-- META: version = "1.0"
-- META: category = "Noise"
-- META: tags = "noise, texture, balanced"
-- META: palette_hint = "2-3 blocks"

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Medium frequency perlin noise (0.15 frequency, 2 octaves)
    local value = noise.perlin(x, y, z, 0.15, 2)
    
    -- Map to palette indices
    local paletteSize = palette.size()
    local index = math.floor((value + 1) * 0.5 * paletteSize)
    
    return math.max(0, math.min(paletteSize - 1, index))
end

return pattern
