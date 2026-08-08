-- Type Demonstration Pattern
-- Shows the difference between integer and float parameters

metadata = {
    name = "Type Demo",
    author = "PatternWand",
    parameters = {
        -- Integer: whole numbers only, used for counts, indices, etc.
        gridSize = {type = "integer", default = 4, min = 2, max = 10},
        stripeWidth = {type = "int", default = 3, min = 1, max = 8},
        
        -- Float: precise decimals, used for scales, ratios, etc.
        noiseScale = {type = "float", default = 0.1, min = 0.01, max = 1.0},
        threshold = {type = "float", default = 0.5, min = 0.0, max = 1.0},
        
        -- Boolean: enable/disable features
        useNoise = {type = "boolean", default = false}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Integer parameters are whole numbers
    local gridSize = params.gridSize or 4
    local stripeWidth = params.stripeWidth or 3
    
    -- Float parameters preserve decimal precision
    local noiseScale = params.noiseScale or 0.1
    local threshold = params.threshold or 0.5
    
    -- Boolean parameters
    local useNoise = params.useNoise or false
    
    -- Debug output to show types (enable with /patternwand debug on)
    if relX == 0 and relY == 0 and relZ == 0 then
        debug.print("=== Type Demonstration ===")
        debug.print("gridSize (integer):", gridSize, "- type:", type(gridSize))
        debug.print("stripeWidth (integer):", stripeWidth, "- type:", type(stripeWidth))
        debug.print("noiseScale (float):", noiseScale, "- type:", type(noiseScale))
        debug.print("threshold (float):", threshold, "- type:", type(threshold))
        debug.print("useNoise (boolean):", useNoise, "- type:", type(useNoise))
    end
    
    -- Use integer for grid calculations (whole block divisions)
    local gridX = util.floor(relX / gridSize)
    local gridZ = util.floor(relZ / gridSize)
    local inGrid = util.mod(gridX + gridZ, 2) == 0
    
    -- Use integer for stripe patterns (exact block counts)
    local stripeIndex = util.floor(relX / stripeWidth)
    local inStripe = util.mod(stripeIndex, 2) == 0
    
    if useNoise then
        -- Use float for noise calculations (precise scaling)
        local noiseVal = noise.simplex(relX * noiseScale, relZ * noiseScale)
        -- Normalize from [-1, 1] to [0, 1]
        noiseVal = (noiseVal + 1) / 2
        
        -- Use float threshold for smooth transitions
        if noiseVal > threshold then
            return 0
        else
            return 1
        end
    else
        -- Combine integer-based patterns
        if inGrid and inStripe then
            return 0
        elseif inGrid then
            return 1
        elseif inStripe then
            return 2
        else
            return 3
        end
    end
end
