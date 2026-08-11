# Threading Investigation: Async Lua Execution & Block Placement

## Current Architecture

### Execution Flow (5 Phases)
Currently in `PatternWandWorker.placeBlocksWithPattern()`:

1. **Phase 1: Generate Placement Plan** - Execute Lua for all positions
2. **Phase 2: Aggregate Materials** - Count required blocks per type
3. **Phase 3: Validate Materials** - Check player inventory
4. **Phase 4: Material Consumption** - Handled by parent's placeBlocks()
5. **Phase 5: Execute Plan** - Place blocks in world (loop calling parent's placeBlocks per block)

**Current Thread:** Main server thread (all phases)

**Key Points:**
- Already batched: Lua execution (Phase 1) is separated from world modification (Phase 5)
- `PlacementPlan` acts as an intermediate data structure
- Lua execution already has timeout protection (10s) via `ExecutorService` in `ScriptEngine`
- Phase 5 loops through plan calling `super.placeBlocks()` for each position individually

### Threading Constraints in Minecraft 1.7.10

**What can be moved off main thread:**
- ✅ Lua script execution (Phase 1) - pure computation, no world access
- ✅ Material aggregation (Phase 2) - pure data processing
- ✅ Material validation (Phase 3) - player inventory is thread-safe for reads

**What MUST stay on main thread:**
- ❌ World block placement (Phase 5) - Minecraft's world is NOT thread-safe
- ❌ Player inventory consumption (Phase 4) - inventory modification must be synchronized
- ❌ Client-server packet synchronization

## Threading Opportunities

### Option 1: Async Plan Generation (Recommended)

**Move Phases 1-3 to background thread, execute 4-5 on main thread.**

**Benefits:**
- Main thread only blocks during actual world modification
- Large patterns (1000+ blocks) won't freeze the game during Lua execution
- Material validation happens before player sees delay
- Minimal architectural changes

**Implementation:**
```java
// In PatternWandWorker.placeBlocksWithPattern()
CompletableFuture<PlacementPlan> planFuture = CompletableFuture.supplyAsync(() -> {
    // Phase 1: Generate plan (Lua execution)
    PlacementPlan plan = generatePlan(...);
    
    // Phase 2: Aggregate materials
    Map<String, MaterialRequirement> requirements = plan.getMaterialRequirements();
    
    // Phase 3: Validate materials
    List<MaterialRequirement> missing = validateMaterials(requirements, playerShim);
    if (!missing.isEmpty()) {
        throw new InsufficientMaterialsException(missing);
    }
    
    return plan;
}, Executors.newCachedThreadPool());

// Back on main thread
planFuture.thenAcceptAsync(plan -> {
    // Phase 4-5: Consume materials and place blocks
    executeplan(plan, ...);
}, minecraftMainThreadExecutor);
```

**Challenges:**
- Need access to Minecraft's main thread executor (check `MinecraftServer.getServer()`)
- Error handling across thread boundaries
- Player might log out/move during async execution
- Need to validate world state hasn't changed when plan executes

**Risk Level:** Medium

---

### Option 2: Chunked Execution with Tick Spreading

**Execute plan over multiple ticks, placing N blocks per tick.**

**Benefits:**
- Prevents single-tick lag spikes for massive patterns
- Stays entirely on main thread (safer)
- Progressive feedback to player
- No threading complexity

**Implementation:**
```java
// In PatternWandWorker
private static class ProgressiveExecutor {
    private static final int BLOCKS_PER_TICK = 50;
    private final PlacementPlan plan;
    private int currentIndex = 0;
    
    public void scheduleExecution() {
        MinecraftServer.getServer().addScheduledTask(this::executeBatch);
    }
    
    private void executeBatch() {
        int end = Math.min(currentIndex + BLOCKS_PER_TICK, plan.size());
        
        for (int i = currentIndex; i < end; i++) {
            PlacementEntry entry = plan.getPlacements().get(i);
            // Place block...
        }
        
        currentIndex = end;
        
        if (currentIndex < plan.size()) {
            // Schedule next batch for next tick
            MinecraftServer.getServer().addScheduledTask(this::executeBatch);
        }
    }
}
```

**Challenges:**
- Player state might change between ticks
- Need to reserve materials upfront
- Risk of partial completion if player disconnects
- Undo becomes more complex

**Risk Level:** Low

---

### Option 3: Hybrid Approach (Best of Both Worlds)

**Async plan generation + Chunked placement**

1. Generate plan async (Phases 1-3) in background
2. Return to main thread with complete plan
3. Spread block placement across multiple ticks

**Benefits:**
- No Lua lag during generation
- No placement lag spikes
- Safest threading model
- Best user experience for large patterns

**Challenges:**
- Most complex implementation
- Requires both async and tick-based coordination

**Risk Level:** Medium-High

---

## Minecraft 1.7.10 Threading APIs

### Available Threading Tools

1. **ExecutorService** (already in use for Lua timeout)
   ```java
   private static final ExecutorService executor = Executors.newCachedThreadPool();
   ```

2. **MinecraftServer Scheduled Tasks**
   ```java
   MinecraftServer.getServer().addScheduledTask(new Runnable() {
       public void run() {
           // Runs on main thread next tick
       }
   });
   ```

3. **World.addBlockEvent** (for spreading updates)
   - Not suitable for placement, only for block updates

4. **CompletableFuture** (Java 8+, Minecraft 1.7.10 uses Java 6-8)
   - Check if available via ForgeGradle's Java version
   - Alternative: Use raw `Future<T>` with `Executors`

### Thread Safety Notes

**Thread-Safe Operations:**
- Read-only world queries (mostly safe, but avoid during world save)
- Player inventory queries (read-only)
- Lua execution (isolated globals per thread)
- NBT reading

**NOT Thread-Safe:**
- `World.setBlock()` / `World.setBlockState()`
- Player inventory modification
- Entity spawning/modification
- Chunk loading/unloading
- Any GUI updates

---

## Recommended Implementation Plan

### Phase 1: Async Plan Generation (Lua Only)

**Goal:** Move Lua execution off main thread

**Changes:**
1. Modify `PatternWandWorker.placeBlocksWithPattern()` to wrap `generatePlan()` in async task
2. Add main thread callback using `MinecraftServer.addScheduledTask()`
3. Add validation that player/world state is still valid when callback executes
4. Improve error reporting for async failures

**Benefits:**
- Immediate improvement for large patterns
- Low risk - Lua already has timeout protection
- Minimal code changes

**Estimated Effort:** 2-4 hours

---

### Phase 2: Chunked Placement (Future)

**Goal:** Spread block placement across ticks

**Changes:**
1. Create `ProgressivePlacementExecutor` class
2. Track placement progress in NBT
3. Add per-tick block limit configuration
4. Handle cancellation/player disconnect

**Benefits:**
- Eliminates placement lag spikes
- Enables very large patterns (10,000+ blocks)
- Better server performance

**Estimated Effort:** 4-8 hours

---

## Risks & Considerations

### Threading Risks

1. **Race Conditions**
   - World state changes during async plan generation
   - Solution: Validate world state before placement

2. **Player State Changes**
   - Player logs out during execution
   - Player moves items during async work
   - Solution: Re-validate before placement phase

3. **Memory Overhead**
   - Large `PlacementPlan` objects held in memory
   - Solution: Add max plan size limit (e.g., 10,000 blocks)

4. **Mod Compatibility**
   - Other mods might not expect async queries
   - Solution: Keep world access on main thread

### Performance Considerations

**Current Bottlenecks** (from debug timing):
- Phase 1 (Lua): 60-80% of execution time for complex patterns
- Phase 5 (Placement): 15-30% for large patterns
- Phases 2-4: Negligible (<5%)

**Expected Improvements:**
- Option 1 (Async): 60-80% reduction in perceived lag
- Option 2 (Chunked): Eliminates lag spikes, spreads load
- Option 3 (Hybrid): 90%+ reduction in perceived lag

---

## Code Locations

### Key Files to Modify

1. **`PatternWandWorker.java`** (Line ~410)
   - `placeBlocksWithPattern()` method
   - Add async wrapper for plan generation
   - Add main thread callback

2. **`ScriptEngine.java`** (Already has ExecutorService)
   - Already uses `ExecutorService` for timeout
   - Can reuse for async pattern execution

3. **`PlacementPlan.java`** (Data structure - no changes needed)
   - Already separates planning from execution
   - Thread-safe by design (immutable after generation)

4. **New File: `AsyncPlacementExecutor.java`** (Optional)
   - Coordinate async plan generation
   - Handle thread callbacks
   - Manage error propagation

### Configuration Options to Add

```java
// In Config.java
public static int maxAsyncPlanSize = 10000; // Limit plan size for memory
public static int blocksPerTick = 50; // For chunked placement
public static boolean enableAsyncPlanning = true; // Feature toggle
public static int asyncPlanTimeout = 30; // Seconds before canceling async plan
```

---

## Testing Strategy

### Unit Tests
1. Test async plan generation with various pattern sizes
2. Test thread safety of `PlacementPlan` construction
3. Test error propagation from background threads

### Integration Tests
1. Test with player logout during async execution
2. Test with world unload during execution
3. Test with very large patterns (5000+ blocks)
4. Test with pattern errors during async phase

### Performance Benchmarks
1. Measure TPS impact before/after
2. Measure perceived lag for various pattern sizes
3. Test with multiple concurrent wand uses

---

## Conclusion

**Recommended Approach:** Option 1 (Async Plan Generation)

**Reasoning:**
- Addresses the main bottleneck (Lua execution: 60-80% of time)
- Low risk - Lua already isolated, plan generation is pure computation
- Minimal code changes - wraps existing architecture
- Can be enhanced later with Option 2 for placement lag

**Next Steps:**
1. Implement async wrapper for `generatePlan()` in `PatternWandWorker`
2. Add main thread callback using `MinecraftServer.addScheduledTask()`
3. Add state validation before placement execution
4. Test with large patterns (1000+ blocks)
5. Monitor for race conditions and edge cases

**Future Enhancements:**
- Add chunked placement (Option 2) for very large patterns
- Add progress indicators for players during async work
- Consider pattern preview system using async generation
- Explore AE2 integration for auto-crafting materials
