# Resource Pack Loading Removed

## Security Concern

Resource pack pattern loading has been **completely removed** due to security concerns:
- Resource packs can be loaded from untrusted sources
- Lua patterns are executable code that run on the player's machine
- While sandboxed, allowing arbitrary code from resource packs is a potential attack vector

## What Was Removed

All code related to resource pack loading has been removed:

1. **PatternScriptLoader.java**
   - Removed `IResourceManagerReloadListener` interface
   - Removed `onResourceManagerReload()` method
   - Removed `loadPatternsFromResources()` method
   - Removed `getResourceManager()` and `getClientResourceManager()` methods
   - Removed `loadPatternFromResource()` method
   - Removed resource manager imports (IResourceManager, IResourceManagerReloadListener, etc.)
   - Added simple `loadPatternsFromModAssets()` that loads only from jar

2. **ClientProxy.java**
   - Removed resource reload listener registration

3. **PatternWandCommand.java**
   - Removed F3+T tip from reload command

4. **Test Resource Pack**
   - Removed `run/client/resourcepacks/PatternWandTest/`

5. **Documentation**
   - Removed RESOURCE_PACK_PATTERNS.md
   - Removed RESOURCE_PACK_SUMMARY.md
   - Removed RELOAD_FIX.md
   - Removed PATTERN_DIRECTORY_FIX.md

## What Still Works

✅ **Built-in patterns from mod jar** - 13 example patterns ship with the mod
✅ **Filesystem patterns** - Load from `config/patternwand/patterns/` (any subdirectories)
✅ **Pattern reload** - `/patternwand reload` reloads all patterns
✅ **Recursive directory support** - Organize patterns in subdirectories
✅ **Filesystem patterns override built-ins** - If you create a pattern with the same name as a built-in, yours takes priority

## Pattern Loading Order

1. **Filesystem patterns** (config/patternwand/patterns/) - Highest priority
2. **Built-in patterns** (mod jar assets) - Only if not overridden by filesystem

## Implementation

Built-in patterns are now loaded using simple classpath resource loading:
```java
InputStream stream = getClass().getResourceAsStream(
    "/assets/patternwand/patterns/examples/" + patternFile);
```

This only loads patterns that are **compiled into the mod jar**, not from external resource packs.

## Benefits

✅ **Security** - No arbitrary code execution from resource packs
✅ **Simplicity** - Simpler codebase, fewer dependencies
✅ **Reliability** - No complex resource manager interactions
✅ **Works everywhere** - Client, server, and test environments

## For Users

**How to add custom patterns:**
1. Place `.lua` files in `config/patternwand/patterns/`
2. Organize in subdirectories if desired (e.g., `patterns/terrain/`, `patterns/decorative/`)
3. Run `/patternwand reload` or `/pw reload` to reload
4. Patterns are available immediately

**Overriding built-in patterns:**
- Create a pattern with the same filename as a built-in (e.g., `checkerboard.lua`)
- Place it in `config/patternwand/patterns/`
- Your version will be used instead of the built-in

## Build Status

✅ All tests pass (155/155)
✅ Build successful
✅ No resource pack dependencies remaining
