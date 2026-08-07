# Pattern Wand Scripting System - Simplified Plan

## Goal
Add programmable patterns to Pattern Wand using **simple, pragmatic scripting** - no security theater.

## Core Principle
**This is a client-side Minecraft mod.** The user already has full control of their game. We don't need enterprise-grade sandboxing. We need simple, working pattern generation.

---

## What We're Building

### Input → Processing → Output
```
Coordinates + Palette → [Script does math] → Block index (or nil)
```

Simple pure function: `(numbers) -> number`

---

## Architecture

### 1. Pattern Interface
```java
public interface IPatternScript {
    /**
     * @return Palette index (0-26) or -1 for gap
     */
    int getBlock(int x, int y, int z, 
                 int relX, int relY, int relZ,
                 PatternPalette palette, 
                 NoiseAPI noise, 
                 long seed);
}
```

### 2. Scripting Engine - **LuaJ** (KEEP)
**Why Lua:**
- ✅ Lightweight (~300KB)
- ✅ Fast (JIT compilation)
- ✅ Simple syntax (easier than JavaScript for math)
- ✅ Already used in many Minecraft mods
- ✅ Pure Java (no native dependencies)

**Script Format:**
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    -- User's pattern logic
    local value = noise.perlin(x * 0.1, z * 0.1)
    if value > 0 then
        return 0  -- Palette slot 0
    else
        return 1  -- Palette slot 1
    end
end

return pattern
```

### 3. Security - **MINIMAL** (SIMPLIFY)

#### What We KEEP:
✅ **Basic timeout** - Kill script after 10 seconds (prevent accidental infinite loops)
```java
// Simple thread-based timeout
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<Integer> future = executor.submit(() -> script.call(...));
try {
    return future.get(10, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    future.cancel(true);
    throw new PatternException("Script timeout");
}
```

#### What We REMOVE:
❌ ~~Instruction counting~~  
❌ ~~Memory guards~~  
❌ ~~Library whitelisting/blacklisting~~  
❌ ~~Java class access prevention~~  
❌ ~~Coroutine blocking~~  
❌ ~~File I/O blocking~~  
❌ ~~Debug library sanitization~~  
❌ ~~Penetration testing~~  
❌ ~~Incident response plans~~  

**Rationale:** If the user writes malicious code, they're only griefing themselves. They already installed the mod with full Java access.

### 4. Noise API - **ESSENTIAL** (KEEP & SIMPLIFY)

Provide **4 core noise functions** - enough for 95% of use cases:

```java
public class NoiseAPI {
    private final PerlinNoise perlin;
    private final SimplexNoise simplex;
    
    public NoiseAPI(long seed) {
        this.perlin = new PerlinNoise(seed);
        this.simplex = new SimplexNoise(seed);
    }
    
    // 2D Perlin noise [-1, 1]
    public double perlin(double x, double z) { }
    
    // 3D Perlin noise [-1, 1]
    public double perlin3d(double x, double y, double z) { }
    
    // 2D Simplex noise [-1, 1]
    public double simplex(double x, double z) { }
    
    // 3D Simplex noise [-1, 1]
    public double simplex3d(double x, double y, double z) { }
}
```

**Skip for now:** Voronoi, Worley, FBM, Ridged, Turbulence. Add later if users request.

### 5. Palette API - **ESSENTIAL** (KEEP)

```java
public class PaletteAPI {
    private final List<PaletteEntry> entries;
    private final Random random;
    
    // Number of palette slots
    public int size() { }
    
    // Get weight (stack size) of palette slot
    public int getWeight(int index) { }
    
    // Pick random slot weighted by stack sizes
    public int pickWeighted() { }
}
```

### 6. Utility API - **NICE TO HAVE**

Basic math helpers:
```java
public class UtilAPI {
    // Hash for pseudorandom
    public int hash(int x, int z) { }
    
    // Distance functions
    public double distance(double x1, double y1, double x2, double y2) { }
    public double manhattan(double x1, double y1, double x2, double y2) { }
    
    // Range mapping
    public double map(double value, double inMin, double inMax, 
                     double outMin, double outMax) { }
}
```

---

## Implementation Plan

### Phase 1: Core System (Week 1)
1. **Add LuaJ dependency** to build.gradle
2. **Create ScriptEngine wrapper**
   - Compile Lua scripts
   - Execute with basic timeout (10s)
   - Bind APIs (noise, palette, util)
3. **Implement PerlinNoise.java** (2D + 3D)
4. **Implement SimplexNoise.java** (2D + 3D)
5. **Create ScriptedPattern** implementing IPlacementPattern
6. **Test with simple checkerboard pattern**

### Phase 2: Integration (Week 1-2)
1. **Pattern loader** - Load .lua files from config directory
2. **Integrate with GUI** - Script patterns show in pattern selector
3. **Error handling** - User-friendly error messages
4. **Script caching** - Don't recompile on every use

### Phase 3: Polish (Week 2)
1. **Example patterns** (5 minimum):
   - `checkerboard.lua` - Simple alternating
   - `noise_terrain.lua` - Natural stone mix
   - `bricks.lua` - Brick pattern with mortar
   - `ripples.lua` - Circular waves
   - `gradient.lua` - Smooth transitions
2. **Documentation** - Pattern author guide
3. **Testing** - Real-world usage

### Phase 4: Optional Enhancements (Later)
- More noise functions (if requested)
- Live reload (file watching)
- Pattern metadata
- In-game pattern tester

---

## File Structure

```
PatternWandMod/
├── src/main/java/com/patternwand/
│   ├── patterns/
│   │   ├── IPlacementPattern.java (existing)
│   │   └── scripted/
│   │       ├── ScriptedPattern.java        [NEW]
│   │       ├── ScriptEngine.java           [NEW]
│   │       ├── PatternScriptLoader.java    [NEW]
│   │       └── api/
│   │           ├── NoiseAPI.java           [NEW]
│   │           ├── PaletteAPI.java         [NEW]
│   │           └── UtilAPI.java            [NEW]
│   └── noise/
│       ├── PerlinNoise.java                [NEW]
│       └── SimplexNoise.java               [NEW]
└── config/patternwand/patterns/
    ├── examples/
    │   ├── checkerboard.lua
    │   ├── noise_terrain.lua
    │   ├── bricks.lua
    │   ├── ripples.lua
    │   └── gradient.lua
    └── README.txt
```

---

## Dependencies

### Add to build.gradle:
```groovy
dependencies {
    compile 'org.luaj:luaj-jse:3.0.1'
}
```

**Size:** ~300KB  
**License:** MIT (compatible)

---

## Example Usage

### Simple Pattern Script
```lua
-- checkerboard.lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    if (relX + relZ) % 2 == 0 then
        return 0
    else
        return 1
    end
end

return pattern
```

### Noise-Based Pattern
```lua
-- noise_terrain.lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)
    local value = noise.perlin(x * 0.05, z * 0.05)
    
    -- Map noise to palette
    if value > 0.3 then
        return 0  -- Stone
    elseif value > 0 then
        return 1  -- Cobble
    elseif value > -0.3 then
        return 2  -- Andesite
    else
        return palette.pickWeighted()  -- Random from palette
    end
