# Lua Async Execution - Focused Implementation Plan

**Goal:** Offload Lua pattern execution to background threads while keeping block placement on main thread.

**Scope:** This plan focuses **exclusively** on making Lua execution async. Block placement (which is already fast) remains unchanged.

**Status:** Ready for implementation  
**Estimated Effort:** 4-6 hours  
**Risk Level:** Low (Lua execution is naturally thread-safe)

---

## Executive Summary

Your testing confirms:
- ✅ **Lua execution is the only bottleneck**
- ✅ **Block placement is already fast** (BBW proven performance)
- ✅ **Lua execution is the easiest part to make async** (naturally isolated)

This plan implements:
1. Async plan generation (Lua runs in background)
2. Sync placement on main thread (unchanged from BBW)
3. OpenComputers-inspired improvements (execution budgets, proper Globals isolation)

**Key insight from OpenComputers analysis:**
> "Your `ScriptEngine` is already optimized conceptually. Don't integrate OC's Machine runtime - steal the good ideas instead."

---

## Current Architecture Analysis

### What's Already Good ✅

```java
// Current ScriptEngine
public int executePattern(...) {
    // 1. Create API wrappers (palette, noise, util, debug)
    // 2. Create Lua tables
    // 3. Call Lua function with timeout
    // 4. Return palette index
}
```

**Strengths:**
- Clean separation of Lua from world access
- Already has timeout protection (10s)
- Sandboxed environment (removed dangerous libraries)
- Stateless execution model (each call independent)

**What makes it thread-safe:**
- Lua `Globals` is per-instance
- No world state access in Lua
- No shared mutable state between executions
- API wrappers are pure data (palette inventory is read-only)

### What Needs Improvement ⚠️

Based on OpenComputers comparison:

**Issue 1: API wrapper creation overhead**
```java
// Current: Created EVERY BLOCK
NoiseAPI noise = new NoiseAPI(seed);
PaletteAPI palette = new PaletteAPI(paletteInventory, seed);
UtilAPI util = new UtilAPI();
DebugAPI debug = new DebugAPI();

LuaTable luaNoise = LuaNoiseWrapper.wrap(noise);
LuaTable luaPalette = LuaPaletteWrapper.wrap(palette);
LuaTable luaUtil = LuaUtilWrapper.wrap(util);
LuaTable luaDebug = LuaDebugWrapper.wrap(debug);
```

**Impact:** For 1000 blocks = 8000 object allocations

**Issue 2: Globals sharing between executions**
```java
private final Globals globals; // Shared across all executePattern() calls
```

While LuaJ is thread-safe for reads, concurrent writes to `math.randomseed()` will interfere.

**Issue 3: Timeout is per-block, not total**
```java
Future<Integer> future = executor.submit(task);
Integer result = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
```
1000 blocks × 10s = potential 10,000s total (2.7 hours!)

---

## Implementation Plan

### Phase 1: Optimize ScriptEngine (2 hours)

**Goal:** Make Lua execution as fast as possible before async.

#### Step 1.1: Reuse API Wrappers

**Problem:** Creating 8 objects per block is wasteful.

**Solution:** Create once, reuse for entire pattern.

```java
/**
 * Execution context - created once per pattern, reused for all blocks
 */
public class PatternExecutionContext {
    public final NoiseAPI noise;
    public final PaletteAPI palette;
    public final UtilAPI util;
    public final DebugAPI debug;
    
    // Lua wrappers (created once)
    public final LuaTable luaNoise;
    public final LuaTable luaPalette;
    public final LuaTable luaUtil;
    public final LuaTable luaDebug;
    
    // Parameters table (created once)
    public final LuaTable luaParams;
    
    // Context table (created once)
    public final LuaTable luaContext;
    
    // Seed
    public final long seed;
    
    public PatternExecutionContext(IInventory paletteInventory, long seed, 
                                    Map<String, Object> parameterValues,
                                    PlacementContext context) {
        this.seed = seed;
        
        // Create APIs once
        this.noise = new NoiseAPI(seed);
        this.palette = new PaletteAPI(paletteInventory, seed);
        this.util = new UtilAPI();
        this.debug = new DebugAPI();
        
        // Wrap once
        this.luaNoise = LuaNoiseWrapper.wrap(noise);
        this.luaPalette = LuaPaletteWrapper.wrap(palette);
        this.luaUtil = LuaUtilWrapper.wrap(util);
        this.luaDebug = LuaDebugWrapper.wrap(debug);
        
        // Create params table once
        this.luaParams = createParamsTable(parameterValues);
        
        // Create context table once
        this.luaContext = (context != null) ? LuaContextWrapper.wrap(context) : new LuaTable();
    }
    
    private LuaTable createParamsTable(Map<String, Object> parameterValues) {
        LuaTable table = new LuaTable();
        if (parameterValues != null) {
            for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Integer) {
                    table.set(entry.getKey(), LuaValue.valueOf((Integer) value));
                } else if (value instanceof Number) {
                    table.set(entry.getKey(), LuaValue.valueOf(((Number) value).doubleValue()));
                } else if (value instanceof Boolean) {
                    table.set(entry.getKey(), LuaValue.valueOf((Boolean) value));
                } else if (value instanceof String) {
                    table.set(entry.getKey(), LuaValue.valueOf((String) value));
                }
            }
        }
        return table;
    }
}
```

