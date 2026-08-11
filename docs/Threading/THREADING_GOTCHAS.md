# Threading Implementation: Gotchas & Edge Cases

## Critical Edge Cases

### 1. Player Disconnects During Async Execution

**Problem:** Background thread generates plan for 5,000 blocks, but player logs out before callback.

**Symptoms:**
- `NullPointerException` when trying to access player in callback
- Memory leak if plan isn't garbage collected
- Wasted CPU time finishing plan for disconnected player

**Solution:**
```java
MinecraftServer.getServer().addScheduledTask(() -> {
    // Re-fetch player - returns null if disconnected
    EntityPlayer player = MinecraftServer.getServer()
        .getConfigurationManager()
        .func_152612_a(playerName);
    
    if (player == null) {
        LOG.debug("Player {} disconnected, aborting placement", playerName);
        return; // Silently abort
    }
    
    // Proceed with placement
});
```

**Additional Safeguards:**
- Track active executions in `ConcurrentHashMap<UUID, Future<?>>`
- Cancel futures on player disconnect event
- Add timeout (30s) to prevent runaway background tasks

---

### 2. Player Inventory Changes During Async Planning

**Problem:** Plan validates materials in background, but player moves/drops items before callback.

**Scenario:**
1. Player has 1,000 stone in inventory
2. Background thread validates: "1,000 stone available ✓"
3. Player drops 900 stone while planning
4. Callback tries to consume 1,000 stone → fails after 100 blocks

**Solution 1 (Recommended):** Re-validate on main thread before consumption
```java
// In main thread callback:
if (!revalidateMaterials(plan, playerShim)) {
    player.addChatMessage("§cInventory changed during planning!");
    return;
}
executePlan(plan);
```

**Solution 2:** Lock inventory during planning
```java
// Mark wand as "busy" in NBT
itemStack.getTagCompound().setBoolean("asyncPlanningActive", true);

// In inventory event handler:
if (wandItem.getTagCompound().getBoolean("asyncPlanningActive")) {
    event.setCanceled(true); // Prevent inventory changes
}
```

**Recommendation:** Use Solution 1 - less intrusive, fails gracefully.

---

### 3. World Unloads During Async Execution

**Problem:** Chunk containing placement area unloads while planning.

**Symptoms:**
- `World.setBlock()` fails silently or throws exception
- Partial placement with missing blocks
- Possible chunk corruption if world is mid-save

**Solution:**
```java
// In main thread callback:
for (PlacementEntry entry : plan.getPlacements()) {
    // Check if chunk is loaded before placing
    if (!world.blockExists(entry.position.x, entry.position.y, entry.position.z)) {
        LOG.debug("Chunk unloaded at {}, skipping block", entry.position);
        continue;
    }
    
    placeBlock(entry);
}
```

**Prevention:**
- Only allow wand use in loaded chunks
- Keep player near placement area (world unloads chunks far from players)

---

### 4. Multiple Concurrent Wand Uses

**Problem:** Player rapidly right-clicks wand, starting multiple async executions.

**Scenario:**
1. Click 1: Starts 5,000 block plan (takes 20s)
2. Click 2: Starts another 5,000 block plan
3. Both complete around same time → inventory consumed twice?

**Solution 1:** Block wand use during active planning
```java
// In onItemUse():
if (itemStack.getTagCompound().getBoolean("asyncPlanningActive")) {
    player.addChatMessage("§cWand is already working on a pattern!");
    return false; // Don't start new execution
}
```

**Solution 2:** Cancel previous execution on new use
```java
// In AsyncPlacementExecutor:
private static final Map<String, Future<?>> activeExecutionsByPlayer = new ConcurrentHashMap<>();

public static void executeAsync(PlanContext context, PlacementCallback callback) {
    String playerName = context.playerName;
    
    // Cancel previous execution for this player
    Future<?> previous = activeExecutionsByPlayer.get(playerName);
    if (previous != null && !previous.isDone()) {
        previous.cancel(true);
        LOG.debug("Canceled previous execution for {}", playerName);
    }
    
    Future<?> future = planExecutor.submit(() -> {
        // ... plan generation
    });
    
    activeExecutionsByPlayer.put(playerName, future);
}
```

