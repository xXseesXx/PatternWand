--------------------------------------------------------
-- Deterministic 3D Voronoi (Optimized)
--
-- Palette:
--   Slot 1 = Borders
--   Everything else = Cell fills
--------------------------------------------------------

local CELL_SIZE = 12.0
local BORDER = 1.2

--------------------------------------------------------
-- Localize globals
--------------------------------------------------------

local floor = math.floor
local sqrt  = math.sqrt
local abs   = math.abs
local huge  = math.huge

local hash3d = util.hash3d

--------------------------------------------------------
-- Hash mixing
--------------------------------------------------------

local function mix(h)
    h = abs(h)
    return (h * 1103515245 + 12345) % 2147483647
end

--------------------------------------------------------

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)

    local gx = floor(x / CELL_SIZE)
    local gy = floor(y / CELL_SIZE)
    local gz = floor(z / CELL_SIZE)

    local nearest = huge
    local second  = huge
    local winnerHash = 0

    for dz = -1, 1 do

        local cellZ = (gz + dz) * CELL_SIZE

        for dy = -1, 1 do

            local cellY = (gy + dy) * CELL_SIZE

            for dx = -1, 1 do

                local cellX = (gx + dx) * CELL_SIZE

                ------------------------------------------------
                -- Single hash
                ------------------------------------------------

                local h = mix(hash3d(gx + dx, gy + dy, gz + dz))

                local ox = (h % 1024) / 1024.0
                h = mix(h)

                local oy = (h % 1024) / 1024.0
                h = mix(h)

                local oz = (h % 1024) / 1024.0

                ------------------------------------------------

                local vx = x - (cellX + ox * CELL_SIZE)
                local vy = y - (cellY + oy * CELL_SIZE)
                local vz = z - (cellZ + oz * CELL_SIZE)

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

    --------------------------------------------------------
    -- Border
    --------------------------------------------------------

    if sqrt(second) - sqrt(nearest) < BORDER then
        return (palette.size() > 1) and 1 or 0
    end

    --------------------------------------------------------
    -- Interior
    --------------------------------------------------------

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