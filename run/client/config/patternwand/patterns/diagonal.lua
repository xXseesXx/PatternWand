-- META: name = "Diagonal"
-- META: description = "Diagonal stripes at 45-degree angle"
-- META: author = "PatternWand"
-- META: version = "1.0"
-- META: category = "Geometric"
-- META: tags = "diagonal, stripes, simple"
-- META: palette_hint = "2+ blocks for alternating stripes"

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Stripe width
    local stripeWidth = 3
    
    -- Diagonal stripe calculation (X + Z creates 45-degree diagonal)
    local stripeIndex = math.floor((relX + relZ) / stripeWidth)
    
    -- Alternate between palette slots
    local paletteSize = palette.countNonEmpty()
    if paletteSize == 0 then
        return 0
    end
    
    return stripeIndex % paletteSize
end

return pattern
