-- META: name = "Checkerboard"
-- META: description = "Classic 2x2 alternating tile pattern"
-- META: author = "BetterBuildersWands"
-- META: version = "1.0"
-- META: category = "Geometric"
-- META: tags = "simple, tiles, floor, basic"
-- META: palette_hint = "2 blocks"

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    local tileX = math.floor(relX / 2)
    local tileZ = math.floor(relZ / 2)
    
    if (tileX + tileZ) % 2 == 0 then
        return 0
    else
        return 1
    end
end

return pattern