**Recommendation:** Use Solution 1 for safety. Solution 2 for better UX.

---

### 5. Lua Script Modifies Global State

**Problem:** Lua pattern uses global variables that interfere between concurrent executions.

**Scenario:**
```lua
-- Bad pattern script
counter = 0  -- Global variable!

function pattern(x, y, z, ...)
    counter = counter + 1
    if counter % 2 == 0 then
        return 0
    else
        return 1
    end
end
```

If two players use this pattern simultaneously, `counter` is shared → unpredictable results.

**Current Status:** ✅ Already handled! Each `ScriptEngine` has its own `Globals` instance.

**Verification:**
```java
// In ScriptEngine.java:
public ScriptEngine() {
    this.globals = JsePlatform.standardGlobals(); // New instance per engine
}
```

**But:** If `ScriptEngine` is shared across threads, need separate instance per execution.

**Solution:** Use thread-local or per-execution `Globals`:
```java
public int executePattern(...) {
    // Create fresh Globals for this execution
    Globals executionGlobals = JsePlatform.standardGlobals();
    // ... setup and execute in isolation
}
```

---

### 6. Pattern Execution Timeout in Background Thread

**Problem:** Lua pattern takes >10s per block, triggering timeout.

**Current Implementation:**
```java
Future<Integer> future = executor.submit(task);
Integer result = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
```

**Gotcha:** This timeout is **per block**, not for entire plan!

**Scenario:**
- 1,000 blocks × 10s timeout = 10,000s max (2.7 hours!)
- Background thread could run indefinitely

**Solution:** Add **total plan timeout** in addition to per-block timeout:
```java
public static void executeAsync(PlanContext context, PlacementCallback callback) {
    Future<?> future = planExecutor.submit(() -> {
        long startTime = System.currentTimeMillis();
        
        for (Point3d pos : context.blocks) {
            // Check total elapsed time
            if (System.currentTimeMillis() - startTime > Config.asyncPlanTimeoutMs) {
                throw new TimeoutException("Plan generation exceeded " + 
                    Config.asyncPlanTimeoutMs + "ms");
            }
            
            // Execute pattern for this block (has its own per-block timeout)
            int paletteIndex = executePattern(...);
        }
    });
}
```

---

### 7. Memory Pressure from Large PlacementPlans

**Problem:** 10,000 block plan with complex metadata uses significant memory.

**Calculation:**
```
PlacementEntry size:
- Point3d: 3 × 4 bytes (int x, y, z) = 12 bytes
- Block reference: 8 bytes
- Metadata int: 4 bytes
= ~24 bytes per entry

10,000 blocks = 240 KB
100,000 blocks = 2.4 MB
```

Not terrible, but can add up with multiple concurrent executions.

**Solution 1:** Limit maximum plan size
```java
// In Config.java
public static int maxAsyncPlanSize = 10000;

// In generatePlan():
if (blocks.size() > Config.maxAsyncPlanSize) {
    throw new IllegalArgumentException(
        "Pattern too large for async execution (max " + 
        Config.maxAsyncPlanSize + " blocks)"
    );
}
```

**Solution 2:** Stream processing (more complex)
```java
// Don't build entire plan in memory
// Instead, use Iterator pattern and place blocks incrementally
```

**Recommendation:** Start with Solution 1. Solution 2 only if memory becomes issue.

---

### 8. Server Shutdown During Async Execution

**Problem:** Server shutting down, but background threads still running.

**Scenario:**
1. Player uses wand for massive pattern
2. Admin types `/stop`
3. Server tries to save world
4. Background thread still modifying data structures

