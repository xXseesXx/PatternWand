# Pattern Wand Architecture Diagram

## Complete System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           MINECRAFT PLAYER                              │
│                                                                         │
│  1. Holds Pattern Wand                                                 │
│  2. Right-clicks on blocks                                             │
│  3. Uses commands: /patternwand seed 12345                            │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      ItemPatternWand.onItemUse()                        │
│                                                                         │
│  • Detects right-click                                                 │
│  • Gets palette from wand NBT                                          │
│  • Creates PatternWandWorker                                           │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        PatternWandWorker                                │
│                                                                         │
│  getBlockPositionList()  ────────┐                                     │
│  • Flood-fill algorithm          │                                     │
│  • Palette-based matching        │                                     │
│  • Returns list of positions     │                                     │
│                                   │                                     │
│  placeBlocksWithPattern()  ◄─────┘                                     │
│  • Gets active pattern from NBT                                        │
│  • Gets seed: getPatternSeed()  ────┐                                 │
│  • Executes pattern for each block   │                                 │
└──────────────────────────────────────┼──────────────────────────────────┘
                                       │
                ┌──────────────────────┴──────────────────────┐
                │                                             │
                ▼                                             ▼
        ┌──────────────┐                          ┌──────────────────────┐
        │  Custom Seed │                          │     World Seed       │
        │   from NBT   │                          │  world.getSeed()     │
        │              │                          │                      │
        │  Per-Wand    │                          │  Deterministic       │
        │  Variation   │                          │  by coordinates      │
        └──────┬───────┘                          └──────┬───────────────┘
               │                                         │
               └─────────────────┬───────────────────────┘
                                 │
                                 ▼
                         ┌───────────────┐
                         │  Final Seed   │
                         └───────┬───────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        ScriptEngine.executePattern()                    │
│                                                                         │
│  1. Create API objects with seed:                                      │
│     • NoiseAPI(seed)                                                   │
│     • PaletteAPI(inventory, seed)                                      │
│     • UtilAPI()                                                        │
│                                                                         │
│  2. Wrap APIs for Lua:                                                 │
│     • luaNoise = LuaNoiseWrapper.wrap(noiseAPI)                       │
│     • luaPalette = LuaPaletteWrapper.wrap(paletteAPI)                 │
│     • luaUtil = LuaUtilWrapper.wrap(utilAPI)                          │
│                                                                         │
│  3. Call Lua function:                                                 │
│     pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)    │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          LUA SCRIPT EXECUTION                           │
│                                                                         │
│  function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)│
│      local value = noise.perlin(x * 0.05, z * 0.05)                   │
│      if value > 0 then                                                 │
│          return 0  -- palette index                                    │
│      else                                                              │
│          return 1                                                      │
│      end                                                               │
│  end                                                                   │
│  return pattern                                                        │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    API WRAPPER LAYER (NEW!)                             │
│                                                                         │
│  LuaNoiseWrapper          LuaPaletteWrapper       LuaUtilWrapper       │
│  ┌──────────────┐         ┌──────────────┐        ┌──────────────┐    │
│  │ perlin()     │         │ size()       │        │ hash()       │    │
│  │ perlin3d()   │         │ getWeight()  │        │ distance()   │    │
│  │ simplex()    │         │ isEmpty()    │        │ map()        │    │
│  │ simplex3d()  │         │ pickWeighted()│        │ clamp()      │    │
│  └──────┬───────┘         └──────┬───────┘        └──────┬───────┘    │
│         │                        │                       │             │
│         └────────────────────────┼───────────────────────┘             │
│                                  │                                     │
└──────────────────────────────────┼──────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       JAVA API IMPLEMENTATION                           │
│                                                                         │
│  NoiseAPI                PaletteAPI              UtilAPI               │
│  ┌──────────────┐       ┌──────────────┐        ┌──────────────┐      │
│  │ PerlinNoise  │       │ IInventory   │        │ Math funcs   │      │
│  │ SimplexNoise │       │ Random       │        │ Helpers      │      │
│  └──────────────┘       └──────────────┘        └──────────────┘      │
└─────────────────────────────────────────────────────────────────────────┘
```

## Seed Determinism Flow

### OLD BEHAVIOR (Non-Deterministic)
```
Player clicks at (100, 64, 200)
        │
        ▼
