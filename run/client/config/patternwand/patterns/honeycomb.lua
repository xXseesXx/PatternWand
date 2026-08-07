-- META: name = "Honeycomb"
-- META: description = "Hexagonal honeycomb pattern"
-- META: author = "PatternWand"
-- META: version = "1.0"
-- META: category = "Geometric"
-- META: tags = "hexagon, honeycomb, complex, organic"
-- META: palette_hint = "2 blocks (hex fill and borders)"

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- Hexagonal grid constants
    local size = 4.0
    local sqrt3 = 1.732050808
    
    -- Convert to hexagonal coordinates
    local q = (2.0/3.0 * relX) / size
    local r = (-1.0/3.0 * relX + sqrt3/3.0 * relZ) / size
    
    -- Round to nearest hex
    local s = -q - r
    local rq = math.floor(q + 0.5)
    local rr = math.floor(r + 0.5)
    local rs = math.floor(s + 0.5)
    
    local q_diff = math.abs(rq - q)
    local r_diff = math.abs(rr - r)
    local s_diff = math.abs(rs - s)
    
    if q_diff > r_diff and q_diff > s_diff then
        rq = -rr - rs
    elseif r_diff > s_diff then
        rr = -rq - rs
    end
    
    -- Determine if on border
    local hexX = size * (3.0/2.0 * rq)
    local hexZ = size * (sqrt3/2.0 * rq + sqrt3 * rr)
    local distToCenter = util.distance(relX, relZ, hexX, hexZ)
    
    -- Border threshold
    if distToCenter > size * 0.8 then
        return 1  -- Border
    else
        return 0  -- Fill
    end
end

return pattern
