# PatternWand

A Minecraft 1.7.10 addon for BetterBuildersWands ([original by Portablejim](https://github.com/Portablejim/BetterBuildersWands), [GTNH fork](https://github.com/GTNewHorizons/BetterBuildersWands)) that adds Lua scripting support for custom building patterns.

## Features

- **Lua Scripting**: Create custom building patterns using Lua scripts
- **Unbreakable Wand**: Single tier with 16384 (2^14) block capacity
- **Pattern Library**: Includes example patterns like checkerboard, bricks, gradients, and noise-based terrain
- **Block Palette System**: Define and use custom block palettes in your patterns
- **Noise Generation**: Built-in Perlin and Simplex noise for procedural patterns

## Usage

Place your Lua pattern scripts in `config/patternwand/patterns/` and use the in-game commands to load and apply them with your pattern wand.

See the example patterns in `src/main/resources/assets/patternwand/patterns/examples/` for reference.

## License

This project is licensed under the GNU Lesser General Public License v3.0 - see the LICENSE file for details.
