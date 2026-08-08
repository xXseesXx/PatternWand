metadata = {
    name = "Parameters: All Types",
    author = "PatternWand",
    description = "Demonstrates all parameter types with configurable stripes or noise pattern",
    parameters = {
        spacing = {type = "integer", default = 4, min = 1, max = 16},
        scale = {type = "float", default = 1.5, min = 0.1, max = 5.0},
        inverted = {type = "boolean", default = false},
        mode = {type = "string", default = "stripes"}
    }
}

-- Showcases: All parameter types (integer, float, boolean, string)
-- Demonstrates how to use each parameter type
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local result
    
    if params.mode == "stripes" then
        -- Integer parameter: spacing between stripes
        result = util.mod(util.floor(relX / params.spacing), 2)
    else
        -- Float parameter: scale for noise
        local n = noise.simplex(x * params.scale * 0.1, z * params.scale * 0.1)
        result = (n > 0) and 1 or 0
    end
    
    -- Boolean parameter: invert the pattern
    if params.inverted then
        result = 1 - result
    end
    
    return result
end
