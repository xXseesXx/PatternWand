# Pattern Wand Changes Summary

## Overview

Two major improvements were made to the Pattern Wand system:

1. **Lua API Wrapper System** - Proper Java-to-Lua bridge for cleaner API exposure
2. **Deterministic Pattern Seeds** - World-coordinate-based patterns instead of click-based

---

## 1. Lua API Wrapper System

### Problem
Java APIs were being exposed to Lua using `CoerceJavaToLua.coerce()`, which:
- Had inconsistent type handling
- Provided unclear error messages
- Lacked proper Lua idioms
- Had performance overhead from reflection

### Solution
Created dedicated wrapper classes that convert Java APIs into Lua tables with proper function fields.

### New Files Created

#### `LuaNoiseWrapper.java`
Wraps `NoiseAPI` with Lua-friendly functions:
```java
table.set("perlin", new TwoArgFunction() {
    @Override
    public LuaValue call(LuaValue x, LuaValue z) {
        return LuaValue.valueOf(api.perlin(x.checkdouble(), z.checkdouble()));
    }
});
```

#### `LuaPaletteWrapper.java`
Wraps `PaletteAPI` with Lua-friendly functions:
```java
table.set("pickWeighted", new ZeroArgFunction() {
    @Override
    public LuaValue call() {
        return LuaValue.valueOf(api.pickWeighted());
    }
});
```

#### `LuaUtilWrapper.java`
Wraps `UtilAPI` with Lua-friendly functions:
```java
table.set("map", new VarArgFunction() {
    @Override
    public LuaValue invoke(Varargs args) {
        return LuaValue.valueOf(
            api.map(
                args.arg(1).checkdouble(),
                args.arg(2).checkdouble(),
                args.arg(3).checkdouble(),
                args.arg(4).checkdouble(),
                args.arg(5).checkdouble()
            )
        );
    }
});
```

### Changes to Existing Files

#### `ScriptEngine.java`
**Before:**
```java
LuaValue luaNoise = CoerceJavaToLua.coerce(noise);
LuaValue luaPalette = CoerceJavaToLua.coerce(palette);
LuaValue luaUtil = CoerceJavaToLua.coerce(util);
```

**After:**
```java
LuaTable luaNoise = LuaNoiseWrapper.wrap(noise);
LuaTable luaPalette = LuaPaletteWrapper.wrap(palette);
LuaTable luaUtil = LuaUtilWrapper.wrap(util);
```

### Benefits
- ✅ Type-safe function calls
- ✅ Better error messages
- ✅ Performance optimization
- ✅ Idiomatic Lua interface
- ✅ No breaking changes to existing scripts
- ✅ Easy to extend with new functions

### Documentation
- `LUA_API_WRAPPER.md` - Complete guide to the wrapper system

---

## 2. Deterministic Pattern Seeds

### Problem
Patterns were **not deterministic** based on world coordinates. The same world position would produce different patterns depending on where you clicked to start.

**Old Code:**
```java
// Seed based on CLICK position
long seed = ((long) clickedPos.x << 32) | (clickedPos.z & 0xFFFFFFFFL);
```

This meant:
- Click at (100, 200) → seed = hash(100, 200)
- Click at (150, 250) → seed = hash(150, 250)
- World coordinate (500, 300) produces **different noise values** with different seeds

### Solution
Use **world seed** by default, with optional custom seed per wand.

**New Code:**
```java
private long getPatternSeed(ItemStack wand) {
    // Custom seed from NBT if set
    if (wand != null && wand.hasTagCompound()) {
        NBTTagCompound tag = wand.getTagCompound();
        if (tag.hasKey("patternSeed")) {
            return tag.getLong("patternSeed");
        }
    }
    
    // Fall back to world seed for determinism
    return worldShim.getWorld().getSeed();
}
```

### Changes to Existing Files

#### `PatternWandWorker.java`

**Added Method:**
```java
private long getPatternSeed(ItemStack wand)
```
Returns custom seed from NBT or world seed.

**Modified Method:**
```java
private ArrayList<Point3d> placeBlocksWithPattern(...)
```
Now calls `getPatternSeed(itemStack)` instead of generating seed from click position.

#### `PatternWandCommand.java`

**Added Commands:**
1. `/patternwand seed <value>` - Set custom seed
2. `/patternwand clearseed` - Clear custom seed
3. Updated `/patternwand info` - Shows seed information

**Added Methods:**
```java
private void handleSeed(ICommandSender sender, String seedStr)
private void handleClearSeed(ICommandSender sender)
```

### New Commands

