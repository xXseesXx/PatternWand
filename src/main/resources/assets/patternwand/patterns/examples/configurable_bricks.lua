-- Configurable Brick Wall Pattern
-- Demonstrates the new pattern metadata and parameters feature

metadata = {
    name = "Configurable Brick Wall",
    author = "PatternWand",
    parameters = {
        brickWidth = {type = "integer", default = 4, min = 2, max = 8},
        brickHeight = {type = "integer", default = 2, min = 1, max = 4},
        weathered = {type = "boolean", default = true},
        offsetPattern = {type = "string", default = "alternating"}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Get parameters with defaults
    local brickWidth = params.brickWidth or 4
    local brickHeight = params.brickHeight or 2
    local weathered = params.weathered or true
    
    -- Calculate brick position
    local brickY = util.floor(relY / brickHeight)
    local brickX = util.floor(relX / brickWidth)
    
    -- Offset alternating rows
    if util.mod(brickY, 2) == 1 then
        brickX = util.floor((relX + brickWidth / 2) / brickWidth)
    end
    
    -- Determine if this is a mortar line
    local isMortarX = util.mod(relX, brickWidth) == 0
    local isMortarY = util.mod(relY, brickHeight) == 0
    
    if isMortarX or isMortarY then
        return 1  -- Mortar
    end
    
    -- Main brick
    if weathered then
        -- Add variation based on brick position
        local hash = util.hash(brickX, brickY)
        if util.mod(hash, 10) < 2 then
            return 2  -- Weathered variation
        end
    end
    
    return 0  -- Main brick
end
