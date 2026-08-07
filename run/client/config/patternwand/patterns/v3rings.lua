--========================================================--
--  Large Deterministic Voronoi Rings
--
--  Palette Layout
--
--  0  = Borders
--
--  1  2  3   Group 1
--  4  5  6   Group 2
--  7  8  9   Group 3
-- 10 11 12   Group 4
-- 13 14 15   Group 5
-- 16 17 18   Group 6
--
-- 19-24 currently unused
--
--  Completely deterministic in world space.
--========================================================--

local CELL_SIZE = 24.0
local BORDER_WIDTH = 2.0

------------------------------------------------------------

local function mix(h)
    h = math.abs(h)
    return (h * 1103515245 + 12345) % 2147483647
end

------------------------------------------------------------

local function featurePoint(cx, cz)

    local hx = mix(util.hash(cx, cz))
    local hz = mix(util.hash(cz, cx))

    local ox = (hx % 100000) / 100000.0
    local oz = (hz % 100000) / 100000.0

    return
        (cx + ox) * CELL_SIZE,
        (cz + oz) * CELL_SIZE,
        hx
end

------------------------------------------------------------

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)

    local gridX = math.floor(x / CELL_SIZE)
    local gridZ = math.floor(z / CELL_SIZE)

    local nearest = math.huge
    local second = math.huge

    local nearestDistance = 0
    local winnerHash = 0

    --------------------------------------------------------
    -- Find nearest feature point
    --------------------------------------------------------

    for dz = -1, 1 do
        for dx = -1, 1 do

            local px, pz, h =
                featurePoint(
                    gridX + dx,
                    gridZ + dz
                )

            local vx = x - px
            local vz = z - pz

            local d = vx * vx + vz * vz

            if d < nearest then

                second = nearest
                nearest = d

                nearestDistance = math.sqrt(d)
                winnerHash = h

            elseif d < second then

                second = d

            end

        end
    end

    --------------------------------------------------------
    -- Border
    --------------------------------------------------------

    local edgeDistance =
        math.sqrt(second) -
        math.sqrt(nearest)

    if edgeDistance < BORDER_WIDTH then
        return 0
    end

    --------------------------------------------------------
    -- Pick one of the six colour groups
    --------------------------------------------------------

    local group = winnerHash % 6

    local base = group * 3 + 1

    --------------------------------------------------------
    -- Ring calculation
    --------------------------------------------------------

    local radius = nearestDistance / CELL_SIZE

    if radius < 0.18 then
        return base

    elseif radius < 0.36 then
        return base + 1

    else
        return base + 2
    end

end

return pattern