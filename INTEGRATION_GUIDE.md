# Pattern Scripting Integration Guide

## Quick Start

The scripting engine is ready to use. Here's how to integrate it with your code.

## Basic Usage

### 1. Initialize the Loader

```java
import com.patternwand.patterns.scripted.*;
import java.io.File;

// In your mod initialization (preInit or init)
File patternsDir = new File("config/patternwand/patterns");
PatternScriptLoader scriptLoader = new PatternScriptLoader(patternsDir);
scriptLoader.loadAllPatterns();

// Log how many patterns were loaded
PatternWandMod.LOG.info("Loaded {} pattern scripts", scriptLoader.getScriptCount());
```

### 2. Execute a Pattern Script

```java
// Get a compiled script
CompiledScript script = scriptLoader.getScript("checkerboard.lua");

if (script != null) {
    try {
        // Execute for a specific block position
        int paletteIndex = scriptLoader.getEngine().executePattern(
            script,
            x, y, z,           // World coordinates
            relX, relY, relZ,  // Relative to pattern origin
            paletteInventory,  // IInventory with 27 slots
            seed               // Pattern seed (use world seed or player-chosen)
        );
        
        if (paletteIndex >= 0) {
            // Place block from palette slot 'paletteIndex'
            ItemStack stack = paletteInventory.getStackInSlot(paletteIndex);
            if (stack != null) {
                // Place this block in the world
            }
        } else {
            // paletteIndex == -1 means skip this position (gap)
        }
        
    } catch (ScriptExecutionException e) {
        PatternWandMod.LOG.error("Script execution error: {}", e.getMessage());
        // Show error to player
    }
}
```

## Integration Patterns

### Pattern A: Generate Blocks in Area

Use when you want to generate a pattern in a defined area:

```java
public void generatePattern(World world, int startX, int startY, int startZ, 
                           int width, int height, int depth,
                           CompiledScript script, IInventory palette, long seed) {
    
    ScriptEngine engine = scriptLoader.getEngine();
    
    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                try {
                    int paletteIndex = engine.executePattern(
                        script,
                        startX + x, startY + y, startZ + z,  // World coords
                        x, y, z,                              // Relative coords
                        palette,
                        seed
                    );
                    
                    if (paletteIndex >= 0) {
                        placeBlockFromPalette(world, startX + x, startY + y, startZ + z, 
                                            palette, paletteIndex);
                    }
                    
                } catch (ScriptExecutionException e) {
                    // Log error but continue with other blocks
                    PatternWandMod.LOG.warn("Script error at {},{},{}: {}", 
                        x, y, z, e.getMessage());
                }
            }
        }
    }
}

private void placeBlockFromPalette(World world, int x, int y, int z, 
                                  IInventory palette, int index) {
    ItemStack stack = palette.getStackInSlot(index);
    if (stack != null && stack.getItem() instanceof ItemBlock) {
        Block block = Block.getBlockFromItem(stack.getItem());
        int meta = stack.getItemDamage();
        world.setBlock(x, y, z, block, meta, 3);
    }
}
```

### Pattern B: Pattern Wand Integration

Example of how to integrate with a wand that places blocks:

```java
public class ScriptedPatternWand {
    
    private final PatternScriptLoader scriptLoader;
    private CompiledScript activeScript;
    private long patternSeed;
    
    public ScriptedPatternWand(PatternScriptLoader loader) {
        this.scriptLoader = loader;
        this.patternSeed = System.currentTimeMillis();
    }
    
    /**
     * Set the active pattern by name
     */
    public void setPattern(String scriptName) {
        this.activeScript = scriptLoader.getScript(scriptName);
        if (activeScript == null) {
            PatternWandMod.LOG.warn("Pattern script not found: {}", scriptName);
        }
    }
    
    /**
     * Use wand on a block - places pattern
     */
    public boolean useWand(World world, EntityPlayer player, int clickX, int clickY, int clickZ,
                          int radius, IInventory palette) {
        
        if (activeScript == null) {
            return false;
        }
        
        // Generate pattern in radius around clicked position
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    
                    int worldX = clickX + x;
                    int worldY = clickY + y;
                    int worldZ = clickZ + z;
                    
                    try {
                        int paletteIndex = scriptLoader.getEngine().executePattern(
                            activeScript,
                            worldX, worldY, worldZ,
                            x, y, z,  // Relative to clicked position
                            palette,
                            patternSeed
                        );
                        
                        if (paletteIndex >= 0) {
                            placeBlock(world, worldX, worldY, worldZ, palette, paletteIndex);
                        }
                        
                    } catch (ScriptExecutionException e) {
                        // Show error to player once, then abort
                        player.addChatMessage(new ChatComponentText(
                            "Pattern error: " + e.getMessage()
                        ));
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    private void placeBlock(World world, int x, int y, int z, IInventory palette, int index) {
        ItemStack stack = palette.getStackInSlot(index);
        if (stack != null && stack.getItem() instanceof ItemBlock) {
            Block block = Block.getBlockFromItem(stack.getItem());
            int meta = stack.getItemDamage();
            
            // Only place if position is replaceable
            if (world.getBlock(x, y, z).isReplaceable(world, x, y, z)) {
                world.setBlock(x, y, z, block, meta, 3);
            }
        }
    }
}
```

## Pattern Selection GUI

