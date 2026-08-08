metadata = {
    name = "Palette: Weighted Selection",
    author = "PatternWand"
}

-- Showcases: palette.pickWeighted()
-- Randomly selects blocks from palette based on stack size weights
-- Higher stack sizes = more likely to be placed
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    return palette.pickWeighted()
end
