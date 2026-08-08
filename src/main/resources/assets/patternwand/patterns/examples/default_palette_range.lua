metadata = {
    name = "Palette: Range Selection",
    author = "PatternWand"
}

-- Showcases: palette.pickWeightedRange()
-- Only uses first 9 slots (0-8) of palette
-- Useful for creating subsets or limiting block variety
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    return palette.pickWeightedRange(0, 8)
end
