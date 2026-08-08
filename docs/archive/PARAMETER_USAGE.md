# Pattern Parameters - Usage Guide

This guide demonstrates how to use pattern parameters with PatternWand.

## Basic Usage

### 1. Setting a Pattern with Default Parameters

```
/patternwand set configurable_bricks
```

This uses the pattern with all default parameter values defined in the metadata.

### 2. Setting a Pattern with Custom Parameters

```
/patternwand set configurable_bricks brickWidth=6 weathered=false
```

You can specify any number of parameters. Parameters not specified will use their default values.

### 3. Parameter Order Doesn't Matter

Both of these commands are equivalent:
```
/patternwand set configurable_bricks brickWidth=6 weathered=false
/patternwand set configurable_bricks weathered=false brickWidth=6
```

### 4. Using Both = and : Separators

Both separators work:
```
/patternwand set configurable_bricks brickWidth=6
/patternwand set configurable_bricks brickWidth:6
```

## Tab Completion

The command supports intelligent tab completion:

1. **Pattern Names**: After typing `/patternwand set `, press TAB to see available patterns
2. **Parameters**: After selecting a pattern, press TAB to see available parameters
3. **Boolean Values**: For boolean parameters, TAB suggests `true` and `false`
4. **Already Used**: Tab completion automatically excludes parameters you've already set

## Example Patterns with Parameters

### Configurable Bricks
```lua
metadata = {
    name = "Configurable Brick Wall",
    parameters = {
        brickWidth = {type = "integer", default = 4, min = 2, max = 8},
        brickHeight = {type = "integer", default = 2, min = 1, max = 4},
        weathered = {type = "boolean", default = true}
    }
}
```

Usage examples:
```
/patternwand set configurable_bricks
/patternwand set configurable_bricks brickWidth=3
/patternwand set configurable_bricks brickWidth=8 brickHeight=3
/patternwand set configurable_bricks weathered=false
/patternwand set configurable_bricks brickWidth=5 brickHeight=2 weathered=true
```

### Spherical Dome
```lua
metadata = {
    name = "Spherical Dome",
    parameters = {
        radius = {type = "float", default = 10, min = 3, max = 50},
        hollow = {type = "boolean", default = false}
    }
}
```

Usage examples:
```
/patternwand set spherical_dome radius=20
/patternwand set spherical_dome radius=15 hollow=true
/patternwand set spherical_dome hollow=true
```

### Random Mix
```lua
metadata = {
    name = "Random Mix",
    parameters = {
        mode = {type = "string", default = "uniform"}
    }
}
```

Usage examples:
```
/patternwand set random_mix mode=uniform
/patternwand set random_mix mode=weighted
/patternwand set random_mix mode=checkerboard
```

## Parameter Types

PatternWand supports strongly-typed parameters to ensure type safety and proper validation.

### Integer Parameters
- **Type names**: `integer`, `int`
- Whole numbers only (no decimals)
- Automatically truncated if decimal provided
- Can have min/max constraints
- Examples: 1, 42, -5, 100

```lua
{name = "size", type = "integer", default = 10, min = 1, max = 50}
{name = "count", type = "int", default = 5}
```

Usage: `size=25` or `count=10`

**Validation:**
- `size=10.7` → stored as `10` (truncated)
- `size=100` with max=50 → clamped to `50`
- `size=0` with min=1 → clamped to `1`

### Float Parameters
- **Type names**: `float`, `number`, `double`
- Decimal numbers (floating-point)
- Can have min/max constraints
- Full precision maintained
- Examples: 3.14, 10.5, -2.7, 100.0

```lua
{name = "radius", type = "float", default = 10.5, min = 1.0, max = 50.0}
{name = "scale", type = "number", default = 1.0}
```

Usage: `radius=15.5` or `scale=2.5`

**Validation:**
- `radius=15.7` → stored as `15.7` (exact)
- `radius=100.0` with max=50.0 → clamped to `50.0`

### Boolean Parameters
- **Type names**: `boolean`, `bool`
- True or false values
- Accepts: `true`, `false`, `yes`, `no`, `1`, `0`
- Case insensitive

```lua
{name = "enabled", type = "boolean", default = true}
{name = "inverted", type = "bool", default = false}
```

Usage: `enabled=false` or `enabled=true`

### String Parameters
- **Type names**: `string`, `text`
- Any text value
- No quotes needed in command
- No validation beyond type checking

```lua
{name = "mode", type = "string", default = "normal"}
{name = "pattern", type = "text", default = "checkerboard"}
```

Usage: `mode=special` or `pattern=diagonal`

## Type Safety Benefits

### 1. **Compile-Time Type Definition**
Parameters have explicit types defined in metadata, making patterns self-documenting:
```lua
{name = "width", type = "integer", default = 4}  -- Clear: expects whole numbers
{name = "scale", type = "float", default = 1.0}  -- Clear: expects decimals
```

### 2. **Automatic Type Conversion**
Values are automatically converted to the correct type:
```
/patternwand set pattern width=10    → Stored as integer 10
/patternwand set pattern scale=2.5   → Stored as float 2.5
/patternwand set pattern enabled=yes → Stored as boolean true
```

### 3. **Range Validation**
Numeric types support min/max constraints:
```lua
{name = "size", type = "integer", default = 10, min = 1, max = 100}
```
- `size=0` → Clamped to 1
- `size=150` → Clamped to 100
- `size=50` → Used as-is

### 4. **Error Prevention**
Invalid values are caught early:
```
/patternwand set pattern size=abc
§cInvalid value for parameter 'size': must be an integer
```

### 5. **Lua Integration**
Parameters arrive in Lua with correct types:
```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    local width = params.width    -- Guaranteed to be integer
    local scale = params.scale    -- Guaranteed to be float
    local enabled = params.enabled -- Guaranteed to be boolean
    
    -- No type checking needed in pattern code!
end
```

## Checking Current Parameters

Use `/patternwand info` to see:
- Currently active pattern
- Custom seed (if set)
- World seed (if no custom seed)

## Parameter Persistence

Parameters are stored in the wand's NBT data and persist:
- Across game sessions
- When the wand is dropped and picked up
- When moving the wand in inventory

To reset to defaults, use `/patternwand set <pattern>` without parameters.

## Creating Patterns with Parameters

See `configurable_bricks.lua` for a complete example:

```lua
metadata = {
    name = "My Pattern",
    author = "YourName",
    parameters = {
        size = {type = "integer", default = 5, min = 1, max = 20},
        inverted = {type = "boolean", default = false}
    }
}

function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    -- Access parameters from params table
    local size = params.size or 5
    local inverted = params.inverted or false
    
    -- Use parameters in your pattern logic
    if util.mod(relX, size) == 0 then
        return inverted and 1 or 0
    end
    
    return inverted and 0 or 1
end
```

## Troubleshooting

### Unknown Parameter Warning
If you specify a parameter that doesn't exist in the pattern's metadata:
```
§eWarning: Unknown parameter 'badname' (ignored)
```

The pattern will still be set, but the unknown parameter is ignored.

### Invalid Value
If you provide an invalid value for a parameter type:
```
§cInvalid value for parameter 'size': must be a number
```

The pattern will not be set. Fix the parameter value and try again.

### No Parameters Available
If a pattern doesn't define any parameters, you can't set any:
```
/patternwand set checkerboard
```

The old patterns without metadata still work perfectly, they just don't support parameters.
