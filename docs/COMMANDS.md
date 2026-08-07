# Pattern Wand Commands

## Overview

The Pattern Wand includes commands for managing and testing pattern scripts without needing full in-game integration.

## Commands

### `/patternwand list`

Lists all loaded pattern scripts.

**Usage:**
```
/patternwand list
```

**Output:**
```
Loaded patterns (5):
  - checkerboard
  - noise_terrain
  - bricks
  - ripples
  - gradient
```

---

### `/patternwand reload`

Reloads all pattern scripts from disk. Useful after editing or adding new .lua files.

**Usage:**
```
/patternwand reload
```

**Output:**
```
Reloaded 5 pattern scripts
```

**When to use:**
- After editing a pattern script
- After adding new pattern scripts
- After fixing compilation errors

---

### `/patternwand set <pattern>`

Sets the active pattern on the currently held Pattern Wand.

**Usage:**
```
/patternwand set checkerboard
```

**Requirements:**
- Must be holding a Pattern Wand in your hand
- Pattern must exist (use `/patternwand list` to see available patterns)

**Output:**
```
Set active pattern to: checkerboard
```

**Tab Completion:**
Type `/patternwand set ` and press TAB to see available patterns.

---

### `/patternwand info`

Shows information about the currently held Pattern Wand.

**Usage:**
```
/patternwand info
```

**Requirements:**
- Must be holding a Pattern Wand

**Output:**
```
Active pattern: checkerboard
```

Or if no pattern is set:
```
No active pattern set
Use /patternwand set <pattern> to set one
```

---

## Workflow

### Testing a Pattern Script

1. **Create or edit a pattern script** in `config/patternwand/patterns/`
   ```lua
   -- mypattern.lua
   function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)
       return (relX + relZ) % 2
   end
   return pattern
   ```

2. **Reload patterns**
   ```
   /patternwand reload
   ```

3. **Verify it loaded**
   ```
   /patternwand list
   ```
   Should show `mypattern` in the list.

4. **Hold a Pattern Wand**

5. **Set your pattern**
   ```
   /patternwand set mypattern
   ```

6. **Verify it's active**
   ```
   /patternwand info
   ```

7. **Use the wand** (when integration is complete)

---

## Pattern Storage Location

Patterns are stored in:
```
config/patternwand/patterns/
```

The mod will create this directory automatically on first run.

### Example Files
```
config/patternwand/patterns/
├── README.md
├── examples/
│   ├── checkerboard.lua
│   ├── noise_terrain.lua
│   ├── bricks.lua
│   ├── ripples.lua
│   └── gradient.lua
└── mypattern.lua  (your custom patterns)
```

**Note:** The example patterns are in `examples/` subdirectory and won't be loaded by default. Copy them to the main `patterns/` directory to use them.

---

## Troubleshooting

### Pattern doesn't appear in list

**Check:**
1. Is the file in `config/patternwand/patterns/` (not in `examples/`)?
2. Does the filename end with `.lua`?
3. Does the script have syntax errors? Check logs.

**Fix:**
```
/patternwand reload
```

### Pattern compilation error

**Symptoms:**
```
Failed to reload patterns: ...
```

**Check the logs** for detailed error messages:
- Look in `logs/latest.log`
- Search for "Failed to load pattern script"

**Common issues:**
- Missing `return pattern` at end of script
- Syntax errors in Lua code
- Invalid function signature

### Pattern set but wand doesn't work

**This is expected** - full wand integration is not yet complete. The commands allow you to:
- Verify patterns load correctly
- Check for compilation errors
- Prepare patterns for when integration is finished

---

## NBT Storage

The active pattern is stored in the wand's NBT data:

```java
// Stored in itemstack NBT
{
    "activePattern": "checkerboard.lua"
}
```

This persists across game restarts.

---

## Examples

### Quick test workflow

```bash
# Check what patterns are available
/patternwand list

# Set checkerboard pattern
/patternwand set checkerboard

# Verify it's set
/patternwand info

# Edit the pattern file (in config/patternwand/patterns/)
# Then reload
/patternwand reload

# Check if it compiled successfully
/patternwand list
```

### After adding new patterns

```bash
# Add mypattern.lua to config/patternwand/patterns/

# Reload to pick it up
/patternwand reload

# Check if it loaded
/patternwand list

# Set it as active
/patternwand set mypattern
```

---

## Permission Level

All commands require **no special permissions** (level 0). Any player can use them.

---

## Tab Completion

Commands support tab completion:

- `/patternwand <TAB>` → Shows: `reload`, `list`, `set`, `info`
- `/patternwand set <TAB>` → Shows all available pattern names

---

## Future Integration

Once wand integration is complete, the workflow will be:

1. `/patternwand set mypattern` (select pattern)
2. Configure palette in GUI (Shift+Right-click wand)
3. Use wand to place blocks (Right-click)
4. Pattern script determines which palette slots to use

The commands are **ready now** and will work with the full integration when it's complete.
