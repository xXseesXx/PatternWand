# Modpack Compatibility Fix

## Problem

PatternWand worked in standalone but crashed in modpacks with `ClassNotFoundException` for LuaJ classes.

## Root Cause

The mod used `implementation("org.luaj:luaj-jse:3.0.1")` which only declares LuaJ as a runtime dependency but doesn't bundle it into the JAR. In standalone development, the dependency is available on the classpath, but in a modpack environment, the library wasn't being loaded correctly.

## Solution

Changed LuaJ from a regular dependency to a **shadowed dependency** which bundles and relocates it inside the mod JAR:

### Changes Made

1. **build.gradle.kts**: Changed `implementation` to `shadowImplementation`
```kotlin
dependencies {
    // Lua scripting engine for pattern scripts - shadowed into JAR for modpack compatibility
    shadowImplementation("org.luaj:luaj-jse:3.0.1")
    
    // JUnit for testing
    testImplementation("junit:junit:4.13.2")
}
```

2. **gradle.properties**: Enabled shadowing feature
```properties
usesShadowedDependencies = true
```

### Results

- JAR size increased from ~124KB to ~401KB (includes LuaJ)
- LuaJ classes are relocated to `com.xXseesXx.patternwand.shadow.org.luaj.*` to prevent conflicts
- All bytecode references are automatically updated during build
- Source code imports remain unchanged (`import org.luaj.*`)

### Verification

You can verify LuaJ is properly bundled:
```bash
unzip -l build/libs/patternwand-*.jar | grep luaj | head -10
```

You should see relocated classes like:
```
com/xXseesXx/patternwand/shadow/org/luaj/vm2/Globals.class
com/xXseesXx/patternwand/shadow/org/luaj/vm2/LuaValue.class
...
```

## Testing

The mod should now work in both:
- ✅ Standalone development environment
- ✅ Full modpack installations

No changes to your Lua pattern scripts are needed.
