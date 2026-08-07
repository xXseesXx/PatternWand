-- META: name = "Stripes"
-- META: description = "Horizontal alternating stripes"
-- META: author = "BetterBuildersWands"
-- META: version = "1.0"
-- META: category = "Geometric"
-- META: tags = "simple, stripes, lines"
-- META: palette_hint = "2 blocks"

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    local stripe = math.floor(relZ / 2)
    
    if stripe % 2 == 0 then
        return 0
    else
        return 1
    end
end

return pattern
