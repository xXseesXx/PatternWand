-- META: name = "Stripes"
-- META: description = "Horizontal or vertical stripes with adjustable width"
-- META: author = "PatternWand"
-- META: version = "1.0"
-- META: category = "Geometric"
-- META: tags = "simple, stripes, lines, basic"
-- META: palette_hint = "2+ blocks for alternating colors"

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Stripe width (adjustable)
    local stripeWidth = 3
    
    -- Calculate which stripe we're in (using Z for horizontal stripes)
    local stripeIndex = math.floor(relZ / stripeWidth)
    
    -- Alternate between palette slots based on stripe index
    local paletteSize = palette.countNonEmpty()
    if paletteSize == 0 then
        return 0
    end
    
    return stripeIndex % paletteSize
end

return pattern