**Solution:** Proper shutdown hook
```java
// In PatternWandMod.java or similar:
@EventHandler
public void serverStopping(FMLServerStoppingEvent event) {
    LOG.info("Canceling all async pattern executions...");
    AsyncPlacementExecutor.shutdown();
}

// In AsyncPlacementExecutor:
public static void shutdown() {
    LOG.info("Shutting down async executor, canceling {} active tasks", 
        activeExecutions.size());
    
    // Cancel all active executions
    for (Future<?> future : activeExecutions.values()) {
        future.cancel(true);
    }
    activeExecutions.clear();
    
    // Shutdown executor gracefully
    planExecutor.shutdown();
    try {
        if (!planExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            planExecutor.shutdownNow();
        }
    } catch (InterruptedException e) {
        planExecutor.shutdownNow();
    }
}
```

---

### 9. Mod Compatibility: World Access in "Read-Only" Phase

**Problem:** Some mods might modify world state during "read-only" operations.

**Example:**
```java
// Seems innocent:
Block block = world.getBlock(pos);

// But some mods override getBlock() to:
// - Generate ores procedurally (lazy generation)
// - Trigger chunk generation
// - Update caches
```

**Solution:** Avoid world access in background thread entirely.

**Safe in Background:**
- ✅ Pure Lua computation
- ✅ Reading from passed context (immutable)
- ✅ Pattern logic and math
- ✅ Building data structures

**Unsafe in Background:**
- ❌ `world.getBlock()` or similar
- ❌ Chunk queries
- ❌ Entity queries
- ❌ Biome queries

**Current Code Review:**
```java
// In generatePlan() - currently SAFE
// No world access, only uses passed blocks list and context
PlacementPlan plan = new PlacementPlan();
for (Point3d pos : blocks) {
    int paletteIndex = engine.executePattern(...);
    plan.addPlacement(pos, block, meta); // Gets block from palette, not world
}
```

✅ Current implementation is already safe!

---

### 10. Deadlock: Main Thread Waits for Callback

**Problem:** Incorrect implementation could cause deadlock.

**Bad Example:**
```java
// DON'T DO THIS!
Future<PlacementPlan> future = planExecutor.submit(() -> generatePlan());

// Main thread blocks waiting for background
PlacementPlan plan = future.get(); // DEADLOCK if background needs main thread!
executePlan(plan);
```

**Why Deadlock?**
- Main thread waiting for background to finish
- Background needs main thread for something (e.g., world query)
- Both threads waiting on each other → deadlock

**Solution:** Use callback pattern, never block main thread
```java
// CORRECT:
planExecutor.submit(() -> {
    PlacementPlan plan = generatePlan();
    
    // Don't block, schedule callback instead
    MinecraftServer.getServer().addScheduledTask(() -> {
        executePlan(plan);
    });
});

// Main thread returns immediately, no blocking
```

---

## Race Condition Checklist

### Data Structures

- [ ] `PlacementPlan` - **Immutable after construction** ✅
- [ ] `PatternPalette` - Read-only in background ✅
- [ ] `PlacementContext` - Immutable ✅
- [ ] `PatternMetadata` - Read-only ✅
- [ ] `CompiledScript` - Shared read-only ✅
- [ ] `Globals` (Lua) - **Per-execution or synchronized?** ⚠️

### Shared State

- [ ] `activeExecutions` - **Use `ConcurrentHashMap`** ✅
- [ ] `planExecutor` - **Thread-safe `ExecutorService`** ✅
- [ ] `PatternScriptLoader.scripts` - Concurrent read? ⚠️
- [ ] Player inventory - **Re-validate on main thread** ✅
- [ ] World state - **No access from background** ✅

### Timing Issues

- [ ] Player disconnects - **Check before callback** ✅
- [ ] World unloads - **Check chunk loaded** ✅
- [ ] Server shutdown - **Cancel all tasks** ✅
- [ ] Wand dropped - **Validate wand still held?** ⚠️

---

## Testing Scenarios

### Stress Tests

1. **Rapid Clicking**
   - Click wand 10 times rapidly
   - Expected: Only first completes, or previous cancelled
   - Fail: Multiple placements, inventory over-consumed