**Modified executePattern:**
```java
public int executePattern(CompiledScript script, int x, int y, int z, 
                         int relX, int relY, int relZ,
                         PatternExecutionContext context) 
    throws ScriptExecutionException {
    
    long startTimeNs = DebugAPI.isDebugEnabled() ? System.nanoTime() : 0;
    
    // Call pattern function with pre-created wrappers
    LuaValue result = script.function.invoke(
        new LuaValue[] {
            LuaValue.valueOf(x), LuaValue.valueOf(y), LuaValue.valueOf(z),
            LuaValue.valueOf(relX), LuaValue.valueOf(relY), LuaValue.valueOf(relZ),
            context.luaPalette, context.luaNoise, context.luaUtil,
            LuaValue.valueOf(context.seed),
            context.luaParams, context.luaContext, context.luaDebug
        }
    ).arg1();
    
    // Handle return value (same as before)
    if (result.isnil()) {
        return -1;
    } else if (result.isnumber()) {
        int index = result.toint();
        if (index < 0 || index >= 54) {
            throw new ScriptExecutionException(
                script.name,
                "Pattern returned invalid palette index: " + index);
        }
        return index;
    } else {
        throw new ScriptExecutionException(
            script.name,
            "Pattern must return a number or nil. Got: " + result.typename());
    }
}
```

**Benefits:**
- 1000 blocks: 8000 allocations → 12 allocations (99.8% reduction)
- Reduced GC pressure
- Faster execution (no object creation per block)

---

#### Step 1.2: Isolate Globals Per Execution

**Problem:** Shared `Globals` means concurrent executions interfere.

**Solution:** Create fresh `Globals` per pattern execution (not per block, per pattern).

```java
public class ScriptEngine {
    // Remove instance-level globals
    // private final Globals globals; // OLD
    
    // Keep only for compilation
    private Globals createCompilationGlobals() {
        Globals g = JsePlatform.standardGlobals();
        sandboxGlobals(g);
        return g;
    }
    
    /**
     * Apply sandbox restrictions to Globals
     */
    private void sandboxGlobals(Globals g) {
        g.set("os", LuaValue.NIL);
        g.set("io", LuaValue.NIL);
        g.set("package", LuaValue.NIL);
        g.set("dofile", LuaValue.NIL);
        g.set("loadfile", LuaValue.NIL);
        g.set("require", LuaValue.NIL);
        g.set("luajava", LuaValue.NIL);
    }
    
    public CompiledScript compile(String source, String name) throws ScriptCompileException {
        Globals compilationGlobals = createCompilationGlobals();
        
        try {
            LuaValue chunk = compilationGlobals.load(source, name);
            LuaValue result = chunk.call();
            
            // Extract pattern function
            LuaValue patternFunction;
            if (result.isfunction()) {
                patternFunction = result;
            } else {
                patternFunction = compilationGlobals.get("pattern");
                if (!patternFunction.isfunction()) {
                    throw new ScriptCompileException(name, "No pattern function found");
                }
            }
            
            // Extract metadata
            LuaValue metadataTable = compilationGlobals.get("metadata");
            PatternMetadata metadata = null;
            if (metadataTable.istable()) {
                metadata = extractMetadata(metadataTable);
            }
            
            return new CompiledScript(name, patternFunction, metadata);
            
        } catch (LuaError e) {
            throw new ScriptCompileException(name, "Compilation failed: " + e.getMessage(), e);
        }
    }
}
```

