--------------------------------------------------------
-- Deterministic 3D Voronoi
--
-- Palette:
--   Slot 1 = Borders
--   Everything else = Cell fills
--
-- Uses only world coordinates.
--------------------------------------------------------

local CELL_SIZE = 12.0

-- Border thickness in blocks
local BORDER = 1.2

--------------------------------------------------------
-- Hash mixing
--------------------------------------------------------

local function mix(h)
    h = math.abs(h)
    h = (h * 1103515245 + 12345) % 2147483647
    return h
end

--------------------------------------------------------
-- Deterministic 3D feature point
--------------------------------------------------------

local function featurePoint(cx, cy, cz)

    local h1 = mix(util.hash3d(cx, cy, cz))
    local h2 = mix(util.hash3d(cz, cx, cy))
    local h3 = mix(util.hash3d(cy, cz, cx))

    local ox = (h1 % 100000) / 100000.0
    local oy = (h2 % 100000) / 100000.0
    local oz = (h3 % 100000) / 100000.0

    return
        (cx + ox) * CELL_SIZE,
        (cy + oy) * CELL_SIZE,
        (cz + oz) * CELL_SIZE,
        h1
end

--------------------------------------------------------

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)

    local gx = math.floor(x / CELL_SIZE)
    local gy = math.floor(y / CELL_SIZE)
    local gz = math.floor(z / CELL_SIZE)

    local nearest = math.huge
    local second = math.huge

    local winnerHash = 0

    ----------------------------------------------------
    -- Search neighboring cells (3×3×3)
    ----------------------------------------------------

    for dz = -1, 1 do
        for dy = -1, 1 do
            for dx = -1, 1 do

                local px, py, pz, h =
                    featurePoint(
                        gx + dx,
                        gy + dy,
                        gz + dz
                    )

                local vx = x - px
                local vy = y - py
                local vz = z - pz

                local d = vx * vx + vy * vy + vz * vz

                if d < nearest then

                    second = nearest
                    nearest = d
                    winnerHash = h

                elseif d < second then

                    second = d

                end

            end
        end
    end

    ----------------------------------------------------
    -- Draw border
    ----------------------------------------------------

    local edge = math.sqrt(second) - math.sqrt(nearest)

    if edge < BORDER then
        if palette.size() > 1 then
            return 1
        else
            return 0
        end
    end

    ----------------------------------------------------
    -- Interior block
    ----------------------------------------------------

    local usable = palette.size()

    if usable <= 1 then
        return 0
    end

    local pick = winnerHash % (usable - 1)

    if pick >= 1 then
        pick = pick + 1
    end

    return pick

end

return pattern