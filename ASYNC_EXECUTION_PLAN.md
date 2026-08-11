# PatternWand Async Lua Execution - Implementation Plan

**Date:** August 11, 2026  
**Status:** Planning Phase  
**Goal:** Move Lua pattern execution off the Minecraft main thread while maintaining thread safety for world operations

## Executive Summary

This plan implements asynchronous Lua execution for PatternWand to prevent server lag during large pattern generation. The architecture maintains a strict boundary: Lua computation happens in background threads, while all world/block operations remain on the main thread.

**Key Principle:** Each milestone must be tested and working before proceeding to the next.

---

## Architecture Overview

```
MAIN THREAD                    BACKGROUND THREAD
───────────────────────────   ────────────────────────
Player uses wand              
  ↓                           
Snapshot immutable inputs     
  ↓                           
Submit async job ────────────→ Execute Lua pattern
  ↓                              ↓
Return immediately               Calculate blocks
  ↓                              ↓
Continue gameplay                Build PlacementPlan
  ↓                              ↓
  ← ──────────────────────────  Return plan
  ↓                           
Validate state                
  ↓                           
Execute block placement       
  ↓                           
Complete                      
```

**Critical Boundary:**
- **Background:** Lua, pattern math, noise, palette lookups, plan construction
- **Main Thread:** World access, block placement, player interaction, inventory changes

---

## Phase A: Measurement & Baseline

### Milestone 1: Establish Performance Baseline
**Goal:** Prove exactly where time is spent before optimizing

**Implementation:**
1. Add timing instrumentation to `PatternWandWorker.java`:
   - Lua execution time
   - Plan construction time  
   - Block placement time
   - Total operation time

2. Add debug command: `/patternwand benchmark <pattern> <size>`

3. Test with varying block counts:
   - 10 blocks
   - 100 blocks
   - 500 blocks
   - 1000 blocks
   - 5000 blocks

**Deliverable:** Timing data table showing bottlenecks

**Pass Condition:** Data confirms Lua execution dominates for large patterns (>100 blocks)

**Stop Condition:** If Lua isn't the bottleneck, reassess the need for async execution

**Files Modified:**
- `src/main/java/com/xXseesXx/patternwand/items/PatternWandWorker.java`
- `src/main/java/com/xXseesXx/patternwand/commands/PatternWandCommand.java`

---

## Phase B: Safe Optimization

### Milestone 2: Cache API Wrappers Per Pattern Execution
**Goal:** Eliminate per-block wrapper allocation overhead

**Current Behavior:**
```
For each block:
  - Create NoiseAPI
  - Create PaletteAPI  
  - Create UtilAPI
  - Create DebugAPI
  - Create Lua wrappers
  - Execute pattern
  - Discard all wrappers
```

**Target Behavior:**
```
For pattern execution:
  - Create APIs once
  - Create wrappers once
  For each block:
    - Execute pattern (reuse wrappers)
  - Discard wrappers
```

**Implementation:**
1. Move wrapper creation to pattern execution start in `ScriptEngine.java`
2. Pass wrapper references through execution context
3. Reuse same wrappers for all blocks in a single operation

**Constraints:**
- Do NOT refactor ScriptEngine architecture
- Do NOT change public API signatures unnecessarily  
- Preserve existing behavior exactly

**Testing:**
- Run all example patterns
- Compare output with baseline
- Verify timing improvement
- Check for allocation reduction

**Pass Condition:**
- All patterns produce identical results
- Measurable performance improvement
- No regression in existing tests

**Files Modified:**
- `src/main/java/com/xXseesXx/patternwand/patterns/scripted/ScriptEngine.java`
- Possibly `PlacementContext.java` if wrappers need context storage

---

## Phase C: Concurrency Validation

### Milestone 3: Prove Lua Thread Safety Requirements
**Goal:** Determine if Globals isolation is actually needed through testing, not speculation

**Test Suite:**

