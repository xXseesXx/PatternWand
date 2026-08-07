-- META: name = "Rough Terrain"
-- META: description = "More varied terrain-like pattern with higher detail"
-- META: author = "BetterBuildersWands"
-- META: version = "1.0"
-- META: category = "Noise"
-- META: tags = "noise, terrain, varied, natural"
-- META: palette_hint = "3-5 blocks"

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Medium-low frequency gradient noise with more octaves (0.08 frequency, 4 octaves)
    local value = noise.gradient(x, y, z, 0.08, 4)
    
    -- Map to palette indices
    local paletteSize = palette.size()
    local index = math.floor((value + 1) * 0.5 * paletteSize)
    
    return math.max(0, math.min(paletteSize - 1, index))
end

return pattern
