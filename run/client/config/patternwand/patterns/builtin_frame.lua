-- META: name = "Frame"
-- META: description = "3x3 border frame pattern"
-- META: author = "BetterBuildersWands"
-- META: version = "1.0"
-- META: category = "Geometric"
-- META: tags = "border, frame, outline"
-- META: palette_hint = "2 blocks"

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    local cellX = relX % 3
    local cellZ = relZ % 3
    
    -- Check if on edge of 3x3 tile
    local isEdge = (cellX == 0 or cellX == 2) or (cellZ == 0 or cellZ == 2)
    
    if isEdge then
        return 0
    else
        return 1
    end
end

return pattern