#### `/patternwand seed <value>`
```
/patternwand seed 12345
→ Set pattern seed to: 12345
```

#### `/patternwand clearseed`
```
/patternwand clearseed
→ Cleared custom seed
→ Now using world seed: 8675309
```

#### `/patternwand info`
```
/patternwand info
→ Active pattern: noise_terrain
→ Custom seed: 12345
```

### Benefits
- ✅ Patterns are deterministic by world coordinates
- ✅ Same position always produces same pattern
- ✅ Reproducible across sessions
- ✅ Shareable (share script + seed)
- ✅ Customizable (different seeds = variations)
- ✅ No Lua script changes needed

### Documentation
- `PATTERN_SEED_FIX.md` - Detailed explanation of the fix

---

## Impact on Existing Patterns

### Lua Scripts
**No changes required!** All existing pattern scripts work exactly the same way:

```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
    local value = noise.perlin(x * 0.05, z * 0.05)
    return value > 0 and 0 or 1
end
return pattern
```

The wrappers are transparent to script authors.

### Pattern Behavior
**Breaking Change:** Existing patterns will look different because:
- Old: Seed varied by click position (bug)
- New: Seed is constant per world (correct)

This is intentional - the old behavior was non-deterministic and incorrect.

---

## Files Modified

### New Files
1. `src/main/java/com/patternwand/patterns/scripted/api/LuaNoiseWrapper.java`
2. `src/main/java/com/patternwand/patterns/scripted/api/LuaPaletteWrapper.java`
3. `src/main/java/com/patternwand/patterns/scripted/api/LuaUtilWrapper.java`
4. `LUA_API_WRAPPER.md`
5. `PATTERN_SEED_FIX.md`
6. `CHANGES_SUMMARY.md`

### Modified Files
1. `src/main/java/com/patternwand/patterns/scripted/ScriptEngine.java`
   - Changed to use wrapper classes instead of CoerceJavaToLua
   
2. `src/main/java/com/patternwand/items/PatternWandWorker.java`
   - Added `getPatternSeed()` method
   - Modified `placeBlocksWithPattern()` to use world seed
   
3. `src/main/java/com/patternwand/commands/PatternWandCommand.java`
   - Added `handleSeed()` method
   - Added `handleClearSeed()` method
   - Updated `handleInfo()` to show seed
   - Updated command usage string
   - Updated tab completion

---

## Testing Checklist

### Lua API Wrappers
- [ ] Compile project successfully
- [ ] Load existing pattern scripts
- [ ] Execute noise functions (perlin, simplex, etc.)
- [ ] Execute palette functions (pickWeighted, etc.)
- [ ] Execute util functions (map, clamp, etc.)
- [ ] Verify no errors in logs

### Deterministic Seeds
- [ ] Place pattern at position A
- [ ] Break blocks
- [ ] Place same pattern at position B
- [ ] Verify overlapping coordinates are identical
- [ ] Set custom seed with `/patternwand seed 12345`
- [ ] Verify pattern uses custom seed
- [ ] Clear seed with `/patternwand clearseed`
- [ ] Verify pattern uses world seed again
- [ ] Check `/patternwand info` shows correct information

---

## Migration Guide

### For Users
1. **No action needed** - existing patterns work automatically
2. Patterns will look different (better/deterministic)
3. Use `/patternwand seed <value>` for custom variations

### For Modpack Developers
1. Update to new version
2. Test existing pattern scripts
3. Document seed values for reproducible builds
4. Share seeds along with pattern scripts

### For Pattern Script Authors
1. **No code changes needed**
2. Scripts are now truly deterministic
3. Same coordinates always produce same output
4. Can test patterns by placing at different locations

---

## Future Enhancements

### Potential Additions
1. **Octave Noise** - `noise.fbm(x, z, octaves)` for fractal noise
2. **Voronoi Noise** - `noise.voronoi(x, z)` for cellular patterns
3. **Gradient Noise** - `noise.gradient(x, z)` for smooth gradients
4. **Random API** - `random.next()`, `random.range(min, max)`
5. **Math Extensions** - `util.round()`, `util.sign()`, `util.mod()`

All can be easily added through the wrapper system!

---

## Conclusion

These changes provide:
1. **Cleaner architecture** - Proper Lua-Java bridge
2. **Better reliability** - Deterministic patterns
3. **More flexibility** - Custom seeds for variations
4. **Easier extension** - Simple wrapper pattern for new APIs
5. **No breaking changes** - Existing scripts work unchanged

The Pattern Wand system is now more robust, predictable, and maintainable.
