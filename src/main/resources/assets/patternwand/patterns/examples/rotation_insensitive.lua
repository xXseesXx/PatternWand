-- Rotation-Insensitive Pattern
-- Demonstrates ignoreMetadata flag for matching blocks regardless of rotation
-- Useful for logs, stairs, pistons, and other directional blocks

metadata = {
    name = "Rotation Insensitive",
    author = "PatternWand",
    ignoreMetadata = true,  -- Ignore block rotation/orientation during flood-fill
    parameters = {
        pattern = {type = "string", default = "checkerboard"}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- With ignoreMetadata = true, flood-fill will match all blocks of the same type
    -- regardless of their rotation. For example:
    -- - Logs oriented vertically, horizontally (E-W), or horizontally (N-S) all match
    -- - Stairs facing different directions all match
    -- - Pistons pointing different directions all match
    
    -- This is useful when you want to replace/fill a structure built with rotated blocks
    
    -- Simple checkerboard pattern using two palette slots
    if params.pattern == "checkerboard" then
        local sum = relX + relY + relZ
        if util.mod(sum, 2) == 0 then
            return 0  -- First palette slot
        else
            return 1  -- Second palette slot
        end
    
    -- Alternating pattern
    elseif params.pattern == "alternating" then
        return util.mod(relY, palette.countNonEmpty())
    
    -- Random weighted selection
    elseif params.pattern == "random" then
        return palette.pickWeighted()
    
    -- Solid fill with first block
    else
        return 0
    end
end
