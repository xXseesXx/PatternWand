--------------------------------------------------------
-- Deterministic Voronoi v2
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
-- Deterministic feature point
--------------------------------------------------------

local function featurePoint(cx, cz)

    local h1 = mix(util.hash(cx, cz))
    local h2 = mix(util.hash(cz, cx))

    local ox = (h1 % 100000) / 100000.0
    local oz = (h2 % 100000) / 100000.0

    return
        (cx + ox) * CELL_SIZE,
        (cz + oz) * CELL_SIZE,
        h1
end

--------------------------------------------------------

function pattern(x,y,z,relX,relY,relZ,palette,noise,seed)

    local gx = math.floor(x / CELL_SIZE)
    local gz = math.floor(z / CELL_SIZE)

    local nearest = math.huge
    local second = math.huge

    local winnerHash = 0

    ----------------------------------------------------
    -- Search neighbouring cells
    ----------------------------------------------------

    for dz = -1,1 do
        for dx = -1,1 do

            local px,pz,h =
                featurePoint(
                    gx+dx,
                    gz+dz
                )

            local vx = x - px
            local vz = z - pz

            local d = vx*vx + vz*vz

            if d < nearest then

                second = nearest
                nearest = d
                winnerHash = h

            elseif d < second then

                second = d

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

    local pick =
        (winnerHash % (usable - 1))

    if pick >= 1 then
        pick = pick + 1
    end

    return pick

end

return pattern