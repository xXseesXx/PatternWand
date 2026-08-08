// Main Application Controller
class SimulatorApp {
    constructor() {
        // Core components
        this.engine = new LuaEngine();
        this.renderer = new Renderer(document.getElementById('previewCanvas'));
        
        // UI elements
        this.elements = {
            codeEditor: document.getElementById('codeEditor'),
            exampleSelect: document.getElementById('exampleSelect'),
            errorIndicator: document.getElementById('errorIndicator'),
            sizeSlider: document.getElementById('sizeSlider'),
            sizeValue: document.getElementById('sizeValue'),
            seedInput: document.getElementById('seedInput'),
            gridSizeDisplay: document.getElementById('gridSizeDisplay'),
            debugConsole: document.getElementById('debugConsole'),
            clearDebugBtn: document.getElementById('clearDebugBtn'),
            paletteSection: document.getElementById('paletteSection'),
            parametersSection: document.getElementById('parametersSection'),
            contextSection: document.getElementById('contextSection'),
            debugSection: document.getElementById('debugSection'),
            paletteEditor: document.getElementById('paletteEditor'),
            parametersContainer: document.getElementById('parametersContainer'),
            canvasTooltip: document.getElementById('canvasTooltip'),
            previewCanvas: document.getElementById('previewCanvas')
        };

        // State
        this.currentSize = 32;
        this.currentParams = {};
        // Context with world-like coordinates (clicked at origin, Y=64 like ground level)
        this.currentContext = { 
            clickedX: 0, 
            clickedY: 64, 
            clickedZ: 0,
            minX: 0,
            minY: 64,
            minZ: 0,
            maxX: 31,
            maxY: 64,
            maxZ: 31
        };
        this.debounceTimer = null;

        // Initialize
        this.init();
    }

    init() {
        console.log('Initializing simulator components...');
        
        // Create shared palette state
        const sharedPaletteState = this.engine.paletteState;
        
        // Set up engine with shared palette
        this.engine.setDebugConsole(this.elements.debugConsole);
        
        // Set up renderer with shared palette colors
        this.renderer.setPalette(sharedPaletteState.colors);

        // Initialize palette editor
        this.initPaletteEditor();

        // Set up event listeners
        this.setupEventListeners();

        // Run diagnostic tests
        this.runDiagnostics();

        // Load initial pattern
        this.onCodeChange();
        
        // Hide loading indicator
        const loadingIndicator = document.getElementById('loadingIndicator');
        if (loadingIndicator) {
            loadingIndicator.classList.add('hidden');
        }
        
        console.log('Simulator ready!');
    }

    runDiagnostics() {
        console.log('=== Running Diagnostics ===');
        
        // Test 1: Basic Lua execution
        try {
            const testCode = 'function test() return 42 end';
            const result = this.engine.loadPattern(testCode);
            console.log('✓ Lua execution:', result.error ? '❌ FAILED' : '✓ OK');
        } catch (e) {
            console.error('✗ Lua execution FAILED:', e);
        }

        // Test 2: Math library
        try {
            const testCode = 'function pattern(x,y,z) return math.floor(math.sin(x) * 10) end';
            const result = this.engine.loadPattern(testCode);
            if (!result.error) {
                const val = this.engine.execute(0, 64, 0, this.currentContext, {});
                console.log('✓ Math library:', val !== null ? '✓ OK' : '❌ FAILED');
            } else {
                console.error('✗ Math library FAILED:', result.error);
            }
        } catch (e) {
            console.error('✗ Math library FAILED:', e);
        }

        // Test 3: Palette API
        try {
            const testCode = 'function pattern(x,y,z,relX,relY,relZ,palette) return palette.size() end';
            const result = this.engine.loadPattern(testCode);
            if (!result.error) {
                const val = this.engine.execute(0, 64, 0, this.currentContext, {});
                console.log('✓ Palette API:', val === 27 ? '✓ OK' : '❌ FAILED (got ' + val + ')');
            } else {
                console.error('✗ Palette API FAILED:', result.error);
            }
        } catch (e) {
            console.error('✗ Palette API FAILED:', e);
        }

        // Test 4: Noise API
        try {
            const testCode = 'function pattern(x,y,z,relX,relY,relZ,palette,noise) return noise.perlin(0.5, 0.5) > -2 and 0 or 1 end';
            const result = this.engine.loadPattern(testCode);
            if (!result.error) {
                const val = this.engine.execute(0, 64, 0, this.currentContext, {});
                console.log('✓ Noise API:', val !== null ? '✓ OK' : '❌ FAILED');
            } else {
                console.error('✗ Noise API FAILED:', result.error);
            }
        } catch (e) {
            console.error('✗ Noise API FAILED:', e);
        }

        // Test 5: Util API
        try {
            const testCode = 'function pattern(x,y,z,relX,relY,relZ,palette,noise,util) return util.floor(5.7) end';
            const result = this.engine.loadPattern(testCode);
            if (!result.error) {
                const val = this.engine.execute(0, 64, 0, this.currentContext, {});
                console.log('✓ Util API:', val === 5 ? '✓ OK' : '❌ FAILED (got ' + val + ')');
            } else {
                console.error('✗ Util API FAILED:', result.error);
            }
        } catch (e) {
            console.error('✗ Util API FAILED:', e);
        }

        console.log('=== Diagnostics Complete ===');
    }