**Key insight:** 
- Compilation uses temporary `Globals`
- Execution doesn't need `Globals` at all (function is already compiled)
- Each async execution is completely isolated

**Benefits:**
- No `math.randomseed()` interference
- True isolation between concurrent executions
- Matches OpenComputers' per-execution architecture

---

#### Step 1.3: Add Total Timeout Budget

**Problem:** Per-block timeout can accumulate to hours.

**Solution:** Add total execution budget (OpenComputers-inspired).

```java
/**
 * Execution budget - tracks total time across all blocks
 */
public class ExecutionBudget {
    private final long startTimeMs;
    private final long totalTimeoutMs;
    private final long perBlockTimeoutMs;
    private int blocksExecuted = 0;
    
    public ExecutionBudget(long totalTimeoutSeconds, long perBlockTimeoutSeconds) {
        this.startTimeMs = System.currentTimeMillis();
        this.totalTimeoutMs = totalTimeoutSeconds * 1000;
        this.perBlockTimeoutMs = perBlockTimeoutSeconds * 1000;
    }
    
    /**
     * Check if we still have budget for another block
     */
    public void checkBudget() throws TimeoutException {
        long elapsed = System.currentTimeMillis() - startTimeMs;
        if (elapsed > totalTimeoutMs) {
            throw new TimeoutException(
                "Pattern execution exceeded total budget of " + 
                (totalTimeoutMs / 1000) + "s after " + blocksExecuted + " blocks"
            );
        }
    }
    
    public long getPerBlockTimeoutMs() {
        return perBlockTimeoutMs;
    }
    
    public void recordBlock() {
        blocksExecuted++;
    }
    
    public long getElapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }
}
```

**Usage in plan generation:**
```java
public PlacementPlan generatePlan(CompiledScript script, List<Point3d> blocks, 
                                  PatternExecutionContext context) {
    ExecutionBudget budget = new ExecutionBudget(60, 5); // 60s total, 5s per block
    PlacementPlan plan = new PlacementPlan();
    
    for (Point3d pos : blocks) {
        // Check total budget FIRST
        budget.checkBudget();
        
        // Execute with per-block timeout
        int index = executePatternWithTimeout(
            script, pos, context, 
            budget.getPerBlockTimeoutMs()
        );
        
        if (index >= 0) {
            plan.addEntry(pos, index);
        }
        
        budget.recordBlock();
    }
    
    return plan;
}
```

**Benefits:**
- Prevents runaway patterns
- Clear failure messages ("timeout after 100 blocks")
- Configurable budgets

---

### Phase 2: Make Plan Generation Async (2-3 hours)

**Goal:** Run Lua execution in background, placement on main thread.

#### Step 2.1: Create AsyncPlanGenerator

```java
package com.xXseesXx.patternwand.execution;

import java.util.List;
import java.util.concurrent.*;

/**
 * Generates placement plans asynchronously using background threads.
 * 
 * Lua execution happens in background, block placement on main thread.
 */
public class AsyncPlanGenerator {
    
    private static final int CORE_THREADS = 2;
    private static final int MAX_THREADS = 4;
    
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        CORE_THREADS,
        MAX_THREADS,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "PatternWand-Async-" + counter.incrementAndGet());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1); // Slightly lower priority
                return t;
            }
        }
    );
    
    /**
     * Generate plan asynchronously
     */
    public static Future<PlanResult> generatePlanAsync(
            CompiledScript script,
            List<Point3d> blocks,
            PatternExecutionContext context,
            String playerName) {
        
        return executor.submit(() -> {
            try {
                ExecutionBudget budget = new ExecutionBudget(
                    Config.asyncPlanTimeoutSeconds, 
                    Config.perBlockTimeoutSeconds
                );
                
                PlacementPlan plan = new PlacementPlan();
                
                for (Point3d pos : blocks) {
                    // Check if cancelled
                    if (Thread.currentThread().isInterrupted()) {
                        return PlanResult.cancelled();
                    }
                    
                    // Check total budget
                    budget.checkBudget();
                    
                    // Execute Lua (thread-safe)
                    int index = script.function.invoke(
                        LuaValue.valueOf((int)pos.x),
                        LuaValue.valueOf((int)pos.y),
                        LuaValue.valueOf((int)pos.z),
                        // ... other args from context
                    ).arg1().toint();
                    
                    if (index >= 0 && index < 54) {
                        plan.addEntry(pos, index);
                    }
                    
                    budget.recordBlock();
                }
                
                return PlanResult.success(plan);
                
            } catch (TimeoutException e) {
                return PlanResult.error("Pattern timeout: " + e.getMessage());
            } catch (Exception e) {
                return PlanResult.error("Pattern error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Shutdown executor (call on server stop)
     */
    public static void shutdown() {
        LOG.info("Shutting down AsyncPlanGenerator...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                LOG.warn("Forcing shutdown of AsyncPlanGenerator");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

---

#### Step 2.2: Create PlanResult Wrapper

```java
/**
 * Result of async plan generation
 */
