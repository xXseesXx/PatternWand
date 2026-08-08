metadata = {
    name = "Context: World Time",
    author = "PatternWand"
}

-- Showcases: context.worldTime, context.dayTime
-- Pattern changes based on time of day
-- dayTime: 0 = dawn, 6000 = noon, 12000 = dusk, 18000 = midnight
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Use day time (cycles 0-24000)
    local timeOfDay = context.dayTime
    
    -- Divide day into palette.size() segments
    local segment = util.floor((timeOfDay / 24000.0) * palette.size())
    
    return util.mod(segment, palette.size())
end