end

return pattern
```

---

## Testing Strategy

### Unit Tests
- ✅ Perlin noise generates correct values
- ✅ Simplex noise generates correct values
- ✅ Script compilation works
- ✅ Script execution returns valid values
- ✅ Timeout kills hung scripts

### Integration Tests
- ✅ Pattern loads from file
- ✅ Pattern integrates with wand
- ✅ Blocks place correctly
- ✅ Palette API works

### User Testing
- ✅ Example patterns work
- ✅ Error messages are clear
- ✅ Performance is acceptable

---

## What We're NOT Doing

❌ Complex sandboxing (unnecessary for client-side mod)  
❌ Instruction counting (overkill)  
❌ Memory limits (trust the user)  
❌ Library whitelisting (not needed)  
❌ Advanced noise functions (not yet)  
❌ Multiple scripting languages (Lua only)  
❌ Server-side execution (client-only)  
❌ Pattern sharing/marketplace (not now)  

---

## Success Criteria

### Must Have (MVP)
1. ✅ User can write Lua pattern scripts
2. ✅ Scripts have access to noise functions
3. ✅ Scripts have access to palette
4. ✅ Basic timeout prevents hangs
5. ✅ 5 working example patterns
6. ✅ Clear error messages
7. ✅ Performance <1ms per block

### Nice to Have (Later)
- Pattern hot reload
- More noise types
- Pattern debugger
- Pattern metadata

---

## Timeline

**Week 1:** Core system + noise implementation  
**Week 2:** Integration + examples + testing  
**Total:** 2 weeks for MVP

---

## Risk Assessment

### Low Risk
- LuaJ is stable and widely used
- Noise algorithms are well-understood
- Client-side only means fewer complications

### Medium Risk
- Performance might need optimization → Add caching if needed
- Users might want more features → Add incrementally

### Mitigation
- Start simple, add features based on real usage
- Benchmark early, optimize as needed
- Keep architecture extensible

---

## Decision Log

### Why Lua over JavaScript?
- Smaller, faster, simpler
- Better for math-heavy operations
- Already proven in Minecraft modding

### Why minimal security?
- Client-side mod = user already has full control
- Over-engineering wastes time
- Can add restrictions later if needed

### Why limited noise functions initially?
- Perlin + Simplex cover 95% of use cases
- Can add more based on user demand
- Keeps initial implementation simple

---

## Next Steps

1. ✅ Get approval on this plan
2. 🔲 Add LuaJ dependency
3. 🔲 Implement noise functions
4. 🔲 Create script engine
5. 🔲 Build first example pattern
6. 🔲 Test and iterate

---

**Document Version:** 1.0  
**Date:** 2026-08-06  
**Status:** Proposed - Awaiting Approval