    setupEventListeners() {
        // Code editor
        this.elements.codeEditor.addEventListener('input', () => this.onCodeChange());

        // Example selector
        this.elements.exampleSelect.addEventListener('change', (e) => {
            if (e.target.value && EXAMPLES[e.target.value]) {
                this.elements.codeEditor.value = EXAMPLES[e.target.value];
                this.onCodeChange();
            }
            e.target.value = '';
        });

        // Size slider
        this.elements.sizeSlider.addEventListener('input', (e) => {
            this.currentSize = parseInt(e.target.value);
            this.elements.sizeValue.textContent = this.currentSize;
            this.elements.gridSizeDisplay.textContent = `${this.currentSize}x${this.currentSize}`;
            this.reload();
        });

        // Seed input
        this.elements.seedInput.addEventListener('input', (e) => {
            this.engine.setSeed(parseInt(e.target.value) || 0);
            this.reload();
        });

        // Debug console clear
        this.elements.clearDebugBtn.addEventListener('click', () => {
            this.elements.debugConsole.innerHTML = '';
        });

        // Palette preset selector
        document.getElementById('palettePresetSelect').addEventListener('change', (e) => {
            if (e.target.value) {
                this.loadPalettePreset(e.target.value);
            }
        });

        // Canvas hover tooltip
        this.elements.previewCanvas.addEventListener('mousemove', (e) => {
            this.onCanvasHover(e);
        });

        this.elements.previewCanvas.addEventListener('mouseleave', () => {
            this.elements.canvasTooltip.classList.add('hidden');
        });
    }

    onCodeChange() {
        // Debounce for performance
        clearTimeout(this.debounceTimer);
        this.debounceTimer = setTimeout(() => {
            this.reload();
        }, 300);
    }

    reload() {
        const code = this.elements.codeEditor.value;

        // Clear previous errors
        this.hideError();

        // Load pattern
        const { metadata, error } = this.engine.loadPattern(code);

        if (error) {
            this.showError(error);
            return;
        }

        // Update UI based on code analysis
        this.updateDynamicControls(code, metadata);

        // Render pattern
        try {
            this.renderPattern();
        } catch (e) {
            this.showError(`Render failed: ${e.message}`);
            console.error('Render exception:', e);
        }
    }

    renderPattern() {
        const stats = this.renderer.render((x, y, z) => {
            return this.engine.execute(x, y, z, this.currentContext, this.currentParams);
        }, this.currentSize);

        console.log(`Rendered ${stats.blocksRendered} blocks in ${stats.renderTime.toFixed(2)}ms`);
        
        if (stats.errors > 0) {
            this.showError(`Pattern executed with ${stats.errors} errors (check console)`);
        }
        
        if (stats.blocksRendered === 0) {
            console.warn('No blocks rendered - pattern returned nil for all coordinates');
        }
    }

