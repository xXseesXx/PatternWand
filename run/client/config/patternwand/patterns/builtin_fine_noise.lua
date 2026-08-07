-- META: name = "Fine Noise"
-- META: description = "High frequency noise for speckled appearance"
-- META: author = "BetterBuildersWands"
-- META: version = "1.0"
-- META: category = "Noise"
-- META: tags = "noise, texture, speckled"
-- META: palette_hint = "2-3 blocks"

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- High frequency perlin noise (0.25 frequency, 3 octaves)
    local value = noise.perlin(x, y, z, 0.25, 3)
    
    -- Map to palette indices
    local paletteSize = palette.size()
    local index = math.floor((value + 1) * 0.5 * paletteSize)
    
    return math.max(0, math.min(paletteSize - 1, index))
end

return pattern
