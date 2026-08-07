-- META: name = "Terrain"
-- META: description = "Large smooth gradients like terrain generation"
-- META: author = "BetterBuildersWands"
-- META: version = "1.0"
-- META: category = "Noise"
-- META: tags = "noise, terrain, smooth, natural"
-- META: palette_hint = "3-5 blocks"

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Low frequency gradient noise (0.05 frequency, 3 octaves)
    local value = noise.gradient(x, y, z, 0.05, 3)
    
    -- Map to palette indices
    local paletteSize = palette.size()
    local index = math.floor((value + 1) * 0.5 * paletteSize)
    
    return math.max(0, math.min(paletteSize - 1, index))
end

return pattern
