# Pattern Seed Determinism Fix

## Problem Identified

The pattern system was **not deterministic** based on world coordinates. The same coordinates would produce different patterns depending on where you clicked to start the pattern.

### Root Cause

In `PatternWandWorker.placeBlocksWithPattern()`, the seed was generated from the **clicked position**:

```java
// OLD CODE (Non-deterministic)
long seed = ((long) clickedPos.x << 32) | (clickedPos.z & 0xFFFFFFFFL);
```

This meant:
- Click at (100, 64, 200) → seed based on (100, 200)
- Click at (150, 64, 250) → seed based on (150, 250)
- Even though the noise functions received world coordinates (x, y, z), the **noise generator itself** was initialized with a different seed each time

### Why This Matters

Perlin/Simplex noise is deterministic **only if the seed is constant**. The noise value at coordinates (500, 64, 300) will be different if you initialize the noise generator with different seeds.

**Example:**
```lua
-- If seed = 12345
noise.perlin(500, 300) → 0.456

-- If seed = 67890
noise.perlin(500, 300) → -0.234  -- Different!
```

## Solution

Changed the seed generation to use the **world seed** by default, with an option for custom seeds.

### Implementation

```java
private long getPatternSeed(ItemStack wand) {
    // Check for custom seed in NBT
    if (wand != null && wand.hasTagCompound()) {
        NBTTagCompound tag = wand.getTagCompound();
        if (tag.hasKey("patternSeed")) {
            return tag.getLong("patternSeed");
        }
    }

    // Fall back to world seed for deterministic patterns
    return worldShim.getWorld().getSeed();
}
```

### Behavior

1. **Default (World Seed)**: All patterns in the same world use the same seed
   - Noise at (500, 300) always produces the same value
   - Patterns are **position-deterministic** across the entire world
   - Same pattern script at (500, 300) always looks the same, no matter where you click

2. **Custom Seed**: Users can set a wand-specific seed
   - Different wands can use different seeds
   - Allows variation without changing the script
   - Still deterministic for that wand

## New Commands

### `/patternwand seed <value>`
Set a custom seed for the held wand.

```
/patternwand seed 12345
```

**Output:**
```
Set pattern seed to: 12345
This seed will be used for all pattern noise generation
```

### `/patternwand clearseed`
Clear the custom seed and revert to using world seed.

```
/patternwand clearseed
```

**Output:**
```
Cleared custom seed
Now using world seed: 8675309
```

### `/patternwand info`
Updated to show both pattern and seed information.

```
/patternwand info
```

**Output (with custom seed):**
```
Active pattern: noise_terrain
Custom seed: 12345
```

**Output (using world seed):**
```
Active pattern: noise_terrain
Using world seed: 8675309
```

## Testing

### Test 1: Determinism with World Seed

1. Create a new world
2. Place a pattern at position A
3. Break the blocks
4. Place the same pattern at position B
5. **Result**: The blocks at overlapping coordinates should be identical

### Test 2: Determinism with Custom Seed

1. Create wand A with custom seed 12345
2. Create wand B with custom seed 12345
3. Place patterns at different locations
4. **Result**: Overlapping coordinates should be identical

### Test 3: Variation with Different Seeds

1. Create wand A with custom seed 12345
2. Create wand B with custom seed 67890
3. Place patterns at the same location
4. **Result**: Patterns should be different

## Technical Details

### Noise Generation Flow

```
1. User clicks with wand
2. PatternWandWorker.placeBlocksWithPattern() is called
3. Seed is retrieved:
   - Check wand NBT for "patternSeed"
   - Fall back to world.getSeed()
4. NoiseAPI is created with this seed
5. For each block at (x, y, z):
   - noise.perlin(x, y, z) uses the SAME noise generator
   - Same coordinates → same noise value
   - Deterministic!
```

### Seed Storage

Seeds are stored in the wand's NBT data:

```java
NBTTagCompound tag = wand.getTagCompound();
tag.setLong("patternSeed", 12345);
```

This allows:
- Per-wand customization
- Persistence across game sessions
- Easy serialization

## Use Cases

### Use Case 1: Consistent World Building
**Scenario**: Building a large structure with noise-based patterns across multiple sessions.

**Solution**: Use default world seed. Patterns will always look the same at the same coordinates, even if you place them weeks apart.

### Use Case 2: Pattern Variations
**Scenario**: Want multiple variations of the same pattern script without editing code.

**Solution**: Create multiple wands with different custom seeds:
- Wand 1: `/patternwand seed 1111` → Variation A
- Wand 2: `/patternwand seed 2222` → Variation B
- Wand 3: `/patternwand seed 3333` → Variation C

### Use Case 3: Reproducible Builds
**Scenario**: Sharing patterns with other players.

**Solution**: Share both the pattern script AND the seed value:
```
Pattern: organic_terrain.lua
Seed: 42424242
Command: /patternwand seed 42424242
```

## Backward Compatibility

**Breaking Change**: Existing patterns will look different after this fix because:
- Old behavior: seed varied by click position
- New behavior: seed is constant (world seed)

However, this is the **correct** behavior for deterministic patterns. The old behavior was a bug.

## Benefits

1. **Deterministic**: Same coordinates always produce same pattern
2. **Consistent**: Patterns look the same across multiple placements
3. **Flexible**: Custom seeds allow variations without code changes
4. **Predictable**: Users can reliably reproduce patterns
5. **World-Aware**: Default behavior respects world seed

## Example Lua Script Usage

No changes needed in Lua scripts! The fix is transparent:

```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    -- noise.perlin(x, z) is now deterministic!
    local value = noise.perlin(x * 0.05, z * 0.05)
    
    if value > 0 then
        return 0
    else
        return 1
    end
end

return pattern
```

The `seed` parameter passed to the pattern function is still available and now contains the world seed (or custom seed).

## Summary

The fix ensures that **world coordinates determine pattern output**, not click position. This makes patterns:
- Deterministic
- Reproducible
- Consistent
- Predictable

Users can customize patterns through seed values without modifying Lua scripts.