#### Test 3A: Concurrent Same-Script Execution
```java
// Run same CompiledScript concurrently with different seeds
Thread 1: pattern X, seed 100
Thread 2: pattern X, seed 200  
Thread 3: pattern X, seed 300
```
Verify each produces deterministic output matching single-threaded execution.

#### Test 3B: Random State Isolation
```lua
-- Test pattern using math.random()
math.randomseed(seed)
return math.random(10)
```
Run concurrently, verify no cross-contamination of random state.

#### Test 3C: Global Variable Pollution
```lua
-- Test pattern with global mutation
counter = (counter or 0) + 1
return counter
```
Run concurrently, detect if globals leak between executions.

#### Test 3D: Multiple Different Scripts
```
Thread 1: pattern_bricks.lua
Thread 2: pattern_noise.lua
Thread 3: pattern_checkerboard.lua  
```
Verify no interference between different scripts.

**Decision Point:**

**If all tests pass:** Do NOT add Globals isolation. Document that LuaJIT thread safety is sufficient.

**If tests fail:** Proceed with Globals isolation per original plan.

**Files Created:**
- `src/test/java/com/xXseesXx/patternwand/patterns/LuaConcurrencyTest.java`

---

## Phase D: Async Foundation

### Milestone 4: Define Async Contract
**Goal:** Establish what data can cross thread boundaries

**Create Immutable Snapshot:**
```java
class PatternExecutionSnapshot {
    // Immutable data safe for background threads
    final CompiledScript script;
    final long seed;
    final Map<String, Object> parameters;
    final PlacementContext context;
    final int[] paletteWeights; // snapshot, not live ItemStack[]
    final boolean debugMode;
}
```

**Create Result Container:**
```java
class PlacementPlan {
    final List<BlockPlacement> placements;
    final String patternName;
    final int blocksCalculated;
    final long computeTimeMs;
}

class BlockPlacement {
    final int x, y, z;
    final int paletteIndex;
}
```

**Rule:** `PlacementPlan` must contain ONLY:
- Primitive types
- Immutable data
- NO World, EntityPlayer, ItemStack, or Minecraft classes

**Files Created:**
- `src/main/java/com/xXseesXx/patternwand/patterns/PatternExecutionSnapshot.java`
- Update `src/main/java/com/xXseesXx/patternwand/patterns/PlacementPlan.java`

---

### Milestone 5: Add Dedicated Lua Executor
**Goal:** Create controlled thread pool for Lua execution

**Implementation:**
```java
public class LuaExecutorService {
    private static final ExecutorService executor = 
        Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
            .setNameFormat("PatternWand-Lua-%d")
            .setDaemon(true)
            .build());
    
    public static Future<PlacementPlan> submitPlanGeneration(
        PatternExecutionSnapshot snapshot
    ) {
        return executor.submit(() -> generatePlan(snapshot));
    }
    
    public static void shutdown() {
        executor.shutdown();
    }
}
```

