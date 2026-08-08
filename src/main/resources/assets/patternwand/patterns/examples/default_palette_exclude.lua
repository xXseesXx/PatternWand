metadata = {
    name = "Palette: Weighted with Exclusion",
    author = "PatternWand"
}

-- Showcases: palette.pickWeightedExcept()
-- Weighted selection but excludes first slot (index 0)
-- Useful for avoiding specific blocks in random patterns
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    return palette.pickWeightedExcept(0)
end
