--------------------------------------------------------
-- Infinite Fibers
--
-- Palette 1 = fiber border
-- Others = fiber interior
--------------------------------------------------------

local SCALE = 0.08
local STRETCH = 4.0
local THICKNESS = 0.22

function pattern(x,y,z,relX,relY,relZ,palette,noise,seed)

    ----------------------------------------------------
    -- Warp space
    ----------------------------------------------------

    local wx = x + noise.simplex3d(x*0.02,y*0.02,z*0.02)*20
    local wy = y + noise.simplex3d(x*0.02+51,y*0.02,z*0.02)*20
    local wz = z + noise.simplex3d(x*0.02,y*0.02+97,z*0.02)*20

    ----------------------------------------------------
    -- Stretch into long fibers
    ----------------------------------------------------

    wx = wx * SCALE
    wy = wy * SCALE
    wz = wz * SCALE / STRETCH

    ----------------------------------------------------
    -- Cross section
    ----------------------------------------------------

    local n =
        noise.simplex3d(
            wx,
            wy,
            wz
        )

    local radius = math.abs(n)

    if radius < THICKNESS then

        if palette.size() > 1 then
            return 1
        end

        return 0

    end

    local usable = palette.size()

    if usable <= 1 then
        return 0
    end

    local h = math.abs(util.hash3d(
        math.floor(wx*12),
        math.floor(wy*12),
        math.floor(wz*12)
    ))

    local pick = h % (usable-1)

    if pick >= 1 then
        pick = pick + 1
    end

    return pick

end

return pattern