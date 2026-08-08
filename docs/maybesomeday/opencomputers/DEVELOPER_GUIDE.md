# OpenComputers Integration - Developer Guide

## Quick Start for Developers

This guide helps you get started implementing the OpenComputers integration for PatternWand.

## Prerequisites

- Java 8+ development environment
- Gradle knowledge
- Familiarity with Forge modding
- Basic understanding of OpenComputers API
- Git for version control

## Development Setup

### 1. Clone and Build
```bash
git clone https://github.com/YourUsername/PatternWand.git
cd PatternWand
./gradlew setupDecompWorkspace
./gradlew build
```

### 2. Import into IDE
```bash
# For IntelliJ IDEA
./gradlew idea

# For Eclipse
./gradlew eclipse
```

### 3. Run with OpenComputers
```bash
# Start Minecraft with mod in dev environment
./gradlew runClient
```

OpenComputers will be loaded from the dev dependency. PatternWand should load successfully whether OC is present or not.

## Project Structure

```
src/main/java/com/xXseesXx/patternwand/
├── PatternWandMod.java              # Main mod class
├── Config.java                       # Configuration
├── integration/                      # NEW: OC integration
│   ├── OCIntegration.java           # Entry point, soft dependency handler
│   └── oc/                           # OC-specific code
│       ├── ComponentPatternManager.java
│       ├── ComponentPatternWandController.java
│       ├── PreviewGenerator.java
│       ├── FileAccessManager.java
│       └── ColorMapper.java
├── patterns/
│   └── scripted/
│       ├── ScriptEngine.java        # Existing: Lua execution
│       ├── PatternScriptLoader.java # Existing: Pattern loading
│       └── ...
└── items/
    └── ItemPatternWandUnbreakable.java  # Existing: Wand implementation
```

## Implementation Workflow

### Phase 1: Foundation (Start Here)

#### Step 1.1: Create OCIntegration Entry Point

```java
// src/main/java/com/xXseesXx/patternwand/integration/OCIntegration.java
package com.xXseesXx.patternwand.integration;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import com.xXseesXx.patternwand.PatternWandMod;

public class OCIntegration {
    
    private static boolean enabled = false;
    
    /**
     * Initialize OC integration if the mod is loaded.
     * Call this from PatternWandMod.init()
     */
    public static void init() {
        if (Loader.isModLoaded("OpenComputers")) {
            PatternWandMod.LOG.info("OpenComputers detected, enabling integration");
            initOC();
        } else {
            PatternWandMod.LOG.info("OpenComputers not found, skipping integration");
        }
    }
    
    @Optional.Method(modid = "OpenComputers")
    private static void initOC() {
        try {
            // Register components here
            OCComponents.register();
            enabled = true;
            PatternWandMod.LOG.info("OpenComputers integration enabled");
        } catch (Exception e) {
            PatternWandMod.LOG.error("Failed to initialize OC integration", e);
        }
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
}
```

#### Step 1.2: Call from Main Mod

```java
// In PatternWandMod.java
@Mod.EventHandler
public void init(FMLInitializationEvent event) {
    proxy.init(event);
    
    // Initialize OC integration
    OCIntegration.init();
}
```

#### Step 1.3: Create Component Base Class

```java
// src/main/java/com/xXseesXx/patternwand/integration/oc/ComponentBase.java
package com.xXseesXx.patternwand.integration.oc;

import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.ManagedEnvironment;

public abstract class ComponentBase extends ManagedEnvironment {
    
    protected ComponentBase() {
        setNode(Network.newNode(this, Visibility.Network)
            .withComponent(getComponentName())
            .withConnector()
            .create());
    }
    
    /**
     * Component name for OC (e.g., "patternwand_manager")
     */
    public abstract String getComponentName();
    
    /**
     * Helper to consume energy and throw if insufficient
     */
    protected boolean consumeEnergy(double amount, boolean simulate) {
        if (node() instanceof Connector) {
            Connector connector = (Connector) node();
            if (connector.tryChangeBuffer(-amount)) {
                return true;
            }
            if (!simulate) {
                throw new RuntimeException("Insufficient energy");
            }
            return false;
        }
        return true; // No energy system available
    }
}
```

### Phase 2: Pattern Management Component

#### Step 2.1: Implement PatternManager Component

```java
// src/main/java/com/xXseesXx/patternwand/integration/oc/ComponentPatternManager.java
package com.xXseesXx.patternwand.integration.oc;

import li.cil.oc.api.machine.*;
import li.cil.oc.api.network.*;
import com.xXseesXx.patternwand.patterns.scripted.*;
import java.util.*;

@li.cil.oc.api.API.NetworkAPI.Network.FullyConnected
public class ComponentPatternManager extends ComponentBase {
    
    @Override
    public String getComponentName() {
        return "patternwand_manager";
    }
    
    /**
     * List all available patterns
     * @return Table of pattern info
     */
    @Callback(doc = "function():table -- Lists all available patterns")
    @Optional.Method(modid = "OpenComputers")
    public Object[] listPatterns(Context context, Arguments args) {
        consumeEnergy(50, false);
        
        Map<String, CompiledScript> patterns = PatternScriptLoader.getLoadedPatterns();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<String, CompiledScript> entry : patterns.entrySet()) {
            Map<String, Object> info = new HashMap<>();
            info.put("name", entry.getKey());
            
            PatternMetadata metadata = entry.getValue().metadata;
            if (metadata != null) {
                info.put("displayName", metadata.name);
                info.put("author", metadata.author);
                // Add more metadata fields...
            }
            
            result.add(info);
        }
        
        return new Object[] { result };
    }
    
    /**
     * Reload patterns from disk
     * @return success boolean
     */
    @Callback(doc = "function():boolean -- Reloads all patterns from disk")
    @Optional.Method(modid = "OpenComputers")
    public Object[] reload(Context context, Arguments args) {
        consumeEnergy(500, false);
        
        try {
            PatternScriptLoader.reloadPatterns();
            return new Object[] { true };
        } catch (Exception e) {
            return new Object[] { false, e.getMessage() };
        }
    }
    
    // Add more methods: getPatternInfo, validatePattern, etc.
}
```

