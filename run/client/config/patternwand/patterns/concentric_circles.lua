-- META: name = "Concentric Circles"
-- META: description = "Rings radiating from center point"
-- META: author = "PatternWand"
-- META: version = "1.0"
-- META: category = "Geometric"
-- META: tags = "circles, radial, rings, target"
-- META: palette_hint = "2+ blocks for alternating rings"

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Calculate distance from center
    local dist = util.distance(relX, relZ, 0, 0)
    
    -- Ring width
    local ringWidth = 4
    
    -- Which ring are we in?
    local ringIndex = math.floor(dist / ringWidth)
    
    -- Alternate between palette slots
    local paletteSize = palette.countNonEmpty()
    if paletteSize == 0 then
        return 0
    end
    
    return ringIndex % paletteSize
end

return pattern