**Configuration:**
- Start with 2 threads (not configurable yet)
- Daemon threads (won't prevent shutdown)
- Named threads for debugging

**Files Created:**
- `src/main/java/com/xXseesXx/patternwand/async/LuaExecutorService.java`

**Files Modified:**
- `PatternWandMod.java` (call shutdown on mod unload)

---

### Milestone 6: Implement Async Plan Generation  
**Goal:** Make Lua execution non-blocking

**Modify PatternWandWorker:**
```java
public void executePattern() {
    // 1. Snapshot immutable inputs
    PatternExecutionSnapshot snapshot = createSnapshot();
    
    // 2. Submit to background
    Future<PlacementPlan> future = 
        LuaExecutorService.submitPlanGeneration(snapshot);
    
    // 3. Return immediately (player continues gameplay)
    
    // 4. Store future for completion handling
    pendingJobs.put(player.getUUID(), future);
}
```

**Critical Constraint:**
The background thread must NOT call:
- `world.setBlock()`
- `player.addChatMessage()`
- `MinecraftServer.getServer()`
- Any Minecraft API

**Testing:**
- Player should be able to move immediately after using wand
- Multiple wands can be used before first completes
- Game remains responsive during Lua execution

**Files Modified:**
- `src/main/java/com/xXseesXx/patternwand/items/PatternWandWorker.java`

---

### Milestone 7: Main-Thread Completion
**Goal:** Execute placement when Lua finishes

**Implementation Options:**

**Option A: Tick Handler** (safer for Minecraft 1.7.10)
```java
@SubscribeEvent
public void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    
    for (PendingJob job : pendingJobs) {
        if (job.future.isDone()) {
            executePlacementPlan(job);
            pendingJobs.remove(job);
        }
    }
}
```

**Option B: Callback** (if Java version supports)
```java
future.thenAcceptAsync(plan -> {
    MinecraftForge.EVENT_BUS.post(
        new PlacementReadyEvent(plan, player)
    );
}, mainThreadExecutor);
```

**Choose Option A** for Minecraft 1.7.10 compatibility.

**Files Created:**
- `src/main/java/com/xXseesXx/patternwand/async/AsyncPlacementHandler.java`

**Files Modified:**
- `PatternWandMod.java` (register tick handler)

---

### Milestone 8: Validate Before Placement
**Goal:** Prevent stale placements after world changes

**Validation Checks:**
```java
private boolean isPlacementValid(PlacementPlan plan) {
    // Player validation
    if (!player.isEntityAlive()) return false;
    if (player.dimension != plan.dimension) return false;
    
    // World validation  
    if (world != plan.world) return false;
    
    // Job validation
    if (job.cancelled) return false;
    if (newerJobExists(player)) return false;
    
    // Wand validation
    ItemStack wand = getHeldWand(player);
    if (wand == null) return false;
    
    // Plan sanity
    if (plan.blockCount > MAX_BLOCKS) return false;
    
    return true;
}
```

**Failure Handling:**
- Log rejection reason
- Notify player if debug mode
- Do NOT place any blocks

**Files Modified:**
- `src/main/java/com/xXseesXx/patternwand/async/AsyncPlacementHandler.java`

---

### Milestone 9: Add Cancellation
**Goal:** Stop unnecessary work early

**Cancellation Triggers:**
- Player disconnects
- Player dies
- New pattern operation starts
- Player switches dimension
- Explicit cancel command

**Implementation:**
```java
public class CancellableJob {
    private final Future<PlacementPlan> future;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    
    public void cancel() {
        cancelled.set(true);
        future.cancel(false); // don't interrupt
    }
    
    public boolean isCancelled() {
        return cancelled.get();
    }
}
```

**Lua Side Checking:**
```java
// In pattern execution loop
if (job.isCancelled()) {
    return null; // early exit
}
```

**Files Modified:**
- `LuaExecutorService.java`
- `AsyncPlacementHandler.java`
- `ScriptEngine.java` (add cancellation checks)

---

### Milestone 10: Add Execution Timeout
**Goal:** Prevent runaway Lua scripts

**Implementation:**
```java
private static final long TIMEOUT_MS = 30_000; // 30 seconds

PlacementPlan generatePlan(PatternExecutionSnapshot snapshot) {
    long start = System.nanoTime();
    
    for (BlockPos pos : positions) {
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        if (elapsed > TIMEOUT_MS) {
            throw new TimeoutException(
                "Pattern exceeded 30s timeout after " + 
                blocksProcessed + " blocks"
            );
        }
        
        executeLuaForBlock(pos);
    }
}
```

**Timeout Handling:**
- Log timeout with pattern name and block count
- Send error message to player
- Return partial plan or null
- Do not crash the server

**Files Modified:**
- `LuaExecutorService.java`
- `ScriptEngine.java`

---

### Milestone 11: Establish Async Threshold
**Goal:** Only use async for operations that benefit

**Implementation:**
```java
private static final int ASYNC_THRESHOLD = 100; // blocks

public void executePattern(List<BlockPos> positions) {
    if (positions.size() < ASYNC_THRESHOLD) {
        // Synchronous path (existing code)
        executeSynchronously(positions);
    } else {
        // Asynchronous path (new code)  
        executeAsynchronously(positions);
    }
}
```

**Benchmarking:**
- Test thresholds: 50, 100, 200, 500
- Measure overhead vs. benefit
- Find crossover point where async wins

**Configuration:**
- Start with threshold=100, disabled by default
- Add `/patternwand async <on|off|threshold>` command
- Document as experimental feature

**Files Modified:**
- `Config.java` (add async settings)
- `PatternWandWorker.java`
- `PatternWandCommand.java`

---

## Phase E: Stress Testing

### Milestone 12: Concurrent Player Testing
**Goal:** Verify multi-player correctness

**Test Scenarios:**

**Test 12A: Same Pattern, Different Seeds**
```
Player A: /pw set noise seed=100
Player B: /pw set noise seed=200
Both use wand simultaneously
```
Verify each gets correct pattern with their seed.

**Test 12B: Different Patterns**
```
Player A: checkerboard
Player B: bricks  
Player C: gradient
All use wands simultaneously
```
Verify no pattern interference.

**Test 12C: Overlapping Regions**
```
Player A: 1000 blocks at (0,0,0)
Player B: 1000 blocks at (5,0,0) (overlapping)
```
Verify correct placement order and no corruption.

**Files Created:**
- `docs/ASYNC_TESTING_SCENARIOS.md`

---

### Milestone 13: Stress Test Executor
**Goal:** Find and fix breaking points

**Stress Tests:**

**Test 13A: Queue Saturation**
```
1 player, 10 rapid wand uses
5 players, 5 wand uses each
```
Expected: Queue doesn't grow indefinitely, oldest jobs cancel gracefully.

**Test 13B: Resource Limits**
```
10 players, 10 concurrent 5000-block patterns
```
Monitor: CPU usage, memory usage, server TPS.

**Test 13C: Pathological Pattern**
```lua
-- Expensive pattern
while true do
    math.sin(x) * math.cos(y)  
end
```
Expected: Timeout fires, server remains stable.

**Add Protection:**
- Max concurrent jobs per player (default: 1)
- Max queued jobs total (default: 10)
- Reject new jobs when queue full

**Files Modified:**
- `LuaExecutorService.java` (add queue limits)

---

### Milestone 14: Failure Testing
**Goal:** Verify graceful degradation

**Test Cases:**

**14A: Lua Runtime Error**
```lua
error("Intentional crash")
```
Expected: Error logged, player notified, server continues.

**14B: Invalid Return Values**
```lua
return "not_a_number"
return -1
return 999999  
return {} 
```
Expected: Same behavior as synchronous execution.

**14C: Player Disconnect During Execution**
```
1. Player starts 5000-block pattern
2. Player disconnects immediately
3. Lua continues in background
4. Completion handler validates player
```
Expected: Job completes but placement rejected.

**14D: Dimension Change**
```
1. Player starts pattern in overworld
2. Player enters nether mid-execution
```
Expected: Placement rejected due to dimension mismatch.

**14E: Server Shutdown**
```
1. Start 5 long-running patterns
2. /stop command
```
Expected: Server waits for graceful shutdown, no hang.

**Files Modified:**
- `PatternWandMod.java` (improve shutdown handling)
- `AsyncPlacementHandler.java` (improve validation)

---

### Milestone 15: Regression Testing
**Goal:** Verify all existing patterns still work correctly

**Test Matrix:**

| Pattern          | Sync Result | Async Result | Match? |
|------------------|-------------|--------------|--------|
| checkerboard     | [hash]      | [hash]       | ✓      |
| bricks           | [hash]      | [hash]       | ✓      |  
| noise_terrain    | [hash]      | [hash]       | ✓      |
| gradient         | [hash]      | [hash]       | ✓      |
| [all examples]   | ...         | ...          | ✓      |

**Methodology:**
1. Run pattern synchronously, hash result
2. Run same pattern asynchronously, hash result  
3. Compare hashes (for deterministic patterns)
4. For random patterns, verify statistical properties

**Files Created:**
- `src/test/java/com/xXseesXx/patternwand/patterns/RegressionTest.java`

---

### Milestone 16: Performance Comparison
**Goal:** Quantify the improvement

**Final Benchmark:**

| Mode              | 100 Blocks | 500 Blocks | 1000 Blocks | 5000 Blocks |
|-------------------|------------|------------|-------------|-------------|
| Original Sync     | ?ms        | ?ms        | ?ms         | ?ms         |
| Optimized Sync    | ?ms        | ?ms        | ?ms         | ?ms         |
| Async (compute)   | ?ms        | ?ms        | ?ms         | ?ms         |
| Async (blocking)  | ?ms        | ?ms        | ?ms         | ?ms         |

**Key Metrics:**
- Main thread stall time (most important)
- Total operation time  
- Server TPS impact
- Memory usage

**Success Criteria:**
- Main thread stall reduced by >80% for 1000+ block patterns
- No regression for small patterns (<100 blocks)
- Server TPS impact negligible

**Files Created:**
- `docs/ASYNC_PERFORMANCE_RESULTS.md`

---

### Milestone 17: Globals Isolation Decision
**Goal:** Make evidence-based decision on complexity

**Decision Tree:**

```
Did concurrency tests (Milestone 3) fail?
│
├─ NO → Do nothing. LuaJIT isolation sufficient.
│        Document in code comments.
│
└─ YES → Are failures critical/frequent?
          │
          ├─ YES → Implement Globals isolation
          │         - Per-execution Globals
          │         - Pooled Globals instances
          │         - Test isolation again
          │
          └─ NO → Document as known limitation
                   Add warning to advanced users
                   Recommend avoiding global state
```

**If Implementing Globals:**
```java
class GlobalsPool {
    private final Queue<Globals> available = new ConcurrentLinkedQueue<>();
    
    public Globals acquire() {
        Globals g = available.poll();
        return (g != null) ? g : createNewGlobals();
    }
    
    public void release(Globals g) {
        resetGlobals(g);
        available.offer(g);
    }
}
```

**Files Created (if needed):**
- `src/main/java/com/xXseesXx/patternwand/async/GlobalsPool.java`

---

### Milestone 18: Simplify and Document
**Goal:** Remove unnecessary abstractions, document decisions

**Code Review:**
- Remove classes that exist only from experimentation
- Collapse single-use abstractions
- Inline obvious delegations
- Rename unclear variables

**Documentation:**
```java
/**
 * THREAD SAFETY:
 * This class is called from background threads. It must NOT access:
 * - World
 * - EntityPlayer  
 * - ItemStack (unless snapshotted)
 * - MinecraftServer
 * - Any Minecraft class that touches game state
 * 
 * All Minecraft operations happen in AsyncPlacementHandler on the main thread.
 */
public class LuaPatternGenerator {
    // ...
}
```

**Files Updated:**
- All async-related classes (add thread safety docs)
- `README.md` (document async feature)

---

## Phase F: Release Preparation

### Milestone 19: Configuration and Commands
**Goal:** Make async feature controllable

**New Commands:**
```
/patternwand async on              - Enable async (default: off)
/patternwand async off             - Disable async
/patternwand async threshold <N>   - Set block threshold
/patternwand async status          - Show current settings
```

**Config Options:**
```java
// config/patternwand/async.cfg
async {
    enabled=false          # Experimental, disabled by default
    threshold=100          # Min blocks for async
    maxConcurrentJobs=10   # Max jobs in queue
    maxJobsPerPlayer=1     # Max jobs per player
    timeoutSeconds=30      # Max execution time
    workerThreads=2        # Background thread count
}
```

**Files Modified:**
- `Config.java`
- `PatternWandCommand.java`

---

### Milestone 20: User Documentation
**Goal:** Explain feature to users

**Create Documentation:**
```markdown
# Async Pattern Execution (Experimental)

## What It Does
Moves Lua pattern calculation to background threads, preventing 
server lag during large pattern generation.

## When To Use
- Patterns with 1000+ blocks
- Server experiencing TPS drops during wand use
- Multiple players using wands simultaneously

## How To Enable
`/patternwand async on`

## Limitations
- Experimental feature
- May have edge cases
- Disabled by default

## Troubleshooting
If patterns behave incorrectly with async enabled:
1. `/patternwand async off`
2. Report issue with pattern script
```

**Files Created:**
- `docs/ASYNC_FEATURE.md`
- Update `README.md`

---

## Definition of Done

The async feature is considered complete when:

### Correctness ✓
- [ ] All existing patterns produce identical results
- [ ] Concurrent patterns don't interfere
- [ ] Placement happens exclusively on main thread
- [ ] Async failures don't crash server
- [ ] Disconnected players don't receive stale placement
- [ ] Dimension changes invalidate pending jobs

### Performance ✓
- [ ] Large patterns don't block server thread
- [ ] Main thread stall reduced by >80% for 1000+ blocks
- [ ] Small patterns have <5% overhead
- [ ] Server TPS remains stable during async execution

### Safety ✓
- [ ] Slow patterns have bounded execution time (timeout)
- [ ] Jobs can be cancelled  
- [ ] Executor cannot grow without bound
- [ ] Server shutdown doesn't leave worker threads behind
- [ ] Queue limits prevent resource exhaustion

### Maintainability ✓
- [ ] No unnecessary abstractions
- [ ] Thread safety documented clearly
- [ ] Async boundary obvious from code
- [ ] Configuration options sensible
- [ ] User documentation complete

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| LuaJIT not thread-safe | High | Test thoroughly (Milestone 3), add Globals isolation if needed |
| World access from worker | Critical | Code review, runtime assertions, testing |
| Queue exhaustion | Medium | Limits, cancellation, monitoring |
| Slower than sync | Medium | Benchmarking, threshold tuning |
| Edge case bugs | Medium | Comprehensive testing, default to off |

---

## Rollback Plan

At any milestone, if critical issues arise:

1. **Disable by default** - Set `async.enabled=false` in config
2. **Keep sync path** - Never remove original synchronous code
3. **Feature flag** - Async is opt-in, users can always disable
4. **Revert** - All changes in feature branch, can abandon

---

## Timeline Estimate

**Fast Track (focused work):** 2-3 weeks
**Realistic (with testing):** 4-6 weeks  
**Conservative (with interruptions):** 8-10 weeks

**Milestone Effort Estimates:**
- Phase A (Measurement): 2-3 days
- Phase B (Optimization): 2-3 days
- Phase C (Concurrency): 3-4 days
- Phase D (Async Core): 5-7 days
- Phase E (Stress Test): 4-5 days
- Phase F (Polish): 2-3 days

---

## Success Metrics

**Before:**
- 1000-block pattern: 2000ms main thread stall
- Server TPS drops from 20 to 15 during operation
- Players experience lag during wand use

**After:**
- 1000-block pattern: <100ms main thread stall  
- Server TPS remains 19-20
- Players can continue gameplay while pattern generates

---

## Open Questions

1. **Globals Isolation**: Wait for Milestone 3 results before deciding
2. **Thread Count**: Start with 2, benchmark later
3. **Async Threshold**: Test 100, adjust based on data
4. **Tick vs Callback**: Use tick handler for 1.7.10 compatibility

---

## References

- Original Plan: `LUA_ASYNC_IMPLEMENTATION.md`
- Maintainer Critique: `GTNH_MAINTAINER_CRITIQUE.md`
- Synthesis: `implementation_ROADMAP.txt`
- Threading Background: `THREADING_*.md`

---

**Next Step:** Begin Phase A, Milestone 1 - Establish Performance Baseline