2. **Large Pattern During Logout**
   - Start 10,000 block pattern
   - Log out immediately
   - Expected: Background task cancelled, no placement
   - Fail: Placement happens after disconnect, crash

3. **Inventory Manipulation**
   - Start 5,000 block pattern
   - Drop all materials while planning
   - Expected: Re-validation fails, error message
   - Fail: Partial placement, items consumed anyway

4. **Multiple Players Simultaneously**
   - 10 players use wands at once
   - Expected: All patterns execute correctly, no interference
   - Fail: Patterns mixed up, Lua state corruption

5. **Server Shutdown During Planning**
   - Start large pattern
   - `/stop` immediately
   - Expected: Clean shutdown, task cancelled
   - Fail: Hang during shutdown, data corruption

### Edge Case Tests

6. **Empty Pattern** (all blocks return nil)
7. **Single Block Pattern** (async overhead worth it?)
8. **Pattern Error During Async** (Lua throws error)
9. **Materials Added During Planning** (player crafts more)
10. **Chunk Unload During Placement** (not planning)

---

## Performance Considerations

### Thread Pool Sizing

**Current:** `Executors.newCachedThreadPool()`
- Creates threads on demand
- Reuses idle threads
- No upper limit

**Pros:**
- Scales with load
- Good for variable workload

**Cons:**
- Could create many threads if abused
- Memory overhead per thread (~1MB stack)

**Alternative:** Fixed thread pool
```java
private static final ExecutorService executor = 
    Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors(), 
        threadFactory
    );
```

**Recommendation:** 
- Start with `newCachedThreadPool()`
- If abuse becomes issue, switch to fixed size
- Add config option: `Config.asyncPlanningThreads`

### CPU Usage

**Background Lua execution competes with:**
- World tick
- Entity updates
- Chunk generation
- Other mods

**Mitigation:**
```java
Thread t = new Thread(r, "PatternWand-AsyncPlanner");
t.setDaemon(true);
t.setPriority(Thread.MIN_PRIORITY); // Lower priority than main thread
return t;
```

---

## Monitoring & Debugging

### Add Metrics

```java
public class AsyncPlacementMetrics {
    public static AtomicInteger activePlannings = new AtomicInteger(0);
    public static AtomicInteger completedPlannings = new AtomicInteger(0);
    public static AtomicInteger failedPlannings = new AtomicInteger(0);
    public static AtomicLong totalPlanningTimeMs = new AtomicLong(0);
    
    public static void recordPlanning(long durationMs, boolean success) {
        if (success) {
            completedPlannings.incrementAndGet();
        } else {
            failedPlannings.incrementAndGet();
        }
        totalPlanningTimeMs.addAndGet(durationMs);
    }
}
```

### Debug Command

```
/patternwand stats async
- Active plannings: 2
- Completed: 150
- Failed: 3
- Avg time: 4.2s
- Thread pool: 4 threads
```

---

## Rollback Strategy

If async causes issues, provide easy rollback:

```java
// In Config.java
public static boolean enableAsyncPlanning = true;

// In PatternWandWorker
if (Config.enableAsyncPlanning && pattern != null) {
    placeBlocksWithPatternAsync(...); // New code
} else {
    placeBlocksWithPattern(...); // Original synchronous code
}
```

Users can disable via config if they experience issues.

---

## Summary: Pre-Flight Checklist

Before implementing async execution:

- [ ] Add shutdown hook for clean server stop
- [ ] Implement player disconnect validation
- [ ] Add re-validation before placement
- [ ] Add max plan size limit
- [ ] Add total timeout (not just per-block)
- [ ] Use concurrent data structures
- [ ] Never block main thread on Future.get()
- [ ] Test with multiple concurrent users
- [ ] Test during server shutdown
- [ ] Add config toggle for easy rollback
- [ ] Add metrics/monitoring
- [ ] Document thread safety in code comments

If any of these are skipped, risk of production issues increases significantly!
