-- Debug Example Pattern
-- Demonstrates the debug.print() function
-- Enable with: /patternwand debug on

metadata = {
    name = "Debug Example",
    author = "PatternWand"
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Debug messages only appear when debug mode is enabled
    
    -- Print on first block only (to avoid spam)
    if relX == 0 and relY == 0 and relZ == 0 then
        debug.print("=== Pattern Execution Started ===")
        debug.print("World position:", x, y, z)
        debug.print("Clicked position:", context.clickedX, context.clickedY, context.clickedZ)
        debug.print("Bounding box:", context.minX, context.minY, context.minZ, "to", context.maxX, context.maxY, context.maxZ)
        debug.print("Player yaw:", context.playerYaw, "pitch:", context.playerPitch)
        debug.print("Palette slots:", palette.countNonEmpty())
        debug.print("Seed:", seed)
    end
    
    -- Print occasional debug info for some blocks
    if util.mod(relX + relY + relZ, 50) == 0 then
        local dist = util.distance3d(relX, relY, relZ, 0, 0, 0)
        debug.print("Block at relative", relX, relY, relZ, "distance:", dist)
    end
    
    -- Simple checkerboard pattern
    if util.mod(relX + relZ, 2) == 0 then
        return 0
    else
        return 1
    end
end
