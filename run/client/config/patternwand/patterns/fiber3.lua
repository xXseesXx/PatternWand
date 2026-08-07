--------------------------------------------------------
-- Infinite Fibers
--
-- Palette:
--   Slot 1 = Border
--   Slot 2+ = Fiber materials
--------------------------------------------------------

local WARP = 18.0
local SCALE = 0.045
local STRETCH = 5.0
local THICKNESS = 0.18

local abs = math.abs
local floor = math.floor

function pattern(x,y,z,relX,relY,relZ,palette,noise,util,seed)

    ----------------------------------------------------
    -- Domain warp
    ----------------------------------------------------

    local wx = x + noise.simplex3d(x*0.018,y*0.018,z*0.018) * WARP
    local wy = y + noise.simplex3d(x*0.018+43,y*0.018,z*0.018) * WARP
    local wz = z + noise.simplex3d(x*0.018,y*0.018+91,z*0.018) * WARP

    ----------------------------------------------------
    -- Stretch one axis
    ----------------------------------------------------

    wx = wx * SCALE
    wy = wy * SCALE
    wz = wz * SCALE / STRETCH

    ----------------------------------------------------
    -- Fiber density
    ----------------------------------------------------

    local density = abs(noise.simplex3d(wx,wy,wz))

    if density < THICKNESS then
        return (palette.size() > 1) and 1 or 0
    end

    ----------------------------------------------------
    -- Material selection
    ----------------------------------------------------

    local usable = palette.size()

    if usable <= 2 then
        return 0
    end

    -- One material per large fiber bundle
    local hx = floor(wx * 2)
    local hy = floor(wy * 2)
    local hz = floor(wz * 2)

    local h = abs(util.hash3d(hx,hy,hz))

    -- Number of usable materials after border
    local materials = usable - 2

    -- Palette slot 2..(usable-1)
    return (h % materials) + 2

end

return pattern