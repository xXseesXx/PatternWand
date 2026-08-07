-- Marble Pattern
-- Realistic marble veins using turbulence noise
--
-- Turbulence creates chaotic, swirling patterns that look like natural
-- marble, wood grain, or flowing water. The sine wave combined with
-- turbulence creates the characteristic wavy veins of marble.

function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- Get turbulence to distort the pattern
    local turb = noise.turbulence(x * 0.1, z * 0.1, 4)
    
    -- Create sine wave pattern and distort it with turbulence
    -- This creates the characteristic marble veins
    local wave = math.sin(x * 0.08 + turb * 4)
    
    -- Map the wave to palette indices for marble-like bands
    local paletteSize = palette.size()
    
    -- Normalize wave from [-1, 1] to [0, paletteSize-1]
    local index = math.floor((wave + 1) * 0.5 * (paletteSize - 1))
    
    return math.max(0, math.min(paletteSize - 1, index))
end

return pattern
