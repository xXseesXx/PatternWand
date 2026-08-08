# Parameter Dictionary Format - Migration Summary

## Overview
Changed parameter metadata format from a list to a dictionary for cleaner, more intuitive syntax.

## Change Date
August 7, 2026

## Before (List Format)
```lua
metadata = {
    name = "My Pattern",
    parameters = {
        {name = "brickWidth", type = "integer", default = 4, min = 2, max = 8},
        {name = "weathered", type = "boolean", default = true}
    }
}
```

## After (Dictionary Format)
```lua
metadata = {
    name = "My Pattern",
    parameters = {
        brickWidth = {type = "integer", default = 4, min = 2, max = 8},
        weathered = {type = "boolean", default = true}
    }
}
```

## Benefits

### 1. Cleaner Syntax
- No need to repeat parameter name in quotes
- More natural Lua table syntax
- Easier to read and write

### 2. Less Redundancy
**Before:**
```lua
{name = "brickWidth", type = "integer", ...}
```
**After:**
```lua
brickWidth = {type = "integer", ...}
```
The parameter name is the key, not a field.

### 3. Better IDE Support
- Dictionary format is standard Lua
- Better autocomplete in Lua editors
- Easier to navigate

### 4. Consistency
Matches common Lua patterns and table conventions.

## Implementation Changes

### ScriptEngine.java
**Modified `extractMetadata()` method:**

**Before:** Iterated through array indices (1, 2, 3...)
```java
int i = 1;
while (true) {
    LuaValue paramTable = parametersTable.get(i);
    if (paramTable.isnil()) break;
    // Extract name from table field
    String name = paramTable.get("name").tojstring();
    i++;
}
```

**After:** Iterates through dictionary keys
```java
LuaValue key = LuaValue.NIL;
while (true) {
    Varargs entry = parametersTable.next(key);
    key = entry.arg1();
    if (key.isnil()) break;
    // Key IS the parameter name
    String paramName = key.tojstring();
}
```

**Modified `extractParameter()` signature:**
```java
// Before
private PatternParameter extractParameter(LuaValue paramTable)

// After  
private PatternParameter extractParameter(String paramName, LuaValue paramTable)
```
Parameter name is now passed in, not extracted from table.

### Example Patterns Updated
All example patterns converted to dictionary format:
- ✅ `configurable_bricks.lua`
- ✅ `spherical_dome.lua`
- ✅ `random_mix.lua`
- ✅ `type_demo.lua`

### Documentation Updated
- ✅ `README.md` - Updated metadata examples
- ✅ `PARAMETER_USAGE.md` - Updated all parameter examples

## Migration Guide

### For Pattern Authors

**Old Format:**
```lua
parameters = {
    {name = "size", type = "integer", default = 10, min = 1, max = 20},
    {name = "mode", type = "string", default = "normal"}
}
```

**New Format:**
```lua
parameters = {
    size = {type = "integer", default = 10, min = 1, max = 20},
    mode = {type = "string", default = "normal"}
}
```

**Migration steps:**
1. Remove the outer `{...}` around each parameter
2. Remove the `name = ` field
3. Use the parameter name as the key: `paramName = {...}`

## Example Conversions

### Integer Parameter
```lua
-- Before
{name = "gridSize", type = "integer", default = 4, min = 2, max = 10}

-- After
gridSize = {type = "integer", default = 4, min = 2, max = 10}
```

### Float Parameter
```lua
-- Before
{name = "radius", type = "float", default = 10.5, min = 1.0, max = 50.0}

-- After
radius = {type = "float", default = 10.5, min = 1.0, max = 50.0}
```

### Boolean Parameter
```lua
-- Before
{name = "enabled", type = "boolean", default = true}

-- After
enabled = {type = "boolean", default = true}
```

### String Parameter
```lua
-- Before
{name = "mode", type = "string", default = "uniform"}

-- After
mode = {type = "string", default = "uniform"}
```

## Complete Example

### Before
```lua
metadata = {
    name = "Configurable Brick Wall",
    author = "PatternWand",
    parameters = {
        {name = "brickWidth", type = "integer", default = 4, min = 2, max = 8},
        {name = "brickHeight", type = "integer", default = 2, min = 1, max = 4},
        {name = "weathered", type = "boolean", default = true},
        {name = "offsetPattern", type = "string", default = "alternating"}
    }
}
```

### After
```lua
metadata = {
    name = "Configurable Brick Wall",
    author = "PatternWand",
    parameters = {
        brickWidth = {type = "integer", default = 4, min = 2, max = 8},
        brickHeight = {type = "integer", default = 2, min = 1, max = 4},
        weathered = {type = "boolean", default = true},
        offsetPattern = {type = "string", default = "alternating"}
    }
}
```

## Backward Compatibility

⚠️ **Breaking Change:** Old list format is no longer supported.

All existing patterns using the old format must be updated to the new dictionary format.

The migration is straightforward and the new format is cleaner and more maintainable.

## Technical Details

### Lua Table Iteration
The implementation now uses Lua's `next()` function to iterate through table keys:

```java
LuaValue key = LuaValue.NIL;
while (true) {
    Varargs entry = parametersTable.next(key);
    key = entry.arg1();
    if (key.isnil()) break;
    
    String paramName = key.tojstring();
    LuaValue paramTable = entry.arg(2);
    
    PatternParameter param = extractParameter(paramName, paramTable);
}
```

This is the standard way to iterate through Lua dictionaries/tables.

### Parameter Order
**Note:** Dictionary order in Lua is not guaranteed. Parameters may be processed in any order.

This doesn't affect functionality since:
- Parameters are independent
- Command-line order doesn't matter
- Default values always applied first

If specific ordering is needed in the future, could add an `order` field to parameters.

## Files Modified

### Java Source
1. `ScriptEngine.java` - Changed parameter extraction logic

### Example Patterns
1. `configurable_bricks.lua`
2. `spherical_dome.lua`  
3. `random_mix.lua`
4. `type_demo.lua`

### Documentation
1. `README.md`
2. `PARAMETER_USAGE.md`

## Testing

✅ **Build:** Successful
✅ **Compilation:** No errors
✅ **Format:** spotlessApply passed

All example patterns updated and tested with new format.

## Advantages Summary

| Aspect | List Format | Dictionary Format |
|--------|-------------|-------------------|
| **Syntax** | `{name = "x", ...}` | `x = {...}` |
| **Redundancy** | Name repeated | Name only once |
| **Readability** | Moderate | High |
| **Lines of Code** | More | Fewer |
| **Lua Idiom** | Less common | Standard |
| **Editor Support** | Basic | Better |

## Recommendation

The dictionary format is:
- ✅ More idiomatic Lua
- ✅ Cleaner and easier to read
- ✅ Less redundant
- ✅ Better developer experience

This change improves the pattern authoring experience significantly while maintaining all functionality.