Example GUI handler for selecting patterns:

```java
public class PatternSelectionGui extends GuiScreen {
    
    private final PatternScriptLoader scriptLoader;
    private final ScriptedPatternWand wand;
    
    public PatternSelectionGui(PatternScriptLoader loader, ScriptedPatternWand wand) {
        this.scriptLoader = loader;
        this.wand = wand;
    }
    
    @Override
    public void initGui() {
        String[] patterns = scriptLoader.getScriptNames();
        
        int y = 20;
        for (String patternName : patterns) {
            // Create button for each pattern
            buttonList.add(new GuiButton(
                buttonList.size(),
                width / 2 - 100,
                y,
                200, 20,
                patternName.replace(".lua", "")
            ));
            y += 22;
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) {
        String[] patterns = scriptLoader.getScriptNames();
        if (button.id < patterns.length) {
            wand.setPattern(patterns[button.id]);
            mc.thePlayer.addChatMessage(new ChatComponentText(
                "Selected pattern: " + patterns[button.id]
            ));
            mc.displayGuiScreen(null);
        }
    }
}
```

## Error Handling Best Practices

### 1. Catch Compilation Errors at Startup

```java
public void loadPatterns() {
    try {
        scriptLoader.loadAllPatterns();
    } catch (Exception e) {
        PatternWandMod.LOG.error("Failed to load patterns", e);
        // Don't crash - just disable scripted patterns
    }
}
```

### 2. Handle Execution Errors Gracefully

```java
try {
    int result = engine.executePattern(script, ...);
    // Use result
    
} catch (ScriptExecutionException e) {
    if (e.getMessage().contains("timeout")) {
        // Script took too long
        player.addChatMessage(new ChatComponentText(
            "§cPattern script is too complex and timed out"
        ));
    } else {
        // Other runtime error
        player.addChatMessage(new ChatComponentText(
            "§cPattern error: " + e.getMessage()
        ));
    }
}
```

### 3. Validate Palette Before Execution

```java
public boolean canExecutePattern(IInventory palette) {
    // Check if palette has at least one block
    for (int i = 0; i < palette.getSizeInventory(); i++) {
        if (palette.getStackInSlot(i) != null) {
            return true;
        }
    }
    return false;
}
```

## Performance Considerations

### 1. Cache the ScriptEngine

Don't create a new engine for each execution:

```java
// GOOD
private final ScriptEngine engine = new ScriptEngine();

// BAD
ScriptEngine engine = new ScriptEngine(); // Every time
```

### 2. Limit Pattern Area

Large patterns can be slow:

```java
public static final int MAX_PATTERN_SIZE = 32;  // 32x32x32 max

if (width * height * depth > MAX_PATTERN_SIZE * MAX_PATTERN_SIZE * MAX_PATTERN_SIZE) {
    player.addChatMessage(new ChatComponentText("Pattern area too large!"));
    return false;
}
```

### 3. Execute on Server Thread

Pattern execution should happen on the server thread, not client:

```java
if (!world.isRemote) {
    // Execute pattern here
}
```

## Configuration

Store pattern settings in wand NBT:

```java
public static CompiledScript getActivePattern(ItemStack wand, PatternScriptLoader loader) {
    NBTTagCompound tag = wand.getTagCompound();
    if (tag != null && tag.hasKey("activePattern")) {
        String scriptName = tag.getString("activePattern");
        return loader.getScript(scriptName);
    }
    return null;
}

public static void setActivePattern(ItemStack wand, String scriptName) {
    NBTTagCompound tag = wand.getTagCompound();
    if (tag == null) {
        tag = new NBTTagCompound();
        wand.setTagCompound(tag);
    }
    tag.setString("activePattern", scriptName);
}
```

## Debugging

Enable debug logging:

```java
// In your config
public static boolean debugScripts = false;

// When executing
if (Config.debugScripts) {
    PatternWandMod.LOG.info("Executing {} at world({},{},{}) rel({},{},{})", 
        script.name, x, y, z, relX, relY, relZ);
}
```

## Testing Without Full Integration

You can test the engine standalone:

```java
@Test
public void testScriptEngine() throws Exception {
    // Create test inventory
    IInventory testPalette = new InventoryBasic("test", false, 27);
    testPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone));
    testPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone));
    
    // Compile simple script
    ScriptEngine engine = new ScriptEngine();
    String script = 
        "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" +
        "    return (relX + relZ) % 2\n" +
        "end\n" +
        "return pattern";
    
    CompiledScript compiled = engine.compile(script, "test.lua");
    
    // Test execution
    int result1 = engine.executePattern(compiled, 0,0,0, 0,0,0, testPalette, 0);
    int result2 = engine.executePattern(compiled, 0,0,0, 1,0,0, testPalette, 0);
    int result3 = engine.executePattern(compiled, 0,0,0, 0,0,1, testPalette, 0);
    
    // Verify checkerboard pattern
    assertEquals(0, result1);
    assertEquals(1, result2);
    assertEquals(1, result3);
}
```

## Next Steps

1. Choose an integration pattern (A or B above)
2. Initialize PatternScriptLoader in your mod's init phase
3. Load patterns from the patterns directory
4. Execute patterns when placing blocks
5. Handle errors gracefully
6. Test with the example patterns

The scripting engine is ready - you just need to wire it into your block placement logic!