seed = hash(100, 200)  = 12345
        │
        ▼
Pattern placed at world coords (500, 64, 300)
        │
        ▼
noise.perlin(500, 300) with seed 12345 → 0.456
        │
        ▼
Block type: Stone

─────────────────────────────────────

Player clicks at (150, 64, 250)  ← DIFFERENT CLICK
        │
        ▼
seed = hash(150, 250)  = 67890  ← DIFFERENT SEED!
        │
        ▼
Pattern placed at world coords (500, 64, 300)  ← SAME COORDS
        │
        ▼
noise.perlin(500, 300) with seed 67890 → -0.234  ← DIFFERENT VALUE!
        │
        ▼
Block type: Cobblestone  ← DIFFERENT BLOCK!

❌ NOT DETERMINISTIC BY WORLD COORDINATES
```

### NEW BEHAVIOR (Deterministic)
```
Player clicks at (100, 64, 200)
        │
        ▼
seed = world.getSeed() = 8675309
        │
        ▼
Pattern placed at world coords (500, 64, 300)
        │
        ▼
noise.perlin(500, 300) with seed 8675309 → 0.456
        │
        ▼
Block type: Stone

─────────────────────────────────────

Player clicks at (150, 64, 250)  ← DIFFERENT CLICK
        │
        ▼
seed = world.getSeed() = 8675309  ← SAME SEED!
        │
        ▼
Pattern placed at world coords (500, 64, 300)  ← SAME COORDS
        │
        ▼
noise.perlin(500, 300) with seed 8675309 → 0.456  ← SAME VALUE!
        │
        ▼
Block type: Stone  ← SAME BLOCK!

✅ DETERMINISTIC BY WORLD COORDINATES
```

## Wrapper Layer Detail

### How Java APIs Become Lua Tables

```
┌─────────────────────────────────────────────────────────────────┐
│                         JAVA SIDE                               │
│                                                                 │
│  NoiseAPI api = new NoiseAPI(seed);                            │
│                                                                 │
│  public double perlin(double x, double z) {                    │
│      return perlinNoise.noise(x, z);                           │
│  }                                                             │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ LuaNoiseWrapper.wrap(api)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      WRAPPER LAYER                              │
│                                                                 │
│  LuaTable table = new LuaTable();                              │
│                                                                 │
│  table.set("perlin", new TwoArgFunction() {                    │
│      @Override                                                 │
│      public LuaValue call(LuaValue x, LuaValue z) {           │
│          double dx = x.checkdouble();    ┐                     │
│          double dz = z.checkdouble();    │ Type conversion     │
│          double result = api.perlin(dx, dz);  ← Java call      │
│          return LuaValue.valueOf(result); ─┘ Lua conversion    │
│      }                                                         │
│  });                                                           │
│                                                                 │
│  return table;                                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Passed to Lua script
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                         LUA SIDE                                │
│                                                                 │
│  local value = noise.perlin(x, z)                              │
│                       │     │   │                              │
│                       │     │   └─► z parameter                │
│                       │     └─────► x parameter                │
│                       └───────────► Lua table with functions   │
└─────────────────────────────────────────────────────────────────┘
```

## Command Flow

```
┌──────────────────────────────────────────────────────────────┐
│  Player types: /patternwand seed 12345                       │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│         PatternWandCommand.processCommand()                  │
│                                                              │
│  1. Parse command arguments                                 │
│  2. Check player is holding wand                            │
│  3. Call handleSeed()                                       │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│              PatternWandCommand.handleSeed()                 │
│                                                              │
│  1. Parse seed value (12345)                                │
│  2. Get wand ItemStack                                      │
│  3. Get/create NBT tag                                      │
│  4. tag.setLong("patternSeed", 12345)                       │
│  5. Send confirmation message                               │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                    WAND NBT DATA                             │
│                                                              │
│  {                                                           │
│    "activePattern": "noise_terrain.lua",                    │
│    "patternSeed": 12345,              ← Stored here        │
│    "palette": [ ... ]                                       │
│  }                                                           │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         │ Next time wand is used
                         ▼
