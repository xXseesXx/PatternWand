-- META: name = "Random Scatter"
-- META: description = "Randomly scattered blocks with controlled density"
-- META: author = "PatternWand"
-- META: version = "1.0"
-- META: category = "Random"
-- META: tags = "random, scatter, sparse, decorative"
-- META: palette_hint = "2 blocks (base and scattered accent)"

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Use hash function for deterministic randomness
    local hashValue = util.hash(relX, relZ)
    
    -- Calculate random value (0-1)
    local randomValue = (hashValue % 1000) / 1000.0
    
    -- Density threshold (20% will be accent blocks)
    local density = 0.2
    
    if randomValue < density then
        return 1  -- Accent block
    else
        return 0  -- Base block
    end
end

return pattern
