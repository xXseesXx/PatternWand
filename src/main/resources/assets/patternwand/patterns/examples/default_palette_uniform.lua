metadata = {
    name = "Palette: Uniform Selection",
    author = "PatternWand"
}

-- Showcases: palette.pickUniform()
-- Randomly selects blocks from palette with equal probability
-- Stack sizes are ignored - all blocks have same chance
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    return palette.pickUniform()
end
