-- Random Mix Pattern
-- Demonstrates the new palette selection methods

metadata = {
    name = "Random Mix",
    author = "PatternWand",
    parameters = {
        mode = {type = "string", default = "uniform"},
        excludeFirst = {type = "boolean", default = false}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local mode = params.mode or "uniform"
    local excludeFirst = params.excludeFirst or false
    
    -- Use different palette selection methods
    if mode == "uniform" then
        -- Each non-empty slot has equal probability
        return palette.pickUniform()
        
    elseif mode == "weighted" then
        -- Probability based on stack size
        if excludeFirst then
            return palette.pickWeightedExcept(0)
        else
            return palette.pickWeighted()
        end
        
    elseif mode == "range" then
        -- Only use first half of palette
        local paletteCount = palette.countNonEmpty()
        local midPoint = util.floor(paletteCount / 2)
        return palette.pickWeightedRange(0, midPoint)
        
    elseif mode == "checkerboard" then
        -- Checkerboard pattern with two different selection methods
        local isEven = util.mod(relX + relZ, 2) == 0
        if isEven then
            return palette.pickWeightedRange(0, 2)
        else
            return palette.pickWeightedRange(3, 5)
        end
    end
    
    return palette.pickWeighted()
end
