metadata = {
    name = "Seed: Custom Randomization",
    author = "PatternWand"
}

-- Showcases: seed parameter
-- The seed affects noise generation and can be set with:
-- /patternwand seed <value>
-- Same seed = same pattern every time
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Seed automatically affects noise functions
    local n = noise.simplex(x * 0.1, z * 0.1)
    
    -- Map noise to palette
    local index = util.floor((n + 1) * 0.5 * (palette.size() - 1))
    
    return index
end
