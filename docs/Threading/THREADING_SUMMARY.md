# Threading Investigation Summary

## Current State

PatternWand already has good architecture:
- **Separated execution phases**: Lua execution → Material validation → Block placement
- **Batch processing**: `PlacementPlan` intermediary already exists
- **Timeout protection**: Lua execution has 10s timeout via `ExecutorService`

**Problem**: Everything runs on main thread, causing freezes on large patterns (1000+ blocks).

## Where Time Is Spent

Based on debug timing phases:
- **Phase 1 (Lua execution)**: 60-80% of time
- **Phase 5 (Block placement)**: 15-30% of time  
- **Phases 2-4 (aggregation/validation)**: <5% of time

## Threading Constraints

### Can Move Off Main Thread ✅
- Lua script execution (pure computation)
- Material aggregation (data processing)
- Material validation (read-only inventory access)

### Must Stay On Main Thread ❌
- World block placement (`World.setBlock()` not thread-safe)
- Player inventory consumption (not thread-safe)
- Any world state modification

## Three Approaches

### 1. Async Plan Generation ⭐ Recommended
**Move Lua execution to background thread, keep placement on main thread.**

**Pros:**
- Addresses main bottleneck (60-80% of time)
- Low risk - Lua already isolated
- Minimal code changes
- Can be enhanced later

**Cons:**
- Still has placement lag on very large patterns
- Need to handle player disconnect during async work

**Effort:** 2-4 hours  
**Risk:** Low-Medium

---

### 2. Chunked Placement
**Spread block placement over multiple ticks (no threading).**

**Pros:**
- No threading complexity (stays on main thread)
- Safer - no race conditions
- Progressive feedback to player
- Eliminates placement lag spikes

**Cons:**
- Lua execution still blocks
- More complex state management (partial completion)
- Need to reserve materials upfront

**Effort:** 4-8 hours  
**Risk:** Low

---

### 3. Hybrid Approach
**Async plan generation + chunked placement.**

**Pros:**
- Best user experience
- No lag at any phase
- Can handle massive patterns (10,000+ blocks)

**Cons:**
- Most complex
- Highest development/testing effort

**Effort:** 8-16 hours  
**Risk:** Medium-High

## Recommended Implementation

**Start with Approach 1 (Async Plan Generation)**

### Phase 1: Async Lua Execution

1. Create `AsyncPlacementExecutor` class (see `AsyncPlacementExecutor.java.example`)
2. Modify `PatternWandWorker.placeBlocksWithPattern()` to use async wrapper
3. Add main thread callback using `MinecraftServer.addScheduledTask()`
4. Add player validation before placement execution

**Key changes:**
```java
// Background thread: Lua + validation (Phases 1-3)
Future<PlacementPlan> planFuture = executor.submit(() -> generatePlan(...));

// Main thread callback: Block placement (Phases 4-5)
MinecraftServer.getServer().addScheduledTask(() -> {
    PlacementPlan plan = planFuture.get();
    executePlan(plan);
});
```

### Phase 2 (Future): Add Chunked Placement

After async works well, add tick-spreading for placement:
```java
if (plan.size() > 500) {
    new ChunkedPlacementExecutor(plan, worker, player).start();
} else {
    executePlan(plan); // Immediate for small patterns
}
```

## Files to Modify

### New Files
- `com.xXseesXx.patternwand.execution.AsyncPlacementExecutor` - Async coordinator
- `com.xXseesXx.patternwand.execution.ChunkedPlacementExecutor` - Tick spreading (future)

### Modified Files
- `PatternWandWorker.java` - Add async wrapper method
  - Make `generatePlan()` accessible (package-private)
  - Extract `executePlacementPlan()` from Phase 5
  - Add `placeBlocksWithPatternAsync()` method
  
- `Config.java` - Add configuration options:
  ```java
  public static boolean enableAsyncPlanning = true;
  public static int maxAsyncPlanSize = 10000;
  public static int asyncPlanTimeoutSeconds = 30;
  public static int blocksPerTick = 100; // For future chunked placement
  ```

- `PatternWandMod.java` - Add shutdown hook:
  ```java
  @EventHandler
  public void serverStopping(FMLServerStoppingEvent event) {
      AsyncPlacementExecutor.shutdown();
  }
  ```

## Testing Checklist

### Unit Tests
- [ ] Async plan generation with 100, 1000, 5000 blocks
- [ ] Error propagation from background thread
- [ ] Thread-safe `PlacementPlan` construction

### Integration Tests
- [ ] Player disconnect during async execution
- [ ] World unload during async execution
- [ ] Multiple concurrent wand uses
- [ ] Pattern errors during async phase
- [ ] Server shutdown with active async tasks

### Performance Tests
- [ ] Measure TPS impact before/after
- [ ] Measure perceived lag for various sizes:
  - 100 blocks (should be instant)
  - 1,000 blocks (should be ~1s planning, instant feeling)
  - 5,000 blocks (should be 3-5s planning, no freeze)
  - 10,000+ blocks (may need chunked placement)

## Expected Results

**Before (Sync):**
- 1,000 block pattern: 2-5 second freeze ❌
- 5,000 block pattern: 10-30 second freeze ❌
- Player cannot do anything during execution

**After (Async Plan Generation):**
- 1,000 block pattern: No freeze, 2s background ✅
- 5,000 block pattern: No freeze, 10s background ✅
- Player can move/interact during planning
- Only brief pause during placement (~0.5-2s)

**After (Async + Chunked):**
- Any size pattern: No freeze ever ✅
- Progressive feedback to player
- 100 blocks per tick = imperceptible lag

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Player logs out during async | Validate player exists in callback |
| World unloads during async | Check world loaded before placement |
| Race condition on inventory | Re-validate materials on main thread |
| Memory overhead for large plans | Add `maxAsyncPlanSize` config limit |
| Mod compatibility | Keep all world access on main thread |

## References

- Investigation details: `THREADING_INVESTIGATION.md`
- Implementation example: `AsyncPlacementExecutor.java.example`
- Usage examples: `ASYNC_USAGE_EXAMPLE.java`

## Decision Tree

```
Is pattern > 100 blocks?
├─ NO → Execute synchronously (fast enough)
└─ YES → Is async enabled in config?
    ├─ NO → Execute synchronously (user preference)
    └─ YES → Generate plan async
        └─ Is plan > 500 blocks?
            ├─ NO → Place immediately on callback
            └─ YES → Use chunked placement (future)
```

## Next Steps

1. **Implement** `AsyncPlacementExecutor` class
2. **Modify** `PatternWandWorker` to use async execution
3. **Add** config options for feature toggle
4. **Test** with various pattern sizes
5. **Monitor** for edge cases and race conditions
6. **Document** for users (mention async in README)
7. **(Future)** Add chunked placement for very large patterns
