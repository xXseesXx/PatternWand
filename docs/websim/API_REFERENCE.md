# Web Simulator - API Reference Guide

This document provides quick lookup for implementing each Lua API in JavaScript for the web simulator.

## File Mapping: Mod → JavaScript

| Mod Java File | Purpose | JS Implementation |
|---------------|---------|-------------------|
| `api/PaletteAPI.java` | Block palette selection | `LuaEngine.injectPaletteAPI()` |
| `api/NoiseAPI.java` | Perlin/Simplex noise | `LuaEngine.injectNoiseAPI()` + external lib |
| `api/UtilAPI.java` | Math/geometry utilities | `LuaEngine.injectUtilAPI()` |
| `api/DebugAPI.java` | Debug output | `LuaEngine.injectDebugAPI()` |
| `api/PlacementContext.java` | Placement metadata | `context` object passed to pattern |
| `pattern/PatternMetadata.java` | Pattern metadata structure | `LuaEngine.extractMetadata()` |
| `pattern/PatternExecutor.java` | Pattern execution | `LuaEngine.execute()` |

---

## Palette API

**Mod file:** `src/main/java/com/patternwand/api/PaletteAPI.java`

### JavaScript Implementation

```javascript
function createPaletteAPI(paletteState) {
  return {
    size: () => 27,
    
    getWeight: (index) => {
      if (index < 0 || index >= 27) return 0;
      return paletteState.weights[index] || 0;
    },
    
    isEmpty: (index) => {
      return paletteState.weights[index] === 0;
    },
    
    countNonEmpty: () => {
      return paletteState.weights.filter(w => w > 0).length;
    },
    
    pickWeighted: () => {
      const totalWeight = paletteState.weights.reduce((sum, w) => sum + w, 0);
      if (totalWeight === 0) return 0;
      
      let random = Math.random() * totalWeight;
      for (let i = 0; i < 27; i++) {
        random -= paletteState.weights[i];
        if (random <= 0) return i;
      }
      return 0;
    },
    
    pickUniform: () => {
      const nonEmpty = [];
      for (let i = 0; i < 27; i++) {
        if (paletteState.weights[i] > 0) nonEmpty.push(i);
      }
      return nonEmpty[Math.floor(Math.random() * nonEmpty.length)] || 0;
    },
    
    pickWeightedExcept: (except) => {
      const excluded = Array.isArray(except) ? except : [except];
      const tempWeights = paletteState.weights.map((w, i) => 
        excluded.includes(i) ? 0 : w
      );
      
      const totalWeight = tempWeights.reduce((sum, w) => sum + w, 0);
      if (totalWeight === 0) return 0;
      
      let random = Math.random() * totalWeight;
      for (let i = 0; i < 27; i++) {
        random -= tempWeights[i];
        if (random <= 0) return i;
      }
      return 0;
    },
    
    pickWeightedRange: (min, max) => {
      if (min < 0) min = 0;
      if (max >= 27) max = 26;
      
      const rangeWeights = paletteState.weights.slice(min, max + 1);
      const totalWeight = rangeWeights.reduce((sum, w) => sum + w, 0);
      if (totalWeight === 0) return min;
      
      let random = Math.random() * totalWeight;
      for (let i = min; i <= max; i++) {
        random -= paletteState.weights[i];
        if (random <= 0) return i;
      }
      return min;
    }
  };
}
```

---

## Noise API

**Mod file:** `src/main/java/com/patternwand/api/NoiseAPI.java`

### JavaScript Implementation

