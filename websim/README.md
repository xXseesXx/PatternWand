# PatternWand Web Simulator

A simplified 2D web-based pattern simulator for rapid prototyping and testing of Lua patterns without launching Minecraft.

## Features

- **Live Preview** - See your patterns render in real-time as you type
- **Smart UI** - Controls automatically appear based on what your pattern uses
- **Example Patterns** - Load pre-built examples to learn from
- **Interactive Canvas** - Hover to see coordinates and palette indices
- **Palette Editor** - Customize colors and weights visually
- **Parameter Controls** - Adjust pattern parameters with sliders and inputs
- **Debug Console** - See output from `debug.print()` calls

## Quick Start

1. Open `index.html` in a modern web browser (Chrome, Firefox, Edge)
2. Start typing in the code editor or load an example pattern
3. Watch the preview update automatically

## Usage

### Basic Pattern

```lua
function pattern(x, y, z, relX, relY, relZ)
    return (relX + relZ) % 2
end
```

This creates a simple checkerboard pattern.

### Using Palette

Right-click any color swatch to adjust its weight (stack size). Patterns using `palette.pickWeighted()` will respect these weights.

### Using Parameters

Define parameters in metadata to create configurable patterns:

```lua
metadata = {
    parameters = {
        size = {type = "integer", default = 4, min = 2, max = 8}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params)
    return (relX // params.size) % 2
end
```

Sliders will automatically appear for your parameters.

### Canvas Controls

- **Size Slider** - Adjust grid size (16x16 to 128x128)
- **Hover** - See coordinates and palette index for each cell
- **Grid Lines** - Automatically shown when zoomed in

### Debug Output

Use `debug.print()` in your patterns:

```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    if relX == 0 and relZ == 0 then
        debug.print("Center block!")
    end
    return 0
end
```

The debug console will appear automatically when your pattern uses debug functions.

## API Support

The simulator implements the complete PatternWand Lua API:

- ✅ **Palette API** - All methods supported
- ✅ **Noise API** - Perlin and Simplex noise (2D and 3D)
- ✅ **Util API** - All math and geometry helpers
- ✅ **Context API** - Basic context (extensible)
- ✅ **Debug API** - Console output
- ✅ **Parameters** - Full metadata support

## Smart UI Behavior

The simulator analyzes your code and shows only relevant controls:

- Type `palette.` → Palette editor appears
- Type `params.` → Parameter controls appear
- Type `debug.` → Debug console appears
- Type `seed` → Seed input appears

## Technical Details

- **Lua Runtime** - fengari (Lua 5.3 in JavaScript)
- **Noise** - SimplexNoise.js + custom Perlin implementation
- **Rendering** - HTML5 Canvas2D
- **No Build Step** - Pure HTML/CSS/JS with CDN dependencies

## Performance

- **Default size** - 32x32 grid (1024 blocks)
- **Max size** - 128x128 grid (16384 blocks)
- **Typical render** - < 100ms for 32x32
- **Debounce** - 300ms delay after typing

## Browser Requirements

- Modern browser with ES6+ support
- Tested on Chrome 90+, Firefox 88+, Edge 90+
- Does not work in Internet Explorer

## Limitations

- **2D Only** - Shows X-Z plane (Y=0), good for most patterns
- **No World Context** - Limited player/time context (can be extended)
- **Simplified Palette** - Uses colors instead of actual Minecraft blocks
- **No Block Properties** - Cannot test rotation, metadata, etc.

## Development

Files:
- `index.html` - Main HTML structure
- `styles.css` - All styling
- `engine.js` - Lua runtime wrapper with API implementations
- `renderer.js` - 2D canvas renderer
- `app.js` - Main application controller
- `lib/perlin.js` - Perlin noise implementation
- `patterns/examples.js` - Embedded example patterns

To add more examples, edit `patterns/examples.js`.

## Tips

1. **Start Simple** - Begin with basic patterns and add complexity
2. **Use Examples** - Load examples to see different API usage
3. **Test Parameters** - Adjust sliders to see how parameters affect output
4. **Debug Often** - Use `debug.print()` to understand pattern behavior
5. **Right-Click Palette** - Adjust weights by right-clicking color swatches

## License

Same as PatternWand - GNU Lesser General Public License v3.0