#### Step 2.2: Register Component

```java
// src/main/java/com/xXseesXx/patternwand/integration/oc/OCComponents.java
package com.xXseesXx.patternwand.integration.oc;

import li.cil.oc.api.Driver;
import cpw.mods.fml.common.Optional;

public class OCComponents {
    
    @Optional.Method(modid = "OpenComputers")
    public static void register() {
        // Register as environment provider (can be accessed from any computer)
        Driver.add(new ComponentPatternManager());
        
        // More components will be registered here
    }
}
```

## Key APIs to Use

### OpenComputers API

```java
// Component annotation for callable methods
@Callback(doc = "function(...):... -- Description")

// Energy system
Connector connector = (Connector) node();
connector.tryChangeBuffer(-energyCost);

// Returning values to Lua
return new Object[] { result };      // Single value
return new Object[] { value1, value2 }; // Multiple values
return new Object[] { table };       // Lua table (Map or List)
```

### PatternWand APIs

```java
// Pattern loading
PatternScriptLoader.getLoadedPatterns()
PatternScriptLoader.reloadPatterns()

// Pattern compilation
ScriptEngine engine = new ScriptEngine();
CompiledScript script = engine.compile(source, name);

// Pattern execution (for preview)
int paletteIndex = engine.executePattern(script, x, y, z, ...);

// Wand NBT access
NBTTagCompound wandNBT = itemStack.getTagCompound();
String activePattern = wandNBT.getString("activePattern");
```

## Testing Checklist

### Unit Tests
- [ ] Component registration works
- [ ] listPatterns() returns correct data
- [ ] Energy costs are calculated correctly
- [ ] File access is properly sandboxed

### Integration Tests (In-Game)
```lua
-- In OpenComputers terminal
local component = require("component")

-- Check component is registered
print(component.patternwand_manager.type)  -- Should print "patternwand_manager"

-- Test listing patterns
local patterns = component.patternwand_manager.listPatterns()
for _, p in ipairs(patterns) do
    print(p.name)
end

-- Test reload
local ok = component.patternwand_manager.reload()
print("Reload: " .. tostring(ok))
```

### Soft Dependency Test
1. Build mod without OC in classpath
2. Run in Minecraft without OC installed
3. Verify no crashes
4. Check log for "OpenComputers not found" message

## Common Issues & Solutions

### Issue: NoClassDefFoundError for OC classes
**Solution:** Use `@Optional.Method(modid = "OpenComputers")` on all methods that reference OC classes

### Issue: Component not appearing in component.list()
**Solution:** Check component name, ensure Driver.add() is called, verify node creation

### Issue: "Method not found" error from Lua
**Solution:** Ensure method has `@Callback` annotation and correct signature

### Issue: Energy errors in creative mode
**Solution:** Check if node is Connector, handle null case

## Performance Tips

1. **Cache compiled patterns** - Don't recompile on every call
2. **Lazy evaluation** - Only generate preview data when requested
3. **Chunk preview data** - For large previews, generate in chunks
4. **Use byte arrays** - More memory efficient than object arrays
5. **Pool resources** - Reuse ScriptEngine instances when possible

## Code Style

- Follow existing PatternWand code style
- Use descriptive variable names
- Add JavaDoc comments to public APIs
- Include `@Optional.Method` on all OC references
- Handle errors gracefully (return error messages, don't crash)

## Git Workflow

```bash
# Create feature branch
git checkout -b feature/oc-integration

# Make changes
git add src/main/java/com/xXseesXx/patternwand/integration/
git commit -m "feat: Add OC component registration framework"

# Push and create PR
git push origin feature/oc-integration
```

## Documentation Requirements

For each implemented component:
1. JavaDoc on class and methods
2. Entry in `docs/opencomputers/API_REFERENCE.md`
3. Example program in `docs/opencomputers/examples/`
4. Update `OPENCOMPUTERS_TASKS.md` checklist

## Resources

- [OpenComputers API Docs](https://ocdoc.cil.li/)
- [OC GitHub Examples](https://github.com/MightyPirates/OpenComputers/tree/master-MC1.7.10/src/main/java/li/cil/oc/api)
- [Forge @Optional Documentation](https://mcforge.readthedocs.io/en/latest/gettingstarted/structuring/)
- PatternWand Design Docs: `docs/OPENCOMPUTERS_INTEGRATION.md`

## Getting Help

- Check existing PatternWand code for patterns
- Review `docs/OPENCOMPUTERS_TASKS.md` for detailed task breakdown
- Look at OC integration in other mods (Computronics, etc.)
- Ask in project Discord/IRC channel

## Next Steps

1. ✅ Read this guide
2. ✅ Set up development environment
3. 📋 Implement Phase 1 (Foundation)
4. 📋 Test soft dependency handling
5. 📋 Implement Phase 2 (Pattern Management)
6. 📋 Create example Lua programs
7. 📋 Write documentation
8. 📋 Submit PR for review

## Quick Reference: File Locations

| Purpose | Path |
|---------|------|
| Integration entry | `integration/OCIntegration.java` |
| Components | `integration/oc/Component*.java` |
| Tests | `test/.../integration/` |
| Docs | `docs/opencomputers/` |
| Examples | `docs/opencomputers/examples/` |

Good luck with implementation! 🚀
