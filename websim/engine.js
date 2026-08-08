// Lua Engine - Wraps fengari and injects all APIs
class LuaEngine {
    constructor() {
        this.fengari = fengari;
        this.L = null;
        this.paletteState = this.createDefaultPalette();
        this.seed = 12345;
        this.debugConsole = null;
        this.currentMetadata = null;
        this.initLua();
    }

    createDefaultPalette() {
        const colors = [
            '#4a4a4a', '#7f7f7f', '#999999', '#b3b3b3', '#cccccc', '#e6e6e6', '#ffffff',
            '#8b4513', '#a0522d', '#cd853f', '#deb887', '#d2691e', '#f4a460', '#ffa07a',
            '#2f4f2f', '#556b2f', '#6b8e23', '#808000', '#9acd32', '#adff2f', '#7cfc00',
            '#1e3a5f', '#2e5090', '#4169e1', '#6495ed', '#87ceeb', '#add8e6', '#b0e0e6'
        ];
        
        const weights = new Array(27).fill(1);
        return { colors, weights };
    }

    initLua() {
        this.L = this.fengari.lauxlib.luaL_newstate();
        // CRITICAL: Load all Lua standard libraries (math, string, table, etc.)
        this.fengari.lualib.luaL_openlibs(this.L);
        
        console.log('Lua state initialized with standard libraries');
    }

    setDebugConsole(consoleElement) {
        this.debugConsole = consoleElement;
    }

    setPaletteState(paletteState) {
        this.paletteState = paletteState;
    }

    setSeed(seed) {
        this.seed = seed;
    }