public class PlanResult {
    public enum Status {
        SUCCESS,
        ERROR,
        CANCELLED
    }
    
    public final Status status;
    public final PlacementPlan plan;
    public final String errorMessage;
    
    private PlanResult(Status status, PlacementPlan plan, String errorMessage) {
        this.status = status;
        this.plan = plan;
        this.errorMessage = errorMessage;
    }
    
    public static PlanResult success(PlacementPlan plan) {
        return new PlanResult(Status.SUCCESS, plan, null);
    }
    
    public static PlanResult error(String message) {
        return new PlanResult(Status.ERROR, null, message);
    }
    
    public static PlanResult cancelled() {
        return new PlanResult(Status.CANCELLED, null, "Execution cancelled");
    }
    
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    public boolean hasError() {
        return status == Status.ERROR;
    }
}
```

---

#### Step 2.3: Integrate into PatternWandWorker

```java
// In PatternWandWorker.java

public ArrayList<Point3d> placeBlocks(ItemStack itemStack, LinkedList<Point3d> blocks,
                                      Point3d clickedPos, ItemStack sourceItems,
                                      IPlayerShim playerShim, int side,
                                      float hitX, float hitY, float hitZ) {
    
    String activePattern = getActivePattern(itemStack);
    
    if (activePattern != null && !activePattern.isEmpty()) {
        // Check if async enabled and pattern large enough
        if (Config.enableAsyncPlanning && blocks.size() >= Config.asyncThresholdBlocks) {
            // Use async execution
            placeBlocksWithPatternAsync(
                itemStack, blocks, clickedPos, sourceItems,
                playerShim, side, hitX, hitY, hitZ, activePattern
            );
            
            // Return empty - placement happens in callback
            return new ArrayList<>();
            
        } else {
            // Use synchronous execution (small patterns)
            return placeBlocksWithPattern(
                itemStack, blocks, clickedPos, sourceItems,
                playerShim, side, hitX, hitY, hitZ, activePattern
            );
        }
    }
    
    // No pattern - use default
    return super.placeBlocks(itemStack, blocks, clickedPos, sourceItems,
                            playerShim, side, hitX, hitY, hitZ);
}

private void placeBlocksWithPatternAsync(
        ItemStack itemStack, LinkedList<Point3d> blocks,
        Point3d clickedPos, ItemStack sourceItems,
        IPlayerShim playerShim, int side,
        float hitX, float hitY, float hitZ,
        String patternName) {
    
    // Get compiled script
    CompiledScript script = PatternScriptLoader.getCompiledScript(patternName);
    if (script == null) {
        playerShim.getPlayer().addChatMessage(
            new ChatComponentText("§cPattern not found: " + patternName)
        );
        return;
    }
    
    // Create execution context (once for entire pattern)
    PatternExecutionContext context = new PatternExecutionContext(
        getWandInventory(itemStack),
        getPatternSeed(itemStack),
        getParameterValues(itemStack, script),
        createPlacementContext(clickedPos, blocks, playerShim, side)
    );
    
    // Start async generation
    Future<PlanResult> future = AsyncPlanGenerator.generatePlanAsync(
        script, blocks, context, playerShim.getPlayer().getCommandSenderName()
    );
    
    // Schedule callback on main thread
    scheduleCallback(future, itemStack, clickedPos, playerShim, side, hitX, hitY, hitZ);
}

