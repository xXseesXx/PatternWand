# Strong Typing Implementation Summary

## Overview
Enhanced the PatternWand parameter system with strong typing, distinguishing between integer and float types for better type safety and clarity.

## Implementation Date
August 7, 2026

## Type System

### Before (Weak Typing)
```lua
{name = "size", type = "number", default = 10}  -- Ambiguous: int or float?
```

### After (Strong Typing)
```lua
{name = "size", type = "integer", default = 10}     -- Clear: whole numbers
{name = "scale", type = "float", default = 10.5}   -- Clear: decimals
```

## Supported Types

### 1. INTEGER
- **Aliases**: `integer`, `int`
- **Java Type**: `int` (32-bit signed integer)
- **Range**: -2,147,483,648 to 2,147,483,647
- **Use Cases**: Counts, indices, grid sizes, block positions
- **Behavior**: Decimal values are truncated to whole numbers
- **Constraints**: Support min/max validation

**Example:**
```lua
{name = "gridSize", type = "integer", default = 4, min = 2, max = 10}
```

**Validation:**
- Input: `gridSize=7.9` → Stored: `7` (truncated)
- Input: `gridSize=15` (max=10) → Stored: `10` (clamped)
- Input: `gridSize=abc` → Error: "must be an integer"

### 2. FLOAT
- **Aliases**: `float`, `number`, `double`
- **Java Type**: `double` (64-bit floating-point)
- **Precision**: ~15-17 decimal digits
- **Use Cases**: Scales, ratios, noise parameters, thresholds
- **Behavior**: Full decimal precision maintained
- **Constraints**: Support min/max validation

**Example:**
```lua
{name = "noiseScale", type = "float", default = 0.1, min = 0.01, max = 1.0}
```

**Validation:**
- Input: `noiseScale=0.125` → Stored: `0.125` (exact)
- Input: `noiseScale=2.0` (max=1.0) → Stored: `1.0` (clamped)
- Input: `noiseScale=xyz` → Error: "must be a number"

### 3. BOOLEAN
- **Aliases**: `boolean`, `bool`
- **Java Type**: `boolean`
- **Values**: true/false
- **Accepts**: `true`, `false`, `yes`, `no`, `1`, `0` (case-insensitive)
- **Use Cases**: Feature toggles, mode switches

**Example:**
```lua
{name = "enabled", type = "boolean", default = true}
```

### 4. STRING
- **Aliases**: `string`, `text`
- **Java Type**: `String`
- **Use Cases**: Mode names, pattern types, custom identifiers

**Example:**
```lua
{name = "mode", type = "string", default = "normal"}
```

## Implementation Details

### Type Recognition in Lua
**ScriptEngine.extractParameter()** recognizes multiple type aliases:

```java
case "integer":
case "int":
    type = PatternParameter.Type.INTEGER;
    break;
case "float":
case "number":
case "double":
    type = PatternParameter.Type.FLOAT;
    break;
```

**Backward Compatibility:**
- Old patterns using `type = "number"` map to FLOAT
- Existing patterns continue to work without changes

### NBT Storage

**Integers:**
```java
paramsTag.setInteger(paramName, (Integer) validatedValue);
```

**Floats:**
```java
paramsTag.setDouble(paramName, (Double) validatedValue);
```

**Different NBT tags ensure type preservation across save/load cycles.**

### Lua Value Passing

**ScriptEngine.executePattern()** passes correct types:

```java
if (value instanceof Integer) {
    luaParams.set(entry.getKey(), LuaValue.valueOf((Integer) value));
} else if (value instanceof Number) {
    luaParams.set(entry.getKey(), LuaValue.valueOf(((Number) value).doubleValue()));
}
```

This ensures integers arrive as integers in Lua, not as floats.

## Benefits

### 1. Type Safety
Parameters have explicit types that are validated and enforced:
- **Compile-time**: Type defined in metadata
- **Command-time**: Type validated when setting parameter
- **Runtime**: Type guaranteed in pattern function

### 2. Self-Documenting Code
```lua
-- Old (ambiguous)
{name = "size", type = "number", default = 10}

-- New (clear intent)
{name = "gridSize", type = "integer", default = 10}  -- Block count
{name = "noiseScale", type = "float", default = 0.1} -- Decimal ratio
```

