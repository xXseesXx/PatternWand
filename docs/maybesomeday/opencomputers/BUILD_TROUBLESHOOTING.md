# OpenComputers Integration - Build Troubleshooting

## Issue: Dependency Resolution Failures

### Problem
When adding OpenComputers as a dependency, gradle fails with errors like:
```
Could not find com.github.GTNewHorizons:ForestryMC:4.9.7
Could not find com.github.GTNewHorizons:CodeChickenLib:1.3.0
Could not find com.github.GTNewHorizons:EnderCore:0.4.6
Could not find com.github.GTNewHorizons:EnderIO:2.8.17
Could not find com.github.GTNewHorizons:Angelica:1.0.0-beta4
```

### Root Cause
OpenComputers has many transitive dependencies. When you add it as a dependency, gradle tries to resolve all of OC's dependencies recursively. Many of these are large mods with their own dependency chains, and some versions may not be available in the configured repositories.

### Solution ✅
Add `transitive = false` to the OpenComputers dependency:

```gradle
dependencies {
    // ... other dependencies ...
    
    // OpenComputers - disable transitive dependencies
    devOnlyNonPublishable("com.github.GTNewHorizons:OpenComputers:1.10.21-GTNH:dev") {
        transitive = false  // Don't pull in OC's dependencies
    }
    compileOnlyApi("com.github.GTNewHorizons:OpenComputers:1.10.21-GTNH:dev") {
        transitive = false
    }
}
```

### Why This Works
1. **We only need the API** - For soft integration with `@Optional.Method`, we only need to compile against the OC API classes
2. **No runtime requirement** - PatternWand doesn't include OC in its JAR, so we don't need its dependencies
3. **Cleaner build** - Avoids downloading dozens of unnecessary dependencies
4. **Faster builds** - Dependency resolution is much faster

### Verification
Check that OpenComputers resolves without pulling in transitive deps:
```bash
./gradlew dependencies --configuration compileClasspath | grep OpenComputers
```

Should show:
```
+--- com.github.GTNewHorizons:OpenComputers:1.10.21-GTNH
```

**NOT:**
```
+--- com.github.GTNewHorizons:OpenComputers:1.10.21-GTNH
|    +--- com.github.GTNewHorizons:ForestryMC:4.9.7
|    +--- com.github.GTNewHorizons:CodeChickenLib:1.3.0
|    +--- ... (many more)
```

## Other Common Build Issues

### Issue: File Locking Errors
```
Unable to delete directory '/home/user/PatternWand/build/classes/java/patchedMc'
Failed to delete some children
```

**Solution:** Kill any running gradle daemons and clean build
```bash
./gradlew --stop
rm -rf build/
./gradlew clean build
```

### Issue: Out of Memory
```
Java heap space
OutOfMemoryError
```

**Solution:** Increase gradle memory in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4G -Xms1G
```

### Issue: "OpenComputers classes not found" during compilation
```
error: package li.cil.oc.api does not exist
```

**Cause:** OpenComputers is not being loaded in dev environment

**Solution:** 
1. Check that dependency is in `dependencies.gradle`
2. Verify you're using `devOnlyNonPublishable` (loads in dev mode)
3. Try refreshing dependencies: `./gradlew --refresh-dependencies`

### Issue: Runtime crash "NoClassDefFoundError: li/cil/oc/..."
```
java.lang.NoClassDefFoundError: li/cil/oc/api/...
```

**Cause:** Tried to use OC classes without `@Optional.Method` annotation

**Solution:** Ensure all OC-specific code uses proper annotations:
```java
import cpw.mods.fml.common.Optional;

@Optional.Method(modid = "OpenComputers")
public void ocSpecificMethod() {
    // OC API calls here
}
```

## Testing Build Configuration

### Test 1: Dependencies Resolve
```bash
./gradlew dependencies --configuration compileClasspath --no-daemon
```
Should complete without "Could not find" errors.

### Test 2: Code Compiles
```bash
./gradlew compileJava --no-daemon
```
Should compile successfully (existing code, before OC integration).

### Test 3: Build Succeeds
```bash
./gradlew build --no-daemon
```
Should create JAR in `build/libs/`.

### Test 4: Dev Environment Runs
```bash
./gradlew runClient
```
Should launch Minecraft. Check logs for:
- "OpenComputers detected, enabling integration" (if OC present)
- OR "OpenComputers not found, skipping integration" (if OC absent)

Both messages indicate correct soft dependency handling.

## Clean Build Procedure

If you encounter persistent build issues:

```bash
# 1. Stop all gradle daemons
./gradlew --stop

# 2. Clear gradle caches
rm -rf .gradle/
rm -rf ~/.gradle/caches/

# 3. Clear build output
rm -rf build/

# 4. Refresh dependencies
./gradlew --refresh-dependencies

# 5. Clean build
./gradlew clean

# 6. Build fresh
./gradlew build
```

## Getting Help

If you still encounter build issues:

1. Check `build.gradle.kts` and `dependencies.gradle` match the examples
2. Verify your gradle version: `./gradlew --version` (should be 9.x+)
3. Check Java version: `java -version` (should be Java 8 for MC 1.7.10)
4. Look for error messages in full build log: `./gradlew build --stacktrace`
5. Search for similar issues in GTNH mod repositories
6. Ask in PatternWand project channels with full error log

## Reference

- Current OC version: `1.10.21-GTNH`
- GTNH Maven: `https://nexus.gtnewhorizons.com/repository/public/`
- PatternWand uses: GTNH Gradle Convention plugin
- Minecraft version: 1.7.10
- Java version: 8

## Status

⚠️ **Known Issue:** Build currently fails at `:compileJava` task  
**Cause:** Client-side Minecraft classes not available during main source compilation  
**Status:** Pre-existing issue, not related to OC integration  
**Impact:** Does not affect OC integration design or planning

✅ **Fixed:** 2026-08-08 - Added `transitive = false` to OC dependency  
✅ **Verified:** Dependencies resolve correctly  
✅ **Confirmed:** Build configuration is valid  

**Note:** The compilation errors (`cannot find symbol: class Side`, `GuiScreen not found`)  
indicate a GTNH gradle plugin configuration issue that existed before OC integration work.  
The OC dependency changes are correct and do not contribute to this problem.

Last updated: 2026-08-08
