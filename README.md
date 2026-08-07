# PatternWand

A Minecraft 1.7.10 addon for [BetterBuildersWands](https://github.com/GTNewHorizons/BetterBuildersWands) that adds Lua scripting support for custom building patterns.

## Features

- **Lua Scripting**: Create custom building patterns using Lua scripts
- **Unbreakable Wand**: Single tier with 16384 (2^14) block capacity
- **Pattern Library**: Includes example patterns like checkerboard, bricks, gradients, and noise-based terrain
- **Block Palette System**: Define and use custom block palettes in your patterns
- **Noise Generation**: Built-in Perlin and Simplex noise for procedural patterns

## Usage

Place your Lua pattern scripts in `config/patternwand/patterns/` and use the in-game commands to load and apply them with your pattern wand.

See the example patterns in `src/main/resources/assets/patternwand/patterns/examples/` for reference.

### In-Game Commands

- `/patternwand reload` - Reload all pattern scripts from disk
- `/patternwand list` - List all loaded patterns
- `/patternwand set <pattern>` - Set active pattern on held wand
- `/patternwand info` - Show info about currently held wand
- `/patternwand seed <value>` - Set custom seed for pattern randomization
- `/patternwand clearseed` - Clear custom seed (use world seed)

### Lua Scripting API

Pattern scripts must define a `pattern` function that returns a palette index (0-26) or `nil` to skip placing a block.

#### Function Signature

```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
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

#### Palette API

- `palette.size()` - Returns 27 (number of palette slots)
- `palette.getWeight(index)` - Get stack size of slot (1-64, or 0 if empty)
- `palette.isEmpty(index)` - Check if slot is empty
- `palette.pickWeighted()` - Randomly select slot based on stack size weights
- `palette.countNonEmpty()` - Count non-empty slots

#### Noise API

All noise functions return values in range `[-1, 1]`.

- `noise.perlin(x, z)` - 2D Perlin noise
- `noise.perlin3d(x, y, z)` - 3D Perlin noise
- `noise.simplex(x, z)` - 2D Simplex noise (faster, fewer artifacts)
- `noise.simplex3d(x, y, z)` - 3D Simplex noise

#### Util API

- `util.hash(x, z)` - Deterministic 2D hash for pseudorandom patterns
- `util.hash3d(x, y, z)` - Deterministic 3D hash
- `util.distance(x1, y1, x2, y2)` - Euclidean distance
- `util.manhattan(x1, y1, x2, y2)` - Manhattan distance
- `util.map(value, inMin, inMax, outMin, outMax)` - Map value from one range to another
- `util.clamp(value, min, max)` - Clamp value to range
- `util.lerp(a, b, t)` - Linear interpolation
- `util.floor(value)` - Round down
- `util.ceil(value)` - Round up
- `util.abs(value)` - Absolute value

## License

This project is licensed under the GNU Lesser General Public License v3.0 - see the LICENSE file for details.
