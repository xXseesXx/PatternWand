--------------------------------------------------------
-- Infinite Fibers (4 Types)
--
-- Palette:
--   Slot 1 = Fiber border
--   Slot 2-5 = Fiber materials
--------------------------------------------------------

local SCALE      = 0.08
local STRETCH    = 4.0
local THICKNESS  = 0.22
local HASH_SCALE = 16

local floor = math.floor
local abs = math.abs

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)

    ----------------------------------------------------
    -- Warp space
    ----------------------------------------------------

    local wx = x + noise.simplex3d(x*0.02,      y*0.02,      z*0.02)      * 20
    local wy = y + noise.simplex3d(x*0.02+51.0, y*0.02,      z*0.02)      * 20
    local wz = z + noise.simplex3d(x*0.02,      y*0.02+97.0, z*0.02)      * 20

    ----------------------------------------------------
    -- Stretch into long fibers
    ----------------------------------------------------

    wx = wx * SCALE
    wy = wy * SCALE
    wz = wz * SCALE / STRETCH

    ----------------------------------------------------
    -- Fiber cross section
    ----------------------------------------------------

    local radius = abs(noise.simplex3d(wx, wy, wz))

    if radius < THICKNESS then
        if palette.size() > 1 then
            return 1
        end
        return 0
    end

    ----------------------------------------------------
    -- Fiber type
    ----------------------------------------------------

    local usable = palette.size()

    if usable <= 2 then
        return 0
    end

    local types = usable - 2
    if types > 4 then
        types = 4
    end

    local h = abs(util.hash3d(
        floor(wx / HASH_SCALE),
        floor(wy / HASH_SCALE),
        floor(wz / HASH_SCALE)
    ))

    return (h % types) + 2

end

return pattern