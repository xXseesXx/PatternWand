-- META: name = "Layers"
-- META: description = "Very low frequency for large horizontal layers"
-- META: author = "BetterBuildersWands"
-- META: version = "1.0"
-- META: category = "Noise"
-- META: tags = "noise, layers, horizontal, smooth"
-- META: palette_hint = "3-5 blocks"

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Very low frequency gradient noise (0.02 frequency, 2 octaves)
    local value = noise.gradient(x, y, z, 0.02, 2)
    
    -- Map to palette indices
    local paletteSize = palette.size()
    local index = math.floor((value + 1) * 0.5 * paletteSize)
    
    return math.max(0, math.min(paletteSize - 1, index))
end

return pattern