    // Extract metadata from Lua code
    extractMetadata(code) {
        const metadata = {
            name: null,
            author: null,
            parameters: {}
        };

        // Extract name
        const nameMatch = code.match(/name\s*=\s*["']([^"']+)["']/);
        if (nameMatch) metadata.name = nameMatch[1];

        // Extract author
        const authorMatch = code.match(/author\s*=\s*["']([^"']+)["']/);
        if (authorMatch) metadata.author = authorMatch[1];

        // Extract parameters block
        const paramsMatch = code.match(/parameters\s*=\s*\{([^}]+(?:\{[^}]*\}[^}]*)*)\}/s);
        if (paramsMatch) {
            const paramsStr = paramsMatch[1];
            
            // Parse each parameter
            const paramRegex = /(\w+)\s*=\s*\{([^}]+)\}/g;
            let match;
            while ((match = paramRegex.exec(paramsStr)) !== null) {
                const [, name, def] = match;
                const param = {};

                // Extract type
                const typeMatch = def.match(/type\s*=\s*["'](\w+)["']/);
                if (typeMatch) param.type = typeMatch[1];

                // Extract default
                const defaultMatch = def.match(/default\s*=\s*([^,}]+)/);
                if (defaultMatch) {
                    const val = defaultMatch[1].trim();
                    if (val === 'true') param.default = true;
                    else if (val === 'false') param.default = false;
                    else if (val.startsWith('"') || val.startsWith("'")) {
                        param.default = val.slice(1, -1);
                    } else {
                        param.default = parseFloat(val);
                    }
                }

                // Extract min/max
                const minMatch = def.match(/min\s*=\s*([\d.-]+)/);
                if (minMatch) param.min = parseFloat(minMatch[1]);

                const maxMatch = def.match(/max\s*=\s*([\d.-]+)/);
                if (maxMatch) param.max = parseFloat(maxMatch[1]);

                metadata.parameters[name] = param;
            }
        }

        return metadata;
    }

    // Load and compile pattern
    loadPattern(code) {
        try {
            // Reinit Lua state to clear previous code
            this.initLua();

            // Extract metadata
            const metadata = this.extractMetadata(code);
            this.currentMetadata = metadata;

            // Load code
            const status = this.fengari.lauxlib.luaL_loadstring(
                this.L,
                this.fengari.to_luastring(code)
            );

            if (status !== this.fengari.lua.LUA_OK) {
                const error = this.fengari.lua.lua_tostring(this.L, -1);
                return { 
                    metadata: null, 
                    error: this.fengari.to_jsstring(error) 
                };
            }

            // Execute to define globals
            const execStatus = this.fengari.lua.lua_pcall(this.L, 0, 0, 0);
            if (execStatus !== this.fengari.lua.LUA_OK) {
                const error = this.fengari.lua.lua_tostring(this.L, -1);
                return { 
                    metadata: null, 
                    error: this.fengari.to_jsstring(error) 
                };
            }

            return { metadata, error: null };
        } catch (e) {
            return { metadata: null, error: e.message };
        }
    }

    // Execute pattern function for a single coordinate
    execute(x, y, z, context, params) {
        try {
            // Get pattern function
            this.fengari.lua.lua_getglobal(this.L, this.fengari.to_luastring('pattern'));

            if (!this.fengari.lua.lua_isfunction(this.L, -1)) {
                console.warn('Pattern function not found');
                this.fengari.lua.lua_pop(this.L, 1);
                return null;
            }

            // Push coordinate arguments (absolute world coordinates)
            this.fengari.lua.lua_pushnumber(this.L, x);
            this.fengari.lua.lua_pushnumber(this.L, y);
            this.fengari.lua.lua_pushnumber(this.L, z);
            
            // Push relative coordinates (relative to clicked position)
            const relX = x - (context.clickedX || 0);
            const relY = y - (context.clickedY || 0);
            const relZ = z - (context.clickedZ || 0);
            
            this.fengari.lua.lua_pushnumber(this.L, relX);
            this.fengari.lua.lua_pushnumber(this.L, relY);
            this.fengari.lua.lua_pushnumber(this.L, relZ);

            // Push API tables
            this.pushPaletteAPI();
            this.pushNoiseAPI();
            this.pushUtilAPI();
            this.fengari.lua.lua_pushnumber(this.L, this.seed);
            this.pushParamsTable(params || {});
            this.pushContextTable(context || {});
            this.pushDebugAPI();

            // Call function (13 arguments, 1 return value)
            const status = this.fengari.lua.lua_pcall(this.L, 13, 1, 0);

            if (status !== this.fengari.lua.LUA_OK) {
                const error = this.fengari.lua.lua_tostring(this.L, -1);
                console.error('Pattern execution error:', this.fengari.to_jsstring(error));
                this.fengari.lua.lua_pop(this.L, 1);
                return null;
            }

            // Get return value
            if (this.fengari.lua.lua_isnil(this.L, -1)) {
                this.fengari.lua.lua_pop(this.L, 1);
                return null;
            }

            const result = this.fengari.lua.lua_tonumber(this.L, -1);
            this.fengari.lua.lua_pop(this.L, 1);

            if (result === null || result === undefined || isNaN(result)) {
                return null;
            }

            return Math.floor(result);
        } catch (e) {
            console.error('Execution exception:', e);
            return null;
        }
    }

    // Push Palette API table
    pushPaletteAPI() {
        const self = this;
        this.fengari.lua.lua_newtable(this.L);

        const api = {
            size: () => 27,
            
            getWeight: (index) => {
                if (index < 0 || index >= 27) return 0;
                return self.paletteState.weights[index] || 0;
            },
            
            isEmpty: (index) => {
                return (self.paletteState.weights[index] || 0) === 0;
            },
            
            countNonEmpty: () => {
                return self.paletteState.weights.filter(w => w > 0).length;
            },
            
            pickWeighted: () => {
                const totalWeight = self.paletteState.weights.reduce((sum, w) => sum + w, 0);
                if (totalWeight === 0) return 0;
                
                let random = Math.random() * totalWeight;
                for (let i = 0; i < 27; i++) {
                    random -= self.paletteState.weights[i];
                    if (random <= 0) return i;
                }
                return 0;
            },
            
            pickUniform: () => {
                const nonEmpty = [];
                for (let i = 0; i < 27; i++) {
                    if (self.paletteState.weights[i] > 0) nonEmpty.push(i);
                }
                if (nonEmpty.length === 0) return 0;
                return nonEmpty[Math.floor(Math.random() * nonEmpty.length)];
            }
        };

        for (const [key, fn] of Object.entries(api)) {
            this.fengari.lua.lua_pushstring(this.L, this.fengari.to_luastring(key));
            this.fengari.lua.lua_pushjsfunction(this.L, fn);
            this.fengari.lua.lua_settable(this.L, -3);
        }
    }

    // Push Noise API table
    pushNoiseAPI() {
        const perlin = new PerlinNoise(this.seed);
        
        // Check if SimplexNoise is available
        let simplex = null;
        if (typeof SimplexNoise !== 'undefined') {
            try {
                simplex = new SimplexNoise(this.seed.toString());
                console.log('SimplexNoise initialized successfully');
            } catch (e) {
                console.warn('SimplexNoise initialization failed, using Perlin fallback:', e);
            }
        } else {
            console.warn('SimplexNoise not available, using Perlin fallback');
        }

        const api = {
            perlin: (x, z) => {
                try {
                    const result = perlin.noise2D(x, z);
                    if (isNaN(result)) {
                        console.error('Perlin returned NaN for', x, z);
                        return 0;
                    }
                    return result;
                } catch (e) {
                    console.error('Perlin error:', e);
                    return 0;
                }
            },
            perlin3d: (x, y, z) => {
                try {
                    const result = perlin.noise3D(x, y, z);
                    if (isNaN(result)) {
                        console.error('Perlin3D returned NaN for', x, y, z);
                        return 0;
                    }
                    return result;
                } catch (e) {
                    console.error('Perlin3D error:', e);
                    return 0;
                }
            },
            simplex: (x, z) => {
                try {
                    if (simplex) {
                        const result = simplex.noise2D(x, z);
                        if (isNaN(result)) {
                            console.error('Simplex returned NaN for', x, z);
                            return 0;
                        }
                        return result;
                    }
                    // Fallback to perlin
                    return perlin.noise2D(x, z);
                } catch (e) {
                    console.error('Simplex error:', e);
                    return 0;
                }
            },
            simplex3d: (x, y, z) => {
                try {
                    if (simplex) {
                        const result = simplex.noise3D(x, y, z);
                        if (isNaN(result)) {
                            console.error('Simplex3D returned NaN for', x, y, z);
                            return 0;
                        }
                        return result;
                    }
                    // Fallback to perlin
                    return perlin.noise3D(x, y, z);
                } catch (e) {
                    console.error('Simplex3D error:', e);
                    return 0;
                }
            }
        };

        this.fengari.lua.lua_newtable(this.L);
        for (const [key, fn] of Object.entries(api)) {
            this.fengari.lua.lua_pushstring(this.L, this.fengari.to_luastring(key));
            this.fengari.lua.lua_pushjsfunction(this.L, fn);
            this.fengari.lua.lua_settable(this.L, -3);
        }
        
        console.log('Noise API injected with methods:', Object.keys(api));
    }

    // Push Util API table
    pushUtilAPI() {
        const api = {
            abs: Math.abs,
            floor: Math.floor,
            ceil: Math.ceil,
            mod: (a, b) => ((a % b) + b) % b,
            sign: Math.sign,
            clamp: (value, min, max) => Math.max(min, Math.min(max, value)),
            lerp: (a, b, t) => a + (b - a) * t,
            smoothstep: (edge0, edge1, x) => {
                const t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
                return t * t * (3 - 2 * t);
            },
            map: (value, inMin, inMax, outMin, outMax) => {
                return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
            },
            distance: (x1, y1, x2, y2) => {
                const dx = x2 - x1, dy = y2 - y1;
                return Math.sqrt(dx * dx + dy * dy);
            },
            distance3d: (x1, y1, z1, x2, y2, z2) => {
                const dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
                return Math.sqrt(dx * dx + dy * dy + dz * dz);
            },
            manhattan: (x1, y1, x2, y2) => Math.abs(x2 - x1) + Math.abs(y2 - y1),
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

        this.fengari.lua.lua_newtable(this.L);
        for (const [key, fn] of Object.entries(api)) {
            this.fengari.lua.lua_pushstring(this.L, this.fengari.to_luastring(key));
            this.fengari.lua.lua_pushjsfunction(this.L, fn);
            this.fengari.lua.lua_settable(this.L, -3);
        }
    }

    // Push params table
    pushParamsTable(params) {
        this.fengari.lua.lua_newtable(this.L);
        for (const [key, value] of Object.entries(params || {})) {
            this.fengari.lua.lua_pushstring(this.L, this.fengari.to_luastring(key));
            
            if (typeof value === 'boolean') {
                this.fengari.lua.lua_pushboolean(this.L, value);
            } else if (typeof value === 'number') {
                this.fengari.lua.lua_pushnumber(this.L, value);
            } else {
                this.fengari.lua.lua_pushstring(this.L, this.fengari.to_luastring(String(value)));
            }
            
            this.fengari.lua.lua_settable(this.L, -3);
        }
    }

    // Push context table
    pushContextTable(context) {
        this.fengari.lua.lua_newtable(this.L);
        for (const [key, value] of Object.entries(context || {})) {
            this.fengari.lua.lua_pushstring(this.L, this.fengari.to_luastring(key));
            this.fengari.lua.lua_pushnumber(this.L, value);
            this.fengari.lua.lua_settable(this.L, -3);
        }
    }

    // Push Debug API table
    pushDebugAPI() {
        const self = this;
        this.fengari.lua.lua_newtable(this.L);

        const print = (...args) => {
            const message = args.map(arg =>
                typeof arg === 'object' ? JSON.stringify(arg) : String(arg)
            ).join(' ');

            if (self.debugConsole) {
                const line = document.createElement('div');
                line.className = 'debug-line';
                line.textContent = message;
                self.debugConsole.appendChild(line);
                self.debugConsole.scrollTop = self.debugConsole.scrollHeight;
            }

            console.log('[Pattern]', message);
        };

        this.fengari.lua.lua_pushstring(this.L, this.fengari.to_luastring('print'));
        this.fengari.lua.lua_pushjsfunction(this.L, print);
        this.fengari.lua.lua_settable(this.L, -3);
    }
}
