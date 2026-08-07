--------------------------------------------------------
-- Custom Voronoi
--
-- Palette:
-- 0+ : Cell materials
-- 1  : Border material
--
-- Borders always use slot 1.
-- Cell interiors randomly use every other available slot.
--------------------------------------------------------

local CELL_SIZE = 10.0
local BORDER = 0.6

local function pointInCell(cx, cz)

    local h = math.abs(util.hash(cx, cz))

    local ox = (h % 1000) / 1000.0
    local oz = (math.floor(h / 1000) % 1000) / 1000.0

    return (cx + ox) * CELL_SIZE,
           (cz + oz) * CELL_SIZE
end

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)

    local gx = math.floor(x / CELL_SIZE)
    local gz = math.floor(z / CELL_SIZE)

    local nearest = math.huge
    local second = math.huge
    local winnerHash = 0

    ----------------------------------------------------
    -- Search neighbouring cells
    ----------------------------------------------------

    for dz = -1, 1 do
        for dx = -1, 1 do

            local cx = gx + dx
            local cz = gz + dz

            local px, pz = pointInCell(cx, cz)

            local vx = x - px
            local vz = z - pz

            local d = math.sqrt(vx * vx + vz * vz)

            if d < nearest then
                second = nearest
                nearest = d
                winnerHash = util.hash(cx, cz)

            elseif d < second then
                second = d
            end

        end
    end

    ----------------------------------------------------
    -- Border
    ----------------------------------------------------

    if second - nearest < BORDER then
        if palette.size() > 1 then
            return 1
        else
            return 0
        end
    end

    ----------------------------------------------------
    -- Count usable interior blocks
    ----------------------------------------------------

    local count = 0

    for i = 0, palette.size() - 1 do
        if i ~= 1 then
            count = count + 1
        end
    end

    if count == 0 then
        return 0
    end

    ----------------------------------------------------
    -- Pick one deterministic interior block
    ----------------------------------------------------

    local choice = math.abs(winnerHash) % count

    local current = 0

    for i = 0, palette.size() - 1 do

        if i ~= 1 then

            if current == choice then
                return i
            end

            current = current + 1
        end

    end

    return 0
end

return pattern