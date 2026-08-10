# Pattern Visibility Feature - Server-Side Implementation

## Overview
Simple server-side config to toggle visibility of default example patterns. Server admin controls whether default patterns show up for all players.

## How It Works

### Server Config
**Location**: `config/PatternWand.cfg`

**Setting**:
```properties
# Show default example patterns (patterns with 'default_' prefix)
# Set to false to hide them server-wide
showDefaultPatterns=true
```

### Pattern Detection
- Patterns starting with `default_` are classified as "default patterns"
- All 25 included example patterns use this prefix
- Easy to add more categories in the future (e.g., `example_`, `advanced_`, etc.)

### Filtering
The `/patternwand list` command automatically filters patterns based on the config:
- `showDefaultPatterns=true` (default): Shows all patterns including defaults
- `showDefaultPatterns=false`: Hides all patterns with `default_` prefix

## Configuration GUI

Access via Minecraft's Mod Options menu:
1. Main Menu → Mods → PatternWand → Config
2. Toggle `showDefaultPatterns` setting
3. Changes take effect immediately (may need `/pw reload`)

## Usage

### Hide Default Patterns
Edit `config/PatternWand.cfg`:
```properties
showDefaultPatterns=false
```

Or use the in-game config GUI.

### Verify Filtering
```
/pw list
```

Shows only the patterns allowed by current config.

## Technical Details

### Files Modified

1. **Config.java**
   - Added `showDefaultPatterns` boolean option
   - Expandable for future categories

2. **PatternScriptLoader.java**
   - `isDefaultPattern(String name)` - Checks if pattern has `default_` prefix
   - `getScriptNames(boolean showDefaultPatterns)` - Returns filtered list

3. **PatternWandCommand.java**
   - `handleList()` uses `Config.showDefaultPatterns` to filter
   - Shows only allowed patterns

4. **PatternWandGuiFactory.java** (NEW)
   - Forge config GUI integration
   - Accessible from Mod Options menu

5. **PatternWandMod.java**
   - Added `guiFactory` to @Mod annotation
   - Stores config file reference for GUI

### Pattern Categories (Future Expansion)

To add a new category:

1. Add config option in `Config.java`:
```java
public static boolean showExamplePatterns = true;
```

2. Add detection in `PatternScriptLoader.java`:
```java
public boolean isExamplePattern(String name) {
    return name.startsWith("example_");
}
```

3. Update filtering logic:
```java
public String[] getScriptNames(boolean showDefaults, boolean showExamples) {
    return scriptCache.keySet()
        .stream()
        .filter(name -> (showDefaults || !isDefaultPattern(name)))
        .filter(name -> (showExamples || !isExamplePattern(name)))
        .toArray(String[]::new);
}
```

## Testing Checklist

✅ Build successful
⬜ Config file generated with default values
⬜ Config GUI accessible from Mod Options
⬜ `showDefaultPatterns=true` shows all patterns
⬜ `showDefaultPatterns=false` hides default patterns
⬜ `/pw list` respects the config setting
⬜ Changes persist after restart

## Notes

- Server-side only (no client preferences needed)
- Simple and maintainable
- Easy to expand for more categories
- Config changes require `/pw reload` to take effect