┌──────────────────────────────────────────────────────────────┐
│        PatternWandWorker.getPatternSeed()                    │
│                                                              │
│  1. Check NBT for "patternSeed"                             │
│  2. Found! Return 12345                                     │
│  3. Use this seed for pattern execution                     │
└──────────────────────────────────────────────────────────────┘
```

## File Structure

```
PatternWandMod/
│
├── src/main/java/com/patternwand/
│   │
│   ├── patterns/scripted/
│   │   ├── ScriptEngine.java                    [MODIFIED]
│   │   ├── CompiledScript.java
│   │   ├── PatternScriptLoader.java
│   │   │
│   │   └── api/
│   │       ├── NoiseAPI.java
│   │       ├── PaletteAPI.java
│   │       ├── UtilAPI.java
│   │       ├── LuaNoiseWrapper.java            [NEW]
│   │       ├── LuaPaletteWrapper.java          [NEW]
│   │       └── LuaUtilWrapper.java             [NEW]
│   │
│   ├── items/
│   │   ├── ItemPatternWand.java
│   │   └── PatternWandWorker.java              [MODIFIED]
│   │
│   └── commands/
│       └── PatternWandCommand.java             [MODIFIED]
│
└── Documentation/
    ├── LUA_API_WRAPPER.md                      [NEW]
    ├── PATTERN_SEED_FIX.md                     [NEW]
    ├── CHANGES_SUMMARY.md                      [NEW]
    ├── COMMAND_REFERENCE.md                    [NEW]
    ├── IMPLEMENTATION_SUMMARY.md               [NEW]
    └── ARCHITECTURE.md                         [NEW - This file]
```

## Data Flow: Single Block Placement

```
Block at world position (500, 64, 300)
        │
        ▼
PatternWandWorker calls ScriptEngine
        │
        │ Parameters:
        │ • x=500, y=64, z=300 (world coords)
        │ • relX=10, relY=5, relZ=15 (relative to origin)
        │ • seed=8675309 (world seed)
        ▼
ScriptEngine creates APIs with seed
        │
        ├─► NoiseAPI(8675309)
        ├─► PaletteAPI(inventory, 8675309)
        └─► UtilAPI()
        │
        ▼
Wrap APIs in Lua tables
        │
        ├─► luaNoise = LuaNoiseWrapper.wrap(...)
        ├─► luaPalette = LuaPaletteWrapper.wrap(...)
        └─► luaUtil = LuaUtilWrapper.wrap(...)
        │
        ▼
Call Lua pattern function
        │
        │ pattern(500, 64, 300, 10, 5, 15, luaPalette, luaNoise, luaUtil, 8675309)
        ▼
Lua script executes
        │
        │ local value = noise.perlin(500 * 0.05, 300 * 0.05)
        │                           (25.0,        15.0)
        ▼
LuaNoiseWrapper.perlin() called
        │
        │ 1. checkdouble(25.0) → double x = 25.0
        │ 2. checkdouble(15.0) → double z = 15.0
        │ 3. api.perlin(25.0, 15.0)
        ▼
NoiseAPI.perlin() executes
        │
        │ With seed 8675309, coords (25.0, 15.0)
        │ PerlinNoise generates deterministic value
        ▼
Returns 0.456
        │
        ▼
Lua script continues
        │
        │ if 0.456 > 0 then return 0 else return 1 end
        │ → returns 0
        ▼
Palette index 0 selected
        │
        │ PaletteEntry[0] = Stone
        ▼
Place Stone block at (500, 64, 300)
```

## Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      KEY IMPROVEMENTS                           │
│                                                                 │
│  1. Lua API Wrappers                                           │
│     ✅ Clean Java→Lua bridge                                   │
│     ✅ Type-safe function calls                                │
│     ✅ Better performance                                      │
│     ✅ Easy to extend                                          │
│                                                                 │
│  2. Deterministic Seeds                                        │
│     ✅ World-coordinate based                                  │
│     ✅ Reproducible patterns                                   │
│     ✅ Custom seed support                                     │
│     ✅ Command management                                      │
│                                                                 │
│  Result: Robust, maintainable, predictable pattern system     │
└─────────────────────────────────────────────────────────────────┘
```