### 3. Appropriate Precision
- **Integers**: No floating-point rounding errors for counts/indices
- **Floats**: Full precision for mathematical calculations

### 4. Better Validation
```lua
{name = "blockCount", type = "integer", default = 5, min = 1, max = 100}
```
- Ensures whole numbers (no `5.7` blocks)
- Range validated at integer precision

### 5. Performance (Minor)
- Integer operations faster than floating-point
- Lua can optimize integer paths

## Usage Examples

### Integer Use Cases
```lua
-- Grid-based patterns
{name = "gridSize", type = "integer", default = 4, min = 2, max = 10}

-- Stripe patterns
{name = "stripeWidth", type = "integer", default = 3, min = 1, max = 8}

-- Layer counts
{name = "layers", type = "integer", default = 5, min = 1, max = 20}

-- Repetition counts
{name = "repeat", type = "integer", default = 2, min = 1, max = 10}
```

### Float Use Cases
```lua
-- Noise scaling
{name = "noiseScale", type = "float", default = 0.1, min = 0.01, max = 1.0}

-- Thresholds
{name = "threshold", type = "float", default = 0.5, min = 0.0, max = 1.0}

-- Scaling factors
{name = "scale", type = "float", default = 1.0, min = 0.1, max = 10.0}

-- Ratios
{name = "aspectRatio", type = "float", default = 1.618, min = 0.1, max = 5.0}
```

## Example Pattern

See `type_demo.lua` for a comprehensive example:
```lua
metadata = {
    parameters = {
        {name = "gridSize", type = "integer", default = 4, min = 2, max = 10},
        {name = "noiseScale", type = "float", default = 0.1, min = 0.01, max = 1.0}
    }
}
```

Command usage:
```
/patternwand set type_demo gridSize=6 noiseScale=0.15
```

## Files Modified

1. **PatternParameter.java**
   - Split `NUMBER` type into `INTEGER` and `FLOAT`
   - Added separate validation logic for each
   - Added `getTypeName()` and `getDescription()` methods

2. **ScriptEngine.java**
   - Updated type parsing to recognize `int`, `integer`, `float`, `double`
   - Backward compatible: `number` → `FLOAT`

3. **PatternWandCommand.java**
   - Store integers using `setInteger()` NBT method
   - Store floats using `setDouble()` NBT method

4. **PatternWandWorker.java**
   - Extract integers using `getInteger()` NBT method
   - Extract floats using `getDouble()` NBT method

5. **Example Patterns**
   - `configurable_bricks.lua` - Uses `integer` for brick dimensions
   - `spherical_dome.lua` - Uses `float` for radius (can be 10.5)
   - `type_demo.lua` - NEW: Demonstrates all type features

6. **Documentation**
   - `README.md` - Updated with type system documentation
   - `PARAMETER_USAGE.md` - Extensive type safety section

## Testing

### Compilation
✅ Clean build successful
✅ All type checks pass
✅ No compilation warnings

### Type Validation
✅ Integer parameters reject decimals (truncate)
✅ Float parameters preserve decimals
✅ Both types support min/max constraints
✅ Type mismatches show clear error messages

### Backward Compatibility
✅ Old patterns with `type = "number"` work (map to FLOAT)
✅ Existing NBT data loads correctly
✅ No breaking changes to API

## Future Enhancements (Optional)

### Additional Types (mentioned as "fever dream")
```lua
-- Vector type
{name = "offset", type = "vector3", default = {0, 0, 0}}

-- Color type
{name = "tint", type = "color", default = {r=255, g=255, b=255}}

-- Enum type
{name = "mode", type = "enum", values = {"normal", "inverted", "random"}, default = "normal"}

-- Range type
{name = "heightRange", type = "range", default = {min=0, max=10}}

-- BlockState type
{name = "block", type = "blockstate", default = "minecraft:stone:0"}
```

These would require:
- Custom validation logic
- Special NBT storage format
- Lua table structures
- Tab completion for enum values

## Status

✅ **COMPLETE**

Strong typing fully implemented with:
- ✅ INTEGER and FLOAT types distinct
- ✅ Type validation and conversion
- ✅ Proper NBT storage by type
- ✅ Lua integration with correct types
- ✅ Backward compatible
- ✅ Full documentation
- ✅ Example patterns
