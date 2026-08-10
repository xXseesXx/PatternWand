# Pattern Visibility Feature Implementation

## Overview
Implemented a dual-sided config system to toggle visibility of default patterns, allowing both server-wide and per-player control. The system is designed to be easily expandable for future pattern categories.

## Architecture

### 1. Server-Side Config (Config.java)
- **Location**: `config/PatternWand.cfg`
- **Setting**: `showDefaultPatterns` (boolean, default: true)
- **Purpose**: Server admin can disable default patterns for all players
- **Expandable**: Easy to add more category toggles like `showExamplePatterns`, `showCommunityPatterns`, etc.

### 2. Client-Side Preferences (ClientPreferences.java)
- **Location**: `config/patternwand_client.cfg`
- **Setting**: `showDefaultPatterns` (boolean, default: true)
- **Purpose**: Individual players can hide default patterns regardless of server settings
- **Persistence**: Saved per-client, persists across sessions

### 3. Pattern Categorization (PatternScriptLoader.java)
- **Detection**: Patterns starting with `"default_"` prefix are classified as default patterns
- **Method**: `isDefaultPattern(String name)` - checks if pattern is default
- **Filtering**: `getScriptNames(boolean showDefaultPatterns)` - returns filtered list
- **Expandable**: Easy to add more categories with different prefixes or metadata

## Features

### Dual-Sided Filtering Logic
Patterns are shown only when **BOTH** conditions are true:
1. Server config allows them (`Config.showDefaultPatterns = true`)
2. Client preference allows them (`ClientPreferences.showDefaultPatterns = true`)

This means:
- Server can hide patterns for everyone
- Players can hide patterns just for themselves even if server allows them
- Players cannot force-show patterns the server has hidden

### User Interface Options

#### 1. Command-Line Toggle
```
/patternwand toggle defaults
```
- Toggles client-side preference
- Shows confirmation message
- Tab completion available

#### 2. GUI Toggle Button
- Located in top-right corner of pattern wand GUI
- Shows current state: "Defaults: ON" or "Defaults: OFF"
- Click to toggle instantly
- Persists preference to config file

#### 3. Pattern List Command
```
/patternwand list
```
- Automatically filters based on both server config and client preference
- Shows only patterns the player is allowed to see

## Implementation Details

### Files Modified

1. **Config.java**
   - Added `showDefaultPatterns` server config option
   - Expandable design for future categories

2. **ClientPreferences.java** (NEW)
   - Client-side preference storage
   - `@SideOnly(Side.CLIENT)` annotation
   - Toggle method for easy updating

3. **ClientProxy.java**
   - Initializes `ClientPreferences` during preInit
   - Creates `patternwand_client.cfg` file

4. **PatternScriptLoader.java**
   - Added pattern categorization methods
   - Filtering support in `getScriptNames()`

5. **PatternWandCommand.java**
   - Added `/patternwand toggle defaults` command
   - Updated tab completion
   - Dual-sided filtering in `handleList()`
   - Uses reflection to safely access client preferences

6. **GuiPatternWand.java**
   - Added toggle button in GUI
   - Updates button text when clicked
   - Persists changes via `ClientPreferences`

### Safety Measures

- **Reflection for Cross-Side Access**: Commands use reflection to safely access `ClientPreferences` from client side
- **Side Checking**: Toggle command validates it's running on client side
- **Graceful Fallbacks**: If client preferences unavailable, falls back to server config only

## Future Expansion

To add a new pattern category (e.g., "example" patterns):

1. **Add config option** in `Config.java`:
   ```java
   public static boolean showExamplePatterns = true;
   ```

2. **Add client preference** in `ClientPreferences.java`:
   ```java
   public static boolean showExamplePatterns = true;
   ```

3. **Add detection method** in `PatternScriptLoader.java`:
   ```java
   public boolean isExamplePattern(String name) {
       return name.startsWith("example_");
   }
   ```

4. **Update filtering logic** in `getScriptNames()`:
   ```java
   if (!showExamplePatterns) {
       // Filter out example patterns
   }
   ```

5. **Add toggle command** in `PatternWandCommand.java`:
   ```java
   case "examples":
       // Toggle example patterns
   ```

6. **Add GUI button** (optional)

## Testing Checklist

✅ Build successful (no compile errors)
⬜ Server config file generated with default values
⬜ Client config file generated when client starts
⬜ `/patternwand list` shows all patterns by default
⬜ Server config `showDefaultPatterns = false` hides defaults for all players
⬜ Client command `/pw toggle defaults` toggles visibility
⬜ GUI button toggles client preference
⬜ Client can hide defaults even if server allows them
⬜ Client cannot show defaults if server hides them
⬜ Settings persist across game restarts
⬜ Tab completion works for `/pw toggle`

## Usage Examples

### Server Admin: Disable defaults for everyone
Edit `config/PatternWand.cfg`:
```properties
showDefaultPatterns=false
```

### Player: Hide defaults just for yourself
Option 1 - Command:
```
/pw toggle defaults
```

Option 2 - GUI:
- Open pattern wand interface
- Click "Defaults: ON" button in top-right
- Button changes to "Defaults: OFF"

### Check visible patterns
```
/pw list
```
Only shows patterns you're allowed to see based on server + client settings.

## Notes

- Default patterns are those with `default_` prefix (all 25 example patterns)
- Pattern categorization is prefix-based, extensible to metadata-based in future
- Client preferences are stored locally, not synced to server
- System designed with future categories in mind (community, advanced, experimental, etc.)
