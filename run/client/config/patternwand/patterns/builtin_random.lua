-- META: name = "Random"
-- META: description = "True random weighted selection per block"
-- META: author = "BetterBuildersWands"
-- META: version = "1.0"
-- META: category = "Random"
-- META: tags = "random, varied, natural"
-- META: palette_hint = "2+ blocks"

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Use noise.random() for true weighted random selection
    -- This respects palette weights
    return noise.random(x, y, z)
end

return pattern