Use external library: [simplex-noise](https://www.npmjs.com/package/simplex-noise) or implement manually.

```javascript
import SimplexNoise from 'simplex-noise'; // Or use CDN

function createNoiseAPI(seed) {
  const simplex = new SimplexNoise(seed);
  
  // Simple Perlin implementation (or use library)
  const perlin = new PerlinNoise(seed); // Custom or library
  
  return {
    perlin: (x, z) => {
      return perlin.noise2D(x, z); // Returns [-1, 1]
    },
    
    perlin3d: (x, y, z) => {
      return perlin.noise3D(x, y, z); // Returns [-1, 1]
    },
    
    simplex: (x, z) => {
      return simplex.noise2D(x, z); // Returns [-1, 1]
    },
    
    simplex3d: (x, y, z) => {
      return simplex.noise3D(x, y, z); // Returns [-1, 1]
    }
  };
}
```

**Note:** The mod uses `FastNoiseLite` library. For web, use `simplex-noise` package or similar.

---

## Util API

**Mod file:** `src/main/java/com/patternwand/api/UtilAPI.java`

### JavaScript Implementation

```javascript
function createUtilAPI() {
  return {
    // Basic math
    abs: (value) => Math.abs(value),
    floor: (value) => Math.floor(value),
    ceil: (value) => Math.ceil(value),
    mod: (a, b) => ((a % b) + b) % b, // Always positive
    sign: (value) => Math.sign(value),
    
    // Range operations
    clamp: (value, min, max) => Math.max(min, Math.min(max, value)),
    
    lerp: (a, b, t) => a + (b - a) * t,
    
    smoothstep: (edge0, edge1, x) => {
      const t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
      return t * t * (3 - 2 * t);
    },
    
    map: (value, inMin, inMax, outMin, outMax) => {
      return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
    },
    
    // Distance functions
    distance: (x1, y1, x2, y2) => {
      const dx = x2 - x1;
      const dy = y2 - y1;
      return Math.sqrt(dx * dx + dy * dy);
    },
    
    distance3d: (x1, y1, z1, x2, y2, z2) => {
      const dx = x2 - x1;
      const dy = y2 - y1;
      const dz = z2 - z1;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
    },
    
    manhattan: (x1, y1, x2, y2) => {
      return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    },
    
    // Geometry
    inSphere: (x, y, z, centerX, centerY, centerZ, radius) => {
      const dx = x - centerX;
      const dy = y - centerY;
      const dz = z - centerZ;
      return (dx * dx + dy * dy + dz * dz) <= (radius * radius);
    },
    
    inBox: (x, y, z, minX, minY, minZ, maxX, maxY, maxZ) => {
      return x >= minX && x <= maxX &&
             y >= minY && y <= maxY &&
             z >= minZ && z <= maxZ;
    },
    
    rotate2D: (x, y, angle) => {
      const cos = Math.cos(angle);
      const sin = Math.sin(angle);
      return {
        x: x * cos - y * sin,
        y: x * sin + y * cos
      };
    },
    
    // Hash functions (simple implementation)
    hash: (x, z) => {
      let h = (x * 374761393 + z * 668265263) | 0;
      h = (h ^ (h >>> 13)) * 1274126177 | 0;
      return (h ^ (h >>> 16)) >>> 0;
    },
    
    hash3d: (x, y, z) => {
      let h = (x * 374761393 + y * 668265263 + z * 1274126177) | 0;
      h = (h ^ (h >>> 13)) * 1911520717 | 0;
      return (h ^ (h >>> 16)) >>> 0;
    }
  };
}
```

---

## Context API

**Mod file:** `src/main/java/com/patternwand/api/PlacementContext.java`

### JavaScript Implementation

```javascript
function createContext(options = {}) {
  return {
    // Click position
    clickedX: options.clickedX || 0,
    clickedY: options.clickedY || 64,
    clickedZ: options.clickedZ || 0,
    clickFace: options.clickFace || 1, // 0=down, 1=up, 2=north, 3=south, 4=west, 5=east
    
    // Bounding box
    minX: options.minX || 0,
    minY: options.minY || 0,
    minZ: options.minZ || 0,
    maxX: options.maxX || 31,
    maxY: options.maxY || 31,
    maxZ: options.maxZ || 31,
    
    // Player orientation (radians)
    playerYaw: options.playerYaw || 0,
    playerPitch: options.playerPitch || 0,
    
    // Time
    worldTime: options.worldTime || 0,
    dayTime: options.dayTime || 0
  };
}
```

**Note:** In 2D simulator, hide most context fields by default. Only show when detected in pattern code.

---

## Debug API

**Mod file:** `src/main/java/com/patternwand/api/DebugAPI.java`

### JavaScript Implementation

```javascript
function createDebugAPI(consoleElement) {
  return {
    print: (...args) => {
      const message = args.map(arg => 
        typeof arg === 'object' ? JSON.stringify(arg) : String(arg)
      ).join(' ');
      
      // Append to debug console UI
      const line = document.createElement('div');
      line.className = 'debug-line';
      line.textContent = message;
      consoleElement.appendChild(line);
      
      // Also log to browser console
      console.log('[Pattern Debug]', ...args);
    }
  };
}
```

---

## Pattern Metadata

**Mod files:** 
- `src/main/java/com/patternwand/pattern/PatternMetadata.java`
- `src/main/java/com/patternwand/pattern/PatternParameter.java`

### JavaScript Implementation

```javascript
function extractMetadata(luaCode) {
  // Parse metadata table from Lua code
  const metadataMatch = luaCode.match(/metadata\s*=\s*{([^}]+)}/s);
  if (!metadataMatch) return null;
  
  const metadata = {
    name: null,
    author: null,
    parameters: {}
  };
  
  // Extract name
  const nameMatch = luaCode.match(/name\s*=\s*"([^"]+)"/);
  if (nameMatch) metadata.name = nameMatch[1];
  
  // Extract author
  const authorMatch = luaCode.match(/author\s*=\s*"([^"]+)"/);
  if (authorMatch) metadata.author = authorMatch[1];
  
  // Extract parameters
  const paramsMatch = luaCode.match(/parameters\s*=\s*{([^}]+)}/s);
  if (paramsMatch) {
    const paramsStr = paramsMatch[1];
    
    // Parse each parameter (simplified regex-based parser)
    const paramRegex = /(\w+)\s*=\s*{([^}]+)}/g;
    let match;
    while ((match = paramRegex.exec(paramsStr)) !== null) {
      const [, name, def] = match;
      
      const param = {};
      
      // Extract type
      const typeMatch = def.match(/type\s*=\s*"(\w+)"/);
      if (typeMatch) param.type = typeMatch[1];
      
      // Extract default
      const defaultMatch = def.match(/default\s*=\s*([^,}]+)/);
      if (defaultMatch) {
        const val = defaultMatch[1].trim();
        if (val === 'true') param.default = true;
        else if (val === 'false') param.default = false;
        else if (val.startsWith('"')) param.default = val.slice(1, -1);
        else param.default = parseFloat(val);
      }
      
      // Extract min/max
      const minMatch = def.match(/min\s*=\s*([\d.]+)/);
      if (minMatch) param.min = parseFloat(minMatch[1]);
      
      const maxMatch = def.match(/max\s*=\s*([\d.]+)/);
      if (maxMatch) param.max = parseFloat(maxMatch[1]);
      
      metadata.parameters[name] = param;
    }
  }
  
  return metadata;
}
```

### Parameter Types

| Type String | JavaScript Type | Control Type |
|-------------|-----------------|--------------|
| `integer`, `int` | `number` | `<input type="number" step="1">` |
| `float`, `number`, `double` | `number` | `<input type="number" step="0.1">` |
| `boolean`, `bool` | `boolean` | `<input type="checkbox">` |
| `string`, `text` | `string` | `<input type="text">` |

---

## Pattern Execution

**Mod file:** `src/main/java/com/patternwand/pattern/PatternExecutor.java`

### JavaScript Implementation

```javascript
class LuaEngine {
  constructor() {
    this.L = fengari.lauxlib.luaL_newstate();
    fengari.lualib.luaL_openlibs(this.L);
    this.paletteState = { weights: Array(27).fill(1) };
    this.seed = 12345;
  }
  
  loadPattern(code) {
    try {
      // Extract metadata
      const metadata = extractMetadata(code);
      
      // Load and compile Lua code
      const status = fengari.lauxlib.luaL_loadstring(this.L, fengari.to_luastring(code));
      if (status !== fengari.lua.LUA_OK) {
        const error = fengari.lua.lua_tostring(this.L, -1);
        return { error: fengari.to_jsstring(error) };
      }
      
      // Execute to define globals
      fengari.lua.lua_call(this.L, 0, 0);
      
      return { metadata, error: null };
    } catch (e) {
      return { error: e.message };
    }
  }
  
  execute(x, y, z, context, params) {
    try {
      // Get pattern function
      fengari.lua.lua_getglobal(this.L, fengari.to_luastring('pattern'));
      
      if (!fengari.lua.lua_isfunction(this.L, -1)) {
        throw new Error('pattern function not defined');
      }
      
      // Push arguments
      fengari.lua.lua_pushnumber(this.L, x);
      fengari.lua.lua_pushnumber(this.L, y);
      fengari.lua.lua_pushnumber(this.L, z);
      fengari.lua.lua_pushnumber(this.L, x - context.clickedX); // relX
      fengari.lua.lua_pushnumber(this.L, y - context.clickedY); // relY
      fengari.lua.lua_pushnumber(this.L, z - context.clickedZ); // relZ
      
      // Inject API tables (palette, noise, util, seed, params, context, debug)
      this.pushAPITable('palette', createPaletteAPI(this.paletteState));
      this.pushAPITable('noise', createNoiseAPI(this.seed));
      this.pushAPITable('util', createUtilAPI());
      fengari.lua.lua_pushnumber(this.L, this.seed);
      this.pushAPITable('params', params);
      this.pushAPITable('context', context);
      this.pushAPITable('debug', createDebugAPI(document.getElementById('console')));
      
      // Call function (13 arguments, 1 return value)
      const status = fengari.lua.lua_pcall(this.L, 13, 1, 0);
      
      if (status !== fengari.lua.LUA_OK) {
        const error = fengari.lua.lua_tostring(this.L, -1);
        console.error('Pattern error:', fengari.to_jsstring(error));
        return null;
      }
      
      // Get return value
      if (fengari.lua.lua_isnil(this.L, -1)) {
        return null;
      }
      
      const result = fengari.lua.lua_tonumber(this.L, -1);
      fengari.lua.lua_pop(this.L, 1);
      
      return Math.floor(result);
    } catch (e) {
      console.error('Execution error:', e);
      return null;
    }
  }
  
  pushAPITable(name, obj) {
    fengari.lua.lua_newtable(this.L);
    for (const [key, value] of Object.entries(obj)) {
      fengari.lua.lua_pushstring(this.L, fengari.to_luastring(key));
      if (typeof value === 'function') {
        fengari.lua.lua_pushjsfunction(this.L, value);
      } else if (typeof value === 'number') {
        fengari.lua.lua_pushnumber(this.L, value);
      } else if (typeof value === 'boolean') {
        fengari.lua.lua_pushboolean(this.L, value);
      } else {
        fengari.lua.lua_pushstring(this.L, fengari.to_luastring(String(value)));
      }
      fengari.lua.lua_settable(this.L, -3);
    }
  }
}
```

---

## Usage Detection (Smart UI)

```javascript
function detectUsage(code) {
  return {
    // API usage
    usesPalette: /palette\./.test(code),
    usesNoise: /noise\./.test(code),
    usesUtil: /util\./.test(code),
    usesContext: /context\./.test(code),
    usesParams: /params\./.test(code),
    usesSeed: /\bseed\b/.test(code) && !/params\.seed/.test(code),
    usesDebug: /debug\./.test(code),
    
    // Coordinate usage
    usesAbsoluteCoords: /\b[xyz]\b/.test(code) && !/rel[XYZ]/.test(code),
    usesRelativeCoords: /rel[XYZ]/.test(code),
    
    // Specific context fields
    contextFields: extractContextFields(code)
  };
}

function extractContextFields(code) {
  const fields = [];
  const contextProps = [
    'clickedX', 'clickedY', 'clickedZ', 'clickFace',
    'minX', 'minY', 'minZ', 'maxX', 'maxY', 'maxZ',
    'playerYaw', 'playerPitch', 'worldTime', 'dayTime'
  ];
  
  for (const prop of contextProps) {
    const regex = new RegExp(`context\\.${prop}\\b`);
    if (regex.test(code)) {
      fields.push(prop);
    }
  }
  
  return fields;
}
```

---

## Testing Checklist

### Palette API Tests
- [ ] `palette.size()` returns 27
- [ ] `palette.getWeight(i)` returns correct weight
- [ ] `palette.isEmpty(i)` works correctly
- [ ] `palette.pickWeighted()` respects weights
- [ ] `palette.pickUniform()` ignores weights
- [ ] `palette.pickWeightedExcept([0, 1])` excludes indices
- [ ] `palette.pickWeightedRange(5, 10)` only returns 5-10

### Noise API Tests
- [ ] All noise functions return values in `[-1, 1]`
- [ ] Deterministic (same input → same output)
- [ ] Different seeds produce different results

### Util API Tests
- [ ] Math functions match JavaScript's `Math.*`
- [ ] `util.mod(-1, 5)` returns 4 (positive modulo)
- [ ] `util.smoothstep(0, 1, 0.5)` returns smooth curve
- [ ] `util.hash(x, z)` is deterministic
- [ ] `util.rotate2D(1, 0, PI/2)` returns `{x: 0, y: 1}`

### Pattern Execution Tests
- [ ] Simple pattern: `return 0` renders all blocks
- [ ] Conditional pattern: `return x % 2` renders checkerboard
- [ ] Nil return: `return nil` skips block
- [ ] Metadata parsing works
- [ ] Parameters passed correctly
- [ ] Errors caught and displayed

### UI Tests
- [ ] Code changes trigger re-render (debounced)
- [ ] Palette editor updates affect rendering
- [ ] Parameter controls update `params` table
- [ ] Debug console shows `debug.print()` output
- [ ] Example patterns load correctly