    showError(message) {
        this.elements.errorIndicator.textContent = `Error: ${message}`;
        this.elements.errorIndicator.classList.remove('hidden');
    }

    hideError() {
        this.elements.errorIndicator.classList.add('hidden');
    }

    updateDynamicControls(code, metadata) {
        const usage = this.detectUsage(code);

        // Always show palette (user needs to set it up)
        this.elements.paletteSection.classList.remove('hidden');
        
        // Show/hide other sections based on usage
        this.elements.contextSection.classList.toggle('hidden', !usage.usesContext && !usage.usesSeed);
        this.elements.debugSection.classList.toggle('hidden', !usage.usesDebug);

        // Build parameter controls
        if (metadata && metadata.parameters && Object.keys(metadata.parameters).length > 0) {
            this.buildParameterControls(metadata.parameters);
            this.elements.parametersSection.classList.remove('hidden');
        } else {
            this.elements.parametersSection.classList.add('hidden');
        }

        // Update palette colors to match renderer
        this.updatePaletteColors();
    }

    detectUsage(code) {
        return {
            usesPalette: /palette\./.test(code),
            usesNoise: /noise\./.test(code),
            usesUtil: /util\./.test(code),
            usesContext: /context\./.test(code),
            usesParams: /params\./.test(code),
            usesSeed: /\bseed\b/.test(code) && !/params\.seed/.test(code),
            usesDebug: /debug\./.test(code)
        };
    }

    initPaletteEditor() {
        const paletteState = this.engine.paletteState;

        for (let i = 0; i < 27; i++) {
            const slotContainer = document.createElement('div');
            slotContainer.className = 'palette-slot-container';
            slotContainer.dataset.index = i;

            const slot = document.createElement('div');
            slot.className = 'palette-slot';
            slot.style.backgroundColor = paletteState.colors[i];

            const colorInput = document.createElement('input');
            colorInput.type = 'color';
            colorInput.value = paletteState.colors[i];
            colorInput.dataset.index = i;
            colorInput.title = `Slot ${i} - Click to change color`;

            colorInput.addEventListener('input', (e) => {
                const idx = parseInt(e.target.dataset.index);
                paletteState.colors[idx] = e.target.value;
                slot.style.backgroundColor = e.target.value;
                this.renderer.setPalette(paletteState.colors);
                this.reload();
            });

            const weightInput = document.createElement('input');
            weightInput.type = 'number';
            weightInput.className = 'weight-input';
            weightInput.min = '0';
            weightInput.max = '64';
            weightInput.value = paletteState.weights[i];
            weightInput.title = `Weight (stack size) for slot ${i}`;

            weightInput.addEventListener('input', (e) => {
                const idx = parseInt(slotContainer.dataset.index);
                const w = Math.max(0, Math.min(64, parseInt(e.target.value) || 0));
                paletteState.weights[idx] = w;
                e.target.value = w;
                this.reload();
            });

            slot.appendChild(colorInput);
            slotContainer.appendChild(slot);
            slotContainer.appendChild(weightInput);
            
            this.elements.paletteEditor.appendChild(slotContainer);
        }
    }

    updatePaletteColors() {
        const paletteState = this.engine.paletteState;
        const slots = this.elements.paletteEditor.querySelectorAll('.palette-slot');
        
        slots.forEach((slot, i) => {
            const colorInput = slot.querySelector('input[type="color"]');
            if (colorInput) {
                colorInput.value = paletteState.colors[i];
            }
        });

        this.renderer.setPalette(paletteState.colors);
    }

    buildParameterControls(parameters) {
        this.elements.parametersContainer.innerHTML = '';
        this.currentParams = {};

        for (const [name, def] of Object.entries(parameters)) {
            const control = this.createParameterControl(name, def);
            this.elements.parametersContainer.appendChild(control);
            
            // Set default value
            this.currentParams[name] = def.default;
        }
    }

    createParameterControl(name, def) {
        const container = document.createElement('div');
        container.className = 'param-control';

        const label = document.createElement('label');
        label.textContent = name;

        const type = def.type.toLowerCase();

        if (type === 'boolean' || type === 'bool') {
            const input = document.createElement('input');
            input.type = 'checkbox';
            input.checked = def.default || false;
            input.addEventListener('change', (e) => {
                this.currentParams[name] = e.target.checked;
                this.reload();
            });

            container.appendChild(label);
            container.appendChild(input);
        } 
        else if (type === 'integer' || type === 'int') {
            const value = document.createElement('span');
            value.className = 'param-value';
            value.textContent = def.default;

            const input = document.createElement('input');
            input.type = 'range';
            input.min = def.min || 0;
            input.max = def.max || 100;
            input.value = def.default || def.min || 0;
            input.step = 1;

            input.addEventListener('input', (e) => {
                const val = parseInt(e.target.value);
                this.currentParams[name] = val;
                value.textContent = val;
                this.reload();
            });

            container.appendChild(label);
            container.appendChild(input);
            container.appendChild(value);
        }
        else if (type === 'float' || type === 'number' || type === 'double') {
            const value = document.createElement('span');
            value.className = 'param-value';
            value.textContent = def.default;

            const input = document.createElement('input');
            input.type = 'range';
            input.min = def.min || 0;
            input.max = def.max || 10;
            input.value = def.default || def.min || 0;
            input.step = (def.max - def.min) / 100 || 0.1;

            input.addEventListener('input', (e) => {
                const val = parseFloat(e.target.value);
                this.currentParams[name] = val;
                value.textContent = val.toFixed(2);
                this.reload();
            });

            container.appendChild(label);
            container.appendChild(input);
            container.appendChild(value);
        }
        else if (type === 'string' || type === 'text') {
            // Check if this is a known enum-like parameter
            if (name === 'mode') {
                // Create dropdown for mode parameter
                const select = document.createElement('select');
                select.className = 'param-select';
                
                const modes = ['uniform', 'weighted', 'range', 'checkerboard'];
                modes.forEach(mode => {
                    const option = document.createElement('option');
                    option.value = mode;
                    option.textContent = mode;
                    if (mode === def.default) option.selected = true;
                    select.appendChild(option);
                });
                
                select.addEventListener('change', (e) => {
                    this.currentParams[name] = e.target.value;
                    this.reload();
                });
                
                container.appendChild(label);
                container.appendChild(select);
                
                // Set initial value
                this.currentParams[name] = def.default || modes[0];
            } else {
                // Regular text input
                const input = document.createElement('input');
                input.type = 'text';
                input.value = def.default || '';

                input.addEventListener('input', (e) => {
                    this.currentParams[name] = e.target.value;
                    this.reload();
                });

                container.appendChild(label);
                container.appendChild(input);
            }
        }

        return container;
    }

