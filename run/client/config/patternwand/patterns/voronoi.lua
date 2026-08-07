-- META: name = "Voronoi"
-- META: description = "Organic cellular voronoi pattern"
-- META: author = "PatternWand"
-- META: version = "1.0"
-- META: category = "Organic"
-- META: tags = "voronoi, cellular, organic, natural"
-- META: palette_hint = "3+ blocks for varied cells"

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Grid spacing for voronoi points
    local cellSize = 8
    
    -- Find the cell we're in
    local cellX = math.floor(relX / cellSize)
    local cellZ = math.floor(relZ / cellSize)
    
    -- Check surrounding cells for closest point
    local minDist = 999999
    local closestHash = 0
    
    for dx = -1, 1 do
        for dz = -1, 1 do
            local checkX = cellX + dx
            local checkZ = cellZ + dz
            
            -- Generate random point within this cell
            local hash = util.hash(checkX, checkZ)
            local pointX = checkX * cellSize + (hash % cellSize)
            local pointZ = checkZ * cellSize + ((hash / cellSize) % cellSize)
            
            -- Calculate distance to this point
            local dist = util.distance(relX, relZ, pointX, pointZ)
            
            if dist < minDist then
                minDist = dist
                closestHash = hash
            end
        end
    end
    
    -- Map hash to palette slot
    local paletteSize = palette.countNonEmpty()
    if paletteSize == 0 then
        return 0
    end
    
    -- Use border detection for edges
    local secondMinDist = 999999
    for dx = -1, 1 do
        for dz = -1, 1 do
            local checkX = cellX + dx
            local checkZ = cellZ + dz
            local hash = util.hash(checkX, checkZ)
            local pointX = checkX * cellSize + (hash % cellSize)
            local pointZ = checkZ * cellSize + ((hash / cellSize) % cellSize)
            local dist = util.distance(relX, relZ, pointX, pointZ)
            
            if dist < secondMinDist and dist > minDist + 0.1 then
                secondMinDist = dist
            end
        end
    end
    
    -- Draw borders
    if secondMinDist - minDist < 1.5 then
        return 0  -- Border
    end
    
    return (util.abs(closestHash) % (paletteSize - 1)) + 1
end

return pattern
