--========================================================--
--  Infinite Deterministic Triangle Pattern
--
--  Generates an infinite tessellation of equilateral
--  triangles with 27 deterministic random palette values.
--
--  Palette:
--      0-26
--
--  Completely deterministic in world space.
--========================================================--

local TRI_SIZE = 24.0

local SQRT3 = math.sqrt(3)

------------------------------------------------------------

local function mix(h)
    h = math.abs(h)
    return (h * 1103515245 + 12345) % 2147483647
end

------------------------------------------------------------

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)

    --------------------------------------------------------
    -- Convert world coordinates to triangle grid
    --------------------------------------------------------

    local gx = x / TRI_SIZE
    local gz = z / TRI_SIZE

    -- Basis coordinates
    local u = gx - gz / SQRT3
    local v = (2.0 * gz) / SQRT3

    local iu = math.floor(u)
    local iv = math.floor(v)

    local fu = u - iu
    local fv = v - iv

    --------------------------------------------------------
    -- Determine which half of the rhombus we're in
    --------------------------------------------------------

    local tx, ty, orient

    if fu + fv < 1.0 then
        tx = iu
        ty = iv
        orient = 0
    else
        tx = iu + 1
        ty = iv + 1
        orient = 1
    end

    --------------------------------------------------------
    -- Deterministic random colour
    --------------------------------------------------------

    local h = mix(util.hash(tx * 2 + orient, ty))

    return h % 27

end

return pattern