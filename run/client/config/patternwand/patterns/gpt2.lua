-- Recursive Voronoi Illusion

function pattern(x,y,z,relX,relY,relZ,palette,noise,seed)

    ----------------------------------------------------
    -- Large cells
    ----------------------------------------------------

    local largeCell = noise.voronoi(x*0.035,z*0.035)
    local largeEdge = noise.worley(x*0.035,z*0.035)

    -- Always draw the large borders
    if largeEdge < 0.045 then
        return 7
    end

    ----------------------------------------------------
    -- Decide whether this cell gets subdivided
    ----------------------------------------------------

    local selector = math.abs(util.hash(largeCell,seed)) % 100

    if selector < 45 then

        ------------------------------------------------
        -- Small Voronoi inside this parent cell
        ------------------------------------------------

        local smallScale = 0.14

        local smallCell =
            noise.voronoi(
                x*smallScale,
                z*smallScale
            )

        local smallEdge =
            noise.worley(
                x*smallScale,
                z*smallScale
            )

        if smallEdge < 0.03 then
            return 6
        end

        local index =
            math.abs(
                util.hash(
                    smallCell,
                    largeCell
                )
            ) % 6

        return index

    end

    ----------------------------------------------------
    -- Leave other parent cells untouched
    ----------------------------------------------------

    return math.abs(largeCell)%6

end

return pattern