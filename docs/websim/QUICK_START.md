# Web Simulator - Quick Start Guide

## For Pattern Creators

### Getting Started (30 seconds)

1. Open `websim/index.html` in your browser
2. That's it! Start coding.

### First Pattern (2 minutes)

```lua
-- Type this in the editor:
function pattern(x, y, z, relX, relY, relZ)
    return (relX + relZ) % 2
end
```

You'll see a checkerboard instantly appear!

### Using Parameters (3 minutes)

```lua
metadata = {
    parameters = {
        size = {type = "integer", default = 4, min = 2, max = 8}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
    return (math.floor(relX / params.size) + math.floor(relZ / params.size)) % 2
end
```

A slider will appear automatically!

### Using Palette (3 minutes)

1. Right-click any color swatch to set its weight
2. Use in your pattern:

```lua
function pattern(x, y, z, relX, relY, relZ, palette)
    return palette.pickWeighted()
end
```

### Debugging (2 minutes)

```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    if relX == 0 and relZ == 0 then
        debug.print("Center!")
    end
    return 0
end
```

Debug console appears automatically!

## For Developers

### File Overview

**Core Files:**
- `index.html` - Main structure, no modification needed
- `styles.css` - All styling, VS Code theme
- `engine.js` - Lua runtime + API implementations
- `renderer.js` - Canvas rendering
- `app.js` - Main controller + UI logic

**Libraries:**
- `lib/perlin.js` - Perlin noise
- `patterns/examples.js` - Example patterns

**Documentation:**
- `websim/README.md` - User guide
- `docs/websim/DESIGN.md` - Design specification
- `docs/websim/WORKFLOW.md` - Implementation guide
- `docs/websim/API_REFERENCE.md` - API implementation details
- `docs/websim/IMPLEMENTATION_SUMMARY.md` - What was built

### Architecture

```
┌─────────────────────────────────────────┐
│          SimulatorApp (app.js)          │
│  - Event handling                       │
│  - UI coordination                      │
│  - Smart UI detection                   │
└───────┬─────────────────────┬───────────┘
        │                     │
        ▼                     ▼
┌───────────────┐    ┌──────────────────┐
│  LuaEngine    │    │   Renderer       │
│  (engine.js)  │    │   (renderer.js)  │
│               │    │                  │
│  - Fengari    │    │  - Canvas2D      │
│  - APIs       │    │  - Grid drawing  │
│  - Metadata   │    │  - Colors        │
└───────────────┘    └──────────────────┘
```

### Adding New Examples

Edit `patterns/examples.js`:

```javascript
const EXAMPLES = {
    my_pattern: `-- My Pattern
function pattern(x, y, z)
    return 0
end`
};
```

Then add to HTML select:
```html
<option value="my_pattern">My Pattern</option>
```

### Extending APIs

Edit `engine.js`, find `push*API()` methods:

```javascript
pushMyAPI() {
    const api = {
        myFunction: (arg) => { /* implementation */ }
    };
    
    this.fengari.lua.lua_newtable(this.L);
    // ... push to Lua stack
}
```

### Custom Styling

Edit `styles.css`. Key variables:
- Background: `#1e1e1e`
- Editor: `#252526`
- Accent: `#007acc`

### Performance Tuning

In `app.js`:
```javascript
this.debounceTimer = setTimeout(() => {
    this.reload();
}, 300); // Adjust debounce time
```

In `renderer.js`:
```javascript
this.setGridSize(size); // Max 128
```

## Common Tasks

### Add a New Parameter Type

In `app.js`, `createParameterControl()`:

```javascript
else if (type === 'mytype') {
    const input = document.createElement('input');
    input.type = 'text';
    // ... setup
    container.appendChild(input);
}
```

### Change Default Palette

In `engine.js`, `createDefaultPalette()`:

```javascript
const colors = [
    '#custom1', '#custom2', // ...
];
```

### Add Context Fields

In `app.js`, pass more context:

```javascript
this.currentContext = {
    clickedX: 0,
    clickedY: 0,
    clickedZ: 0,
    myCustomField: 123
};
```

### Improve Error Messages

In `engine.js`, `loadPattern()`:

```javascript
if (status !== this.fengari.lua.LUA_OK) {
    const error = this.fengari.lua.lua_tostring(this.L, -1);
    // Add custom error formatting here
    return { error: enhanceError(error) };
}
```

## Testing

### Manual Test Checklist

- [ ] Load each example pattern
- [ ] Adjust grid size slider
- [ ] Change palette colors
- [ ] Right-click palette to adjust weights
- [ ] Adjust parameters (if present)
- [ ] Change seed value
- [ ] Hover over canvas (tooltip appears)
- [ ] Use debug.print() (console appears)
- [ ] Check browser console for errors

### Browser Testing

```bash
# Open in different browsers
firefox websim/index.html
chromium websim/index.html
```

### Pattern Validation

Copy patterns from mod:
```bash
cp src/main/resources/assets/patternwand/patterns/examples/bricks.lua /tmp/test.lua
# Paste into simulator, verify it renders correctly
```

## Debugging

### Browser Console

Press F12 to open developer tools. Check:
- Console for JavaScript errors
- Network tab for failed CDN loads
- Performance tab for slow renders

### Common Issues

**"pattern function not defined"**
- Check Lua syntax (missing `end`, typos)

**"fengari is not defined"**
- CDN failed to load
- Check internet connection
- Use local copy of fengari-web.js

**Canvas is blank**
- Pattern returns nil for all coordinates
- Check logic in pattern function
- Use debug.print() to see what's happening

**Controls don't appear**
- Pattern not using detected APIs
- Check detectUsage() regex in app.js

## Advanced Usage

### Local Development Server

For better debugging:

```bash
cd websim
python3 -m http.server 8000
# Open http://localhost:8000
```

### Syntax Highlighting

Add Prism.js or Monaco Editor for better UX:

```html
<script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/prism.js"></script>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism-okaidia.css">
```

### Export Patterns

Add to `app.js`:

```javascript
exportPattern() {
    const code = this.elements.codeEditor.value;
    const blob = new Blob([code], {type: 'text/plain'});
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'pattern.lua';
    a.click();
}
```

## Resources

- **Fengari Docs**: https://fengari.io/
- **Lua Reference**: https://www.lua.org/manual/5.3/
- **Canvas API**: https://developer.mozilla.org/en-US/docs/Web/API/Canvas_API
- **Simplex Noise**: https://github.com/jwagner/simplex-noise.js

## Support

Check these files for help:
1. `websim/README.md` - User documentation
2. `docs/websim/API_REFERENCE.md` - API details
3. `docs/websim/WORKFLOW.md` - Implementation guide

Happy pattern creating! 🎨
