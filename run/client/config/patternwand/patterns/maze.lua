-- META: name = "Maze"
-- META: description = "Procedural maze-like pattern"
-- META: author = "PatternWand"
-- META: version = "1.0"
-- META: category = "Geometric"
-- META: tags = "maze, complex, walls, paths"
-- META: palette_hint = "2 blocks (walls and paths)"

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Maze parameters
    local scale = 0.15
    
    -- Generate two noise fields
    local noise1 = noise.simplex(relX * scale, relZ * scale)
    local noise2 = noise.simplex(relX * scale + 100, relZ * scale + 100)
    
    -- Combine noise to create maze-like pattern
    local combined = noise1 * noise2
    
    -- Create walls based on noise threshold
    local threshold = 0.1
    
    if util.abs(combined) < threshold then
        return 1  -- Wall
    else
        return 0  -- Path
    end
end

return pattern
