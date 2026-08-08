metadata = {
    name = "Util: Hash Functions",
    author = "PatternWand",
    parameters = {
        use3d = {type = "boolean", default = false}
    }
}

-- Showcases: util.hash() and util.hash3d()
-- Deterministic pseudorandom patterns using hash functions
-- Same coordinates always produce same random value
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local hashValue
    
    if params.use3d then
        -- 3D hash - considers all three dimensions
        hashValue = util.hash3d(x, y, z)
    else
        -- 2D hash - ignores Y, creates vertical columns
        hashValue = util.hash(x, z)
    end
    
    -- Hash returns value in [0, 1], map to palette
    local index = util.floor(hashValue * palette.size())
    return util.mod(index, palette.size())
end
