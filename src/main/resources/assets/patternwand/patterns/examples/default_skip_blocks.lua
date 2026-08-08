metadata = {
    name = "Skip Blocks: Sparse Pattern",
    author = "PatternWand",
    parameters = {
        density = {type = "float", default = 0.3, min = 0.0, max = 1.0}
    }
}

-- Showcases: Returning nil to skip placing blocks
-- Creates sparse patterns with gaps
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Use hash for deterministic randomness
    local random = util.hash3d(x, y, z)
    
    -- Skip blocks based on density parameter
    if random > params.density then
        return nil  -- Don't place block
    end
    
    -- Place block from palette
    return palette.pickWeighted()
end
