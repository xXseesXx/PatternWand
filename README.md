# PatternWand

A Minecraft 1.7.10 addon for [BetterBuildersWands](https://github.com/GTNewHorizons/BetterBuildersWands) that adds Lua scripting support for custom building patterns.

## ⚠️ Security Warning

**Only use pattern scripts from sources you trust!**

While PatternWand implements security measures (sandboxed Lua environment, execution timeouts, removed dangerous libraries), pattern scripts are still code that runs on your computer. Malicious patterns could potentially:
- Cause game lag or crashes through computationally expensive operations
- Consume excessive memory
- Exploit unforeseen vulnerabilities

**Best Practices:**
- Only download patterns from trusted sources (official repository, known mod authors)
- Review pattern code before using it if you're technically inclined
- Test new patterns in creative/single-player before using on servers
- Report suspicious patterns to the mod author

The included example patterns are safe and can be used as templates for creating your own.

## Features

- **Lua Scripting**: Create custom building patterns using Lua scripts
- **Pattern Metadata & Parameters**: Expose configurable parameters without editing Lua files
- **Placement Context**: Patterns can access click position, bounding box, player orientation, and world time
- **Unbreakable Wand**: Single tier with 16384 (2^14) block capacity
- **Pattern Library**: Includes example patterns like checkerboard, bricks, gradients, and noise-based terrain
- **Block Palette System**: Define and use custom block palettes with advanced selection methods
- **Noise Generation**: Built-in Perlin and Simplex noise for procedural patterns
- **Geometry Utilities**: Helper functions for spheres, boxes, rotation, and smoothstep
- **Debug Tools**: Built-in debugging with `debug.print()` for development
- **Web Simulator**: Browser-based pattern playground for rapid prototyping (see `websim/`)

## Usage

Place your Lua pattern scripts in `config/patternwand/patterns/` and use the in-game commands to load and apply them with your pattern wand.

See the example patterns in `src/main/resources/assets/patternwand/patterns/examples/` for reference.

### Web Simulator

A browser-based pattern simulator is available in the `websim/` directory for rapid prototyping without launching Minecraft:

1. Open `websim/index.html` in a modern web browser
2. Write or load example patterns
3. See instant 2D preview with adjustable parameters
4. Test patterns before using them in-game

See `websim/README.md` for full documentation.

### In-Game Commands

All commands are available under both `/patternwand` and the shorter alias `/pw`.

- `/patternwand reload` (or `/pw reload`) - Reload all pattern scripts from disk
- `/patternwand list` (or `/pw list`) - List all loaded patterns
- `/patternwand set <pattern> [param=value ...]` (or `/pw set ...`) - Set active pattern with optional parameters
  - Example: `/pw set configurable_bricks brickWidth=6 weathered=false`
  - Parameters can be in any order
  - Use `=` or `:` as separator (e.g., `size=10` or `size:10`)
  - Tab completion suggests available parameters
- `/patternwand info` (or `/pw info`) - Show info about currently held wand
- `/patternwand seed <value>` (or `/pw seed ...`) - Set custom seed for pattern randomization
- `/patternwand clearseed` (or `/pw clearseed`) - Clear custom seed (use default seed)
- `/patternwand debug <on|off>` (or `/pw debug ...`) - Enable/disable debug output for pattern development

### Lua Scripting API

Pattern scripts define a `pattern` function that returns a palette index (0-26) or `nil` to skip placing a block.

#### Pattern Metadata (Optional)

Patterns can expose metadata and configurable parameters using a dictionary format:

```lua
metadata = {
    name = "Brick Wall",
    author = "PatternWand",
    description = "Creates a realistic brick wall pattern with configurable dimensions",
    ignoreMetadata = false,
    parameters = {
        brickWidth = {type = "integer", default = 4, min = 2, max = 8},
        radius = {type = "float", default = 10.5, min = 1.0, max = 50.0},
        weathered = {type = "boolean", default = true},
        mode = {type = "string", default = "normal"}
    }
}
```

**Metadata fields:**
- `name` - Display name for the pattern (optional)
- `author` - Pattern author name (optional)
- `description` - Brief description of what the pattern does (optional)
- `ignoreMetadata` - If `true`, flood-fill ignores block rotation/metadata when matching (optional, default: `false`)
  - Use this when you want to match blocks regardless of rotation (e.g., logs in any orientation)
  - When `false`, blocks must match exactly including metadata (e.g., only match north-facing logs)
- `parameters` - Dictionary of configurable parameters (optional)

**Parameter format:**
```lua
parameterName = {type = "...", default = ..., min = ..., max = ...}
```

**Parameter types:**
- `integer` or `int` - Whole numbers (e.g., 1, 42, -5)
- `float` or `number` or `double` - Decimal numbers (e.g., 3.14, 10.5, -2.7)
- `boolean` or `bool` - True/false values
- `string` or `text` - Text values

#### Function Signature

```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Your pattern logic here
    return paletteIndex  -- 0-26, or nil to skip
end
```

#### Parameters

- `x, y, z` - Absolute world coordinates
- `relX, relY, relZ` - Coordinates relative to where you clicked
- `palette` - Palette API for accessing wand's block palette
- `noise` - Noise API for Perlin/Simplex noise generation
- `util` - Utility API for math and helper functions
- `seed` - Random seed (world seed or custom seed from command)
- `params` - Table of parameter values from metadata
- `context` - Placement context (click position, bounding box, player orientation, time)
- `debug` - Debug API for development

#### Palette API

**Basic Functions:**
- `palette.size()` - Returns 27 (number of palette slots)
- `palette.getWeight(index)` - Get stack size of slot (1-64, or 0 if empty)
- `palette.isEmpty(index)` - Check if slot is empty
- `palette.countNonEmpty()` - Count non-empty slots

**Selection Methods:**
- `palette.pickWeighted()` - Randomly select slot based on stack size weights
- `palette.pickUniform()` - Randomly select with equal probability (ignores stack size)
- `palette.pickWeightedExcept(indices)` - Weighted selection excluding specified indices (single number or table)
- `palette.pickWeightedRange(min, max)` - Weighted selection from index range

#### Context API

- `context.clickedX, clickedY, clickedZ` - Block that was clicked
- `context.clickFace` - Face that was clicked (0-5)
- `context.minX, minY, minZ` - Bounding box minimum
- `context.maxX, maxY, maxZ` - Bounding box maximum
- `context.playerYaw` - Player yaw rotation
- `context.playerPitch` - Player pitch rotation
- `context.worldTime` - Total world time in ticks
- `context.dayTime` - Day time in ticks (0-24000)

#### Noise API

All noise functions return values in range `[-1, 1]`.

- `noise.perlin(x, z)` - 2D Perlin noise
- `noise.perlin3d(x, y, z)` - 3D Perlin noise
- `noise.simplex(x, z)` - 2D Simplex noise (faster, fewer artifacts)
- `noise.simplex3d(x, y, z)` - 3D Simplex noise

#### Util API

**Basic Math:**
- `util.abs(value)` - Absolute value
- `util.floor(value)` - Round down
- `util.ceil(value)` - Round up
- `util.mod(a, b)` - Modulo (always positive)
- `util.sign(value)` - Sign (-1, 0, or 1)
- `util.clamp(value, min, max)` - Clamp value to range
- `util.lerp(a, b, t)` - Linear interpolation
- `util.smoothstep(edge0, edge1, x)` - Smooth Hermite interpolation
- `util.map(value, inMin, inMax, outMin, outMax)` - Map value from one range to another

**Distance Functions:**
- `util.distance(x1, y1, x2, y2)` - 2D Euclidean distance
- `util.distance3d(x1, y1, z1, x2, y2, z2)` - 3D Euclidean distance
- `util.manhattan(x1, y1, x2, y2)` - Manhattan distance

**Geometry Functions:**
- `util.inSphere(x, y, z, centerX, centerY, centerZ, radius)` - Check if point is in sphere
- `util.inBox(x, y, z, minX, minY, minZ, maxX, maxY, maxZ)` - Check if point is in box
- `util.rotate2D(x, y, angle)` - Rotate 2D point (returns table `{x, y}`)
- `util.rotateFace(relX, relY, relZ, face)` - Transform coordinates based on clicked face (returns table `{u, v, w}`)
  - Transforms relative coordinates so 2D patterns work correctly on any surface
  - `u` = horizontal axis (left-right when facing the surface)
  - `v` = vertical axis (up-down when facing the surface)
  - `w` = depth axis (perpendicular to surface, positive = away from surface)
  - Face values: 0=DOWN, 1=UP, 2=NORTH, 3=SOUTH, 4=WEST, 5=EAST
  - Use with `context.clickFace` to orient patterns relative to clicked face

**Hash Functions:**
- `util.hash(x, z)` - Deterministic 2D hash for pseudorandom patterns
- `util.hash3d(x, y, z)` - Deterministic 3D hash

#### Debug API

- `debug.print(...)` - Print debug message to console (only when debug mode is enabled)

Enable debug mode with `/patternwand debug on` before using patterns. Disable with `/patternwand debug off`.

## License

This project is licensed under the GNU Lesser General Public License v3.0 - see the LICENSE file for details.
