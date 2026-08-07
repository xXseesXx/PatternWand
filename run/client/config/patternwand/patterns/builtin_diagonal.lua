-- META: name = "Diagonal"
-- META: description = "3x3 diagonal stripe pattern"
-- META: author = "BetterBuildersWands"
-- META: version = "1.0"
-- META: category = "Geometric"
-- META: tags = "diagonal, stripes"
-- META: palette_hint = "2 blocks"

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    local cellX = relX % 3
    local cellZ = relZ % 3
    
    -- Diagonal pattern: (x + (3-1-z)) % 2
    if (cellX + (2 - cellZ)) % 2 == 0 then
        return 0
    else
        return 1
    end
end

return pattern