private void scheduleCallback(Future<PlanResult> future, ItemStack itemStack,
                             Point3d clickedPos, IPlayerShim playerShim,
                             int side, float hitX, float hitY, float hitZ) {
    
    MinecraftServer.getServer().addScheduledTask(new Runnable() {
        public void run() {
            // Check if future is done
            if (!future.isDone()) {
                // Not ready yet, check again next tick
                MinecraftServer.getServer().addScheduledTask(this);
                return;
            }
            
            try {
                PlanResult result = future.get();
                
                // Validate player still exists
                EntityPlayer player = MinecraftServer.getServer()
                    .getConfigurationManager()
                    .func_152612_a(playerShim.getPlayer().getCommandSenderName());
                
                if (player == null) {
                    LOG.debug("Player disconnected, aborting placement");
                    return;
                }
                
                if (result.isSuccess()) {
                    // Execute placement on main thread
                    executePlacementPlan(result.plan, itemStack, clickedPos,
                                        playerShim, side, hitX, hitY, hitZ);
                    
                    player.addChatMessage(new ChatComponentText(
                        String.format("§aPlaced %d blocks", result.plan.size())
                    ));
                    
                } else if (result.hasError()) {
                    player.addChatMessage(new ChatComponentText(
                        "§cPattern error: " + result.errorMessage
                    ));
                }
                
            } catch (Exception e) {
                LOG.error("Error in async callback", e);
            }
        }
    });
}
```

---

### Phase 3: Configuration (30 minutes)

Add to `Config.java`:

```java
// Async execution
public static boolean enableAsyncPlanning = true;
public static int asyncThresholdBlocks = 100; // Patterns < 100 blocks stay sync
public static int asyncPlanTimeoutSeconds = 60;
public static int perBlockTimeoutSeconds = 5;
public static int asyncCoreThreads = 2;
public static int asyncMaxThreads = 4;
```

---

## Testing Plan

### Unit Tests

```java
@Test
public void testExecutionContextReuse() {
    // Verify API wrappers created once
    PatternExecutionContext ctx = new PatternExecutionContext(...);
    
    // Execute 1000 times
    for (int i = 0; i < 1000; i++) {
        engine.executePattern(script, i, 0, 0, i, 0, 0, ctx);
    }
    
    // Verify same objects used
    assertSame(ctx.luaPalette, ctx.luaPalette);
}

@Test
public void testGlobalsIsolation() {
    // Two concurrent executions with different seeds
    // Verify random sequences don't interfere
}

@Test
public void testTotalTimeout() {
    // Pattern that takes 1s per block
    // Total timeout 5s
    // Should fail at ~5 blocks
}
```

### Integration Tests

```java
@Test
public void testAsyncPlanGeneration() {
    // Start async plan
    Future<PlanResult> future = AsyncPlanGenerator.generatePlanAsync(...);
    
    // Verify completes
    PlanResult result = future.get(10, TimeUnit.SECONDS);
    assertTrue(result.isSuccess());
}

@Test
public void testPlayerDisconnectDuringAsync() {
    // Start async plan
    // Simulate player disconnect
    // Verify callback handles gracefully
}
```

---

## Performance Expectations

### Before Optimization

```
1000 blocks:
- API wrapper creation: 50ms
- Lua execution: 2000ms
- Placement: 100ms
Total: 2150ms (freezes game)
```

### After Phase 1 (Optimizations)

```
1000 blocks:
- API wrapper creation: 0.5ms (99% reduction)
- Lua execution: 2000ms (same)
- Placement: 100ms
Total: 2100ms (2% faster, but still freezes)
```

### After Phase 2 (Async)

```
1000 blocks:
- API wrapper creation: 0.5ms (background)
- Lua execution: 2000ms (background)
- Placement: 100ms (main thread)
Total perceived: 100ms (95% improvement!)
```

---

## Rollout Plan

1. **Week 1:** Implement Phase 1 optimizations, test performance gains
2. **Week 2:** Implement Phase 2 async, test on single-player
3. **Week 3:** Deploy to test server, gather metrics
4. **Week 4:** Production release with config toggle

---

## Success Criteria

✅ API wrapper allocations reduced by 99%  
✅ Lua execution moved to background thread  
✅ Player can move during pattern generation  
✅ Block placement speed unchanged (still fast)  
✅ No crashes after 48 hours testing  
✅ Config toggle allows disabling async  

---

## Risk Assessment

**Risk Level: LOW**

**Why low risk:**
- Lua execution already isolated (no world access)
- Block placement unchanged (proven BBW code)
- Only optimizing what's already working
- Small, incremental changes
- Config toggle for rollback

**Potential issues:**
- API wrapper reuse might cause issues if not truly stateless → Verify in testing
- Globals isolation might break some patterns → Test with all example patterns
- Async callback timing → Already handled by BBW architecture

---

**End of Implementation Plan**

Ready to start with Phase 1 - optimize before async!
