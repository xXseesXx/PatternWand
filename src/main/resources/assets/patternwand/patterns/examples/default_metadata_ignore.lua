metadata = {
    name = "Metadata: Ignore Block Metadata",
    author = "PatternWand",
    ignoreMetadata = true
}

-- Showcases: ignoreMetadata flag
-- When true, flood-fill matches blocks regardless of rotation/metadata
-- Useful for replacing logs in any orientation, stairs, etc.
-- This pattern just fills with alternating blocks
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local checker = util.mod(util.floor(relX) + util.floor(relY) + util.floor(relZ), 2)
    return checker
end