    loadPalettePreset(presetName) {
        const presets = {
            grayscale: [
                '#000000', '#111111', '#222222', '#333333', '#444444', '#555555', '#666666',
                '#777777', '#888888', '#999999', '#aaaaaa', '#bbbbbb', '#cccccc', '#dddddd',
                '#eeeeee', '#ffffff', '#f0f0f0', '#e0e0e0', '#d0d0d0', '#c0c0c0', '#b0b0b0',
                '#a0a0a0', '#909090', '#808080', '#707070', '#606060', '#505050'
            ],
            warm: [
                '#8b0000', '#a52a2a', '#b22222', '#dc143c', '#ff0000', '#ff4500', '#ff6347',
                '#ff7f50', '#ff8c00', '#ffa500', '#ffb347', '#ffc04c', '#ffd700', '#f0e68c',
                '#ffebcd', '#ffe4b5', '#ffdab9', '#ffa07a', '#cd853f', '#d2691e', '#8b4513',
                '#a0522d', '#deb887', '#d2b48c', '#bc8f8f', '#f4a460', '#8b4513'
            ],
            cool: [
                '#000080', '#00008b', '#0000cd', '#0000ff', '#1e90ff', '#4169e1', '#6495ed',
                '#87ceeb', '#87cefa', '#00bfff', '#add8e6', '#b0e0e6', '#afeeee', '#00ced1',
                '#48d1cc', '#40e0d0', '#00ffff', '#e0ffff', '#5f9ea0', '#4682b4', '#b0c4de',
                '#778899', '#708090', '#2f4f4f', '#008080', '#008b8b', '#20b2aa'
            ],
            rainbow: [
                '#ff0000', '#ff3300', '#ff6600', '#ff9900', '#ffcc00', '#ffff00', '#ccff00',
                '#99ff00', '#66ff00', '#33ff00', '#00ff00', '#00ff33', '#00ff66', '#00ff99',
                '#00ffcc', '#00ffff', '#00ccff', '#0099ff', '#0066ff', '#0033ff', '#0000ff',
                '#3300ff', '#6600ff', '#9900ff', '#cc00ff', '#ff00ff', '#ff00cc'
            ],
            earth: [
                '#2f4f2f', '#556b2f', '#6b8e23', '#808000', '#8b7355', '#8b4513', '#a0522d',
                '#cd853f', '#d2691e', '#daa520', '#b8860b', '#bc8f8f', '#d2b48c', '#deb887',
                '#f5deb3', '#ffe4b5', '#ffefd5', '#8b4513', '#a0522d', '#696969', '#808080',
                '#a9a9a9', '#c0c0c0', '#d3d3d3', '#2f4f2f', '#3c5c3c', '#4a6f4a'
            ]
        };

        if (presets[presetName]) {
            const paletteState = this.engine.paletteState;
            paletteState.colors = [...presets[presetName]];
            
            // Update all color inputs
            const slots = this.elements.paletteEditor.querySelectorAll('.palette-slot-container');
            slots.forEach((container, i) => {
                const colorInput = container.querySelector('input[type="color"]');
                const slot = container.querySelector('.palette-slot');
                if (colorInput && slot) {
                    colorInput.value = paletteState.colors[i];
                    slot.style.backgroundColor = paletteState.colors[i];
                }
            });

            this.renderer.setPalette(paletteState.colors);
            this.reload();
        }
    }

    onCanvasHover(e) {
        const coords = this.renderer.getGridCoords(e.clientX, e.clientY);
        
        if (coords) {
            // Use world coordinates (Y=64)
            const paletteIndex = this.engine.execute(
                coords.x, 64, coords.z, 
                this.currentContext, 
                this.currentParams
            );

            this.elements.canvasTooltip.textContent = 
                `World: x:${coords.x} y:64 z:${coords.z} | Relative: x:${coords.x} z:${coords.z} → ${paletteIndex !== null ? paletteIndex : 'nil'}`;
            this.elements.canvasTooltip.style.left = `${e.clientX + 10}px`;
            this.elements.canvasTooltip.style.top = `${e.clientY + 10}px`;
            this.elements.canvasTooltip.classList.remove('hidden');
        } else {
            this.elements.canvasTooltip.classList.add('hidden');
        }
    }
}

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    console.log('=== PatternWand Simulator Starting ===');
    
    // Check dependencies
    const checks = {
        fengari: typeof fengari !== 'undefined',
        SimplexNoise: typeof SimplexNoise !== 'undefined',
        PerlinNoise: typeof PerlinNoise !== 'undefined',
        EXAMPLES: typeof EXAMPLES !== 'undefined'
    };
    
    console.log('Dependency checks:', checks);
    
    if (!checks.fengari) {
        alert('Error: Fengari (Lua runtime) failed to load. Check your internet connection.');
        return;
    }
    
    if (!checks.PerlinNoise) {
        alert('Error: Perlin noise library failed to load.');
        return;
    }
    
    if (!checks.SimplexNoise) {
        console.warn('SimplexNoise not available, will use Perlin fallback');
    }
    
    try {
        window.app = new SimulatorApp();
        console.log('=== Simulator initialized successfully ===');
    } catch (e) {
        console.error('Failed to initialize simulator:', e);
        alert('Failed to initialize simulator. Check console for details.');
    }
});

