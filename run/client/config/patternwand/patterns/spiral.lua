-- META: name = "Spiral"
-- META: description = "Logarithmic spiral pattern radiating from center"
-- META: author = "PatternWand"
-- META: version = "1.0"
-- META: category = "Geometric"
-- META: tags = "spiral, radial, complex, artistic"
-- META: palette_hint = "3+ blocks for smooth color transitions"

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Calculate angle and distance from center
    local angle = math.atan2(relZ, relX)
    local dist = util.distance(relX, relZ, 0, 0)
    
    -- Logarithmic spiral formula
    local spiralValue = angle + math.log(dist + 1) * 2
    
    -- Map to palette slots
    local paletteSize = palette.countNonEmpty()
    if paletteSize == 0 then
        return 0
    end
    
    local index = math.floor(spiralValue * 2) % paletteSize
    return index
end

return pattern
