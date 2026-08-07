-- ============================================================
-- PRISMATIC CRYSTAL CAVERNS
--
-- Uses all 8 palette slots.
--
-- Palette:
-- 0 - Core Crystal
-- 1 - Bright Crystal
-- 2 - Secondary Crystal
-- 3 - Smooth Stone
-- 4 - Dark Stone
-- 5 - Marble Veins
-- 6 - Ore / Accent
-- 7 - Rare Sparkle
--
-- No premade selection helpers.
-- Everything is driven by custom procedural math.
-- ============================================================

local PI = math.pi

local function clamp(v,a,b)
    if v < a then return a end
    if v > b then return b end
    return v
end

function pattern(x,y,z,relX,relY,relZ,palette,noise,seed)

    ---------------------------------------------------
    -- Base noise layers
    ---------------------------------------------------

    local terrain =
        noise.fbm(
            x*0.018,
            z*0.018,
            4
        )

    local ridge =
        noise.ridged(
            x*0.045,
            z*0.045,
            3
        )

    local swirl =
        noise.turbulence(
            x*0.07,
            z*0.07,
            3
        )

    local crystal =
        noise.simplex3d(
            x*0.055,
            y*0.055,
            z*0.055
        )

    local cell =
        noise.voronoi(
            x*0.055,
            z*0.055
        )

    local edge =
        noise.worley(
            x*0.06,
            z*0.06
        )

    ---------------------------------------------------
    -- Radial energy field
    ---------------------------------------------------

    local radius =
        math.sqrt(
            relX*relX +
            relZ*relZ
        )

    local rings =
        math.sin(radius*0.23 + swirl*5)

    ---------------------------------------------------
    -- Spiral modulation
    ---------------------------------------------------

    local angle =
        math.atan2(relZ,relX)

    local spiral =
        math.sin(
            angle*6 +
            radius*0.18 +
            terrain*4
        )

    ---------------------------------------------------
    -- Vertical shimmer
    ---------------------------------------------------

    local bands =
        math.sin(
            y*0.22 +
            terrain*3 +
            swirl*2
        )

    ---------------------------------------------------
    -- Deterministic sparkle
    ---------------------------------------------------

    local hash =
        math.abs(
            util.hash3d(x,y,z)
        ) % 1000

    ---------------------------------------------------
    -- Composite energy
    ---------------------------------------------------

    local energy =
        terrain*0.40 +
        ridge*0.30 +
        crystal*0.45 +
        spiral*0.25 +
        rings*0.20 -
        edge*0.25

    ---------------------------------------------------
    -- Palette mapping
    ---------------------------------------------------

    if edge < 0.06 then
        return 5          -- marble veins
    end

    if energy > 1.05 then
        return 1          -- bright crystal
    end

    if energy > 0.75 then
        return 0          -- crystal core
    end

    if crystal > 0.35 and ridge > 0.55 then
        return 2          -- secondary crystal
    end

    if ridge > 0.82 then
        return 6          -- ore accents
    end

    if bands > 0.72 then
        return 3          -- smooth stone
    end

    if rings > 0.82 then
        return 4          -- dark stone
    end

    if hash == 0 then
        return 7          -- ultra rare sparkle
    end

    return 3

end

return pattern