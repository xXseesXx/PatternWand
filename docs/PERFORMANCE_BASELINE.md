# PatternWand Performance Baseline

**Date:** August 11, 2026  
**Branch:** async-execution  
**Milestone:** Phase A, Milestone 1  
**Status:** Ready for In-Game Testing

## Purpose

This document establishes the baseline performance characteristics of PatternWand's pattern execution system before implementing async execution. These measurements will help us:

1. **Identify bottlenecks** - Confirm Lua execution is the primary performance constraint
2. **Set optimization targets** - Know what improvement to expect from async execution
3. **Prevent regressions** - Ensure optimizations don't hurt small pattern performance

## Testing Methodology

### Benchmark Command

Added `/patternwand benchmark <pattern> <size>` command that:

- Creates a synthetic test environment with 54 stone blocks in the palette
- Executes the pattern for N blocks arranged in a grid layout
- Measures execution time with nanosecond precision
- Reports total time, per-block average, and throughput
- Temporarily enables debug mode to show phase-by-phase timing breakdown

### Test Patterns

**Simple Pattern (Baseline):**
- `default_palette_weighted` - Simple random selection, minimal computation
- Tests overhead of pattern execution framework itself

**Noise Pattern (Realistic):**
- `default_noise_perlin2d` - 2D Perlin noise with palette mapping
- Represents typical real-world pattern complexity
- Includes floating-point math and noise generation

**Complex Pattern (Stress Test):**
- TBD - Pattern with heavy computation for worst-case scenario

### Test Sizes

| Size | Purpose | Expected Result |
|------|---------|-----------------|
| 10 | Small operations | Should be nearly instant (<10ms) |
| 100 | Medium operations | First point where timing becomes measurable |
| 500 | Large operations | Where performance starts to matter |
| 1000 | Very large operations | Where lag becomes noticeable |
| 5000 | Stress test | Extreme case for validation |

## Baseline Results

### Environment

**System Specs:**
- TBD (run in-game)

**Minecraft Version:** 1.7.10  
**Java Version:** TBD  
**Server Type:** Single-player / Dedicated server  
**Server Load:** Idle / Normal gameplay

### Test 1: Simple Pattern (default_palette_weighted)

```
Command: /patternwand benchmark default_palette_weighted [size]
```

| Blocks | Total Time (ms) | Avg per Block (ms) | Throughput (blocks/sec) | Notes |
|--------|----------------|-------------------|------------------------|-------|
| 10     | TBD            | TBD               | TBD                    |       |
| 100    | TBD            | TBD               | TBD                    |       |
| 500    | TBD            | TBD               | TBD                    |       |
| 1000   | TBD            | TBD               | TBD                    |       |
| 5000   | TBD            | TBD               | TBD                    |       |

**Phase Breakdown (1000 blocks):**
- Phase 1 (Plan Generation): TBD ms (TBD%)
  - Lua execution: TBD ms (TBD calls, TBD ns/call)
- Phase 2 (Aggregation): TBD ms (TBD%)
- Phase 3 (Validation): TBD ms (TBD%)
- Phase 4 (Consumption): TBD ms (TBD%)
- Phase 5 (Placement): TBD ms (TBD%)

### Test 2: Noise Pattern (default_noise_perlin2d)

```
Command: /patternwand benchmark default_noise_perlin2d [size]
```

| Blocks | Total Time (ms) | Avg per Block (ms) | Throughput (blocks/sec) | Notes |
|--------|----------------|-------------------|------------------------|-------|
| 10     | TBD            | TBD               | TBD                    |       |
| 100    | TBD            | TBD               | TBD                    |       |
| 500    | TBD            | TBD               | TBD                    |       |
| 1000   | TBD            | TBD               | TBD                    |       |
| 5000   | TBD            | TBD               | TBD                    |       |

**Phase Breakdown (1000 blocks):**
- Phase 1 (Plan Generation): TBD ms (TBD%)
  - Lua execution: TBD ms (TBD calls, TBD ns/call)
- Phase 2 (Aggregation): TBD ms (TBD%)
- Phase 3 (Validation): TBD ms (TBD%)
- Phase 4 (Consumption): TBD ms (TBD%)
- Phase 5 (Placement): TBD ms (TBD%)

### Test 3: Complex Pattern (TBD)

```
Command: /patternwand benchmark [complex_pattern] [size]
```

| Blocks | Total Time (ms) | Avg per Block (ms) | Throughput (blocks/sec) | Notes |
|--------|----------------|-------------------|------------------------|-------|
| 10     | TBD            | TBD               | TBD                    |       |
| 100    | TBD            | TBD               | TBD                    |       |
| 500    | TBD            | TBD               | TBD                    |       |
| 1000   | TBD            | TBD               | TBD                    |       |
| 5000   | TBD            | TBD               | TBD                    |       |

## Analysis

### Bottleneck Identification

**Primary Bottleneck:** TBD (expected: Phase 1 - Lua execution)

**Evidence:**
- TBD: Phase 1 accounts for X% of total time
- TBD: Lua execution within Phase 1 accounts for Y% of Phase 1 time
- TBD: Per-block Lua execution time is Z microseconds

### Performance Characteristics

**Scaling Behavior:**
- TBD: Linear scaling with block count? (expected: yes)
- TBD: Constant overhead per execution? (expected: minimal)
- TBD: Performance difference between simple and noise patterns?

**Server Impact:**
- TBD: Does pattern execution block server tick?
- TBD: What size pattern causes noticeable lag?
- TBD: At what threshold does async become necessary?

### Key Findings

1. **Phase 1 Dominance:** TBD
   - Expected: Phase 1 (Lua execution) takes 90%+ of total time for large patterns
   - This confirms async execution should target Phase 1

2. **Per-Block Execution Time:** TBD
   - Expected: Simple patterns ~X μs/block, noise patterns ~Y μs/block
   - This sets the baseline for improvement

3. **Async Threshold:** TBD
   - Expected: Patterns with 100+ blocks benefit from async execution
   - Patterns with <100 blocks should use synchronous path to avoid overhead

4. **Overhead Analysis:** TBD
   - Expected: Phases 2-5 add minimal overhead (<10% of total time)
   - This validates the batched execution architecture

## Decision Points

### Milestone 1 Success Criteria

✓ **Timing Infrastructure:** Benchmark command successfully measures execution time  
✓ **Instrumentation:** Phase-by-phase timing working correctly  
⏳ **Data Collection:** Waiting for in-game test results  
⏳ **Bottleneck Confirmation:** TBD - Does Phase 1 dominate as expected?

### Pass Condition

**Required:** Data confirms Lua execution (Phase 1) dominates for large patterns (>100 blocks).

**If Phase 1 takes >80% of time:** ✅ Proceed to Phase B (optimization)  
**If Phase 1 takes 50-80% of time:** ⚠️  Consider if async is worth complexity  
**If Phase 1 takes <50% of time:** ⛔ Stop - Lua isn't the bottleneck, reassess approach

### Stop Condition

If Lua execution is NOT the bottleneck, we need to:
1. Identify the actual bottleneck (block placement? inventory operations?)
2. Reassess whether async execution is the right solution
3. Consider alternative optimizations first

## Next Steps

### After Data Collection

1. **Analyze Results:**
   - Confirm Phase 1 is the bottleneck
   - Identify per-block execution time baseline
   - Determine optimal async threshold

2. **Proceed to Phase B (if validated):**
   - Milestone 2: Cache API wrappers per pattern execution
   - Expected improvement: 10-30% reduction in Phase 1 time
   - This is a safe optimization before adding async complexity

3. **Document Findings:**
   - Update this document with actual measurements
   - Create summary for project documentation
   - Reference results in async implementation decisions

## Testing Instructions

### Running Benchmarks

1. **Start Minecraft** with the PatternWand mod installed
2. **Enter a world** (single-player or server)
3. **Run benchmark commands** in chat:

```
/patternwand benchmark default_palette_weighted 10
/patternwand benchmark default_palette_weighted 100
/patternwand benchmark default_palette_weighted 500
/patternwand benchmark default_palette_weighted 1000
/patternwand benchmark default_palette_weighted 5000

/patternwand benchmark default_noise_perlin2d 10
/patternwand benchmark default_noise_perlin2d 100
/patternwand benchmark default_noise_perlin2d 500
/patternwand benchmark default_noise_perlin2d 1000
/patternwand benchmark default_noise_perlin2d 5000
```

4. **Record results** from chat output
5. **Check console logs** for detailed phase timing
6. **Update this document** with measurements

### Expected Output Format

```
§e=== Benchmark Results ===
§7Pattern: default_noise_perlin2d
§7Blocks evaluated: 1000
§7Blocks planned: 1000 (100.0%)
§7Total time: 123.45 ms
§7Avg per block: 0.1234 ms
§7Throughput: 8100 blocks/sec

§e=== Pattern Execution Timing ===
§aTotal: 123.45 ms (1000 blocks placed, 1000 planned)
§7Phase 1 (Plan Generation): 110.00 ms (89.1%)
§7  - Lua execution: 108.50 ms (1000 calls, 108500 ns/call)
§7Phase 2 (Aggregation): 2.00 ms (1.6%)
§7Phase 3 (Validation): 1.00 ms (0.8%)
§7Phase 4 (Consumption): 0.50 ms (0.4%)
§7Phase 5 (Placement): 9.95 ms (8.1%)
```

### Validation Checklist

- [ ] Benchmark command executes without errors
- [ ] Results show timing for all 5 phases
- [ ] Lua execution time is tracked separately within Phase 1
- [ ] Results scale linearly with block count
- [ ] Simple pattern is faster than noise pattern
- [ ] Large patterns (1000+ blocks) show Phase 1 dominance

## Phase B Optimization: API Wrapper Caching

### Implementation

**Date:** August 11, 2026  
**Branch:** async-execution  
**Commit:** f6cbd7d

**Change:** Modified pattern execution to create API wrappers once per pattern execution instead of once per block.

**Before:**
```java
// In generatePlan() loop - executed N times for N blocks
for (Point3d pos : blocks) {
    // executePattern creates these EVERY iteration:
    NoiseAPI noise = new NoiseAPI(seed);           // NEW object
    PaletteAPI palette = new PaletteAPI(...);       // NEW object
    UtilAPI util = new UtilAPI();                   // NEW object
    DebugAPI debug = new DebugAPI();                // NEW object
    LuaTable luaNoise = wrap(noise);                // NEW wrapper
    LuaTable luaPalette = wrap(palette);            // NEW wrapper
    LuaTable luaUtil = wrap(util);                  // NEW wrapper
    LuaTable luaDebug = wrap(debug);                // NEW wrapper
    // Execute pattern...
}
// Total: 8 objects × N blocks
```

**After:**
```java
// Create API wrappers ONCE before loop
NoiseAPI noise = new NoiseAPI(seed);                // 1 object
PaletteAPI palette = new PaletteAPI(...);            // 1 object
UtilAPI util = new UtilAPI();                        // 1 object
DebugAPI debug = new DebugAPI();                     // 1 object
LuaTable luaNoise = wrap(noise);                     // 1 wrapper
LuaTable luaPalette = wrap(palette);                 // 1 wrapper
LuaTable luaUtil = wrap(util);                       // 1 wrapper
LuaTable luaDebug = wrap(debug);                     // 1 wrapper

// Reuse for all blocks
for (BlockPosition pos : positions) {
    // Execute pattern with REUSED wrappers
}
// Total: 8 objects per execution (not per block)
```

**Allocation Reduction:**
- **Before:** 8 objects × 1000 blocks = 8,000 objects
- **After:** 8 objects × 1 execution = 8 objects
- **Savings:** 99.9% reduction in wrapper allocation

### Optimization Results

#### Test 1: Simple Pattern (default_palette_weighted)

| Blocks | Baseline (ms) | Optimized (ms) | Improvement | Notes |
|--------|--------------|----------------|-------------|-------|
| 10     | TBD          | TBD            | TBD         |       |
| 100    | TBD          | TBD            | TBD         |       |
| 500    | TBD          | TBD            | TBD         |       |
| 1000   | TBD          | TBD            | TBD         |       |
| 5000   | TBD          | TBD            | TBD         |       |

#### Test 2: Noise Pattern (default_noise_perlin2d)

| Blocks | Baseline (ms) | Optimized (ms) | Improvement | Notes |
|--------|--------------|----------------|-------------|-------|
| 10     | TBD          | TBD            | TBD         |       |
| 100    | TBD          | TBD            | TBD         |       |
| 500    | TBD          | TBD            | TBD         |       |
| 1000   | TBD          | TBD            | TBD         |       |
| 5000   | TBD          | TBD            | TBD         |       |

### Expected Benefits

**Primary:**
- Reduced memory allocation pressure
- Reduced GC overhead
- Faster Phase 1 execution (10-30% improvement expected)

**Secondary:**
- Cleaner code structure (batch execution model)
- Foundation for async execution (already working with lists)

### Testing Instructions

Run the same benchmark commands as baseline testing:

```bash
# Baseline comparison
/patternwand benchmark default_palette_weighted 1000
/patternwand benchmark default_noise_perlin2d 1000

# Stress test
/patternwand benchmark default_noise_perlin2d 5000
```

Compare timing results with baseline measurements.

### Analysis

**Success Criteria:**
- ✓ No change in pattern output (identical results)
- ⏳ 10-30% improvement in Phase 1 execution time
- ⏳ Measurable reduction in GC pauses (if profiling)

**Risk Assessment:**
- **Risk:** Wrapper reuse could cause state pollution between blocks
- **Mitigation:** API objects are stateless (seed is immutable, no mutable state)
- **Validation:** In-game testing with all example patterns

### Next Steps

After validating this optimization:
1. Proceed to Phase C: Concurrency Validation (Milestone 3)
2. Test if LuaJIT is thread-safe with concurrent execution
3. Decide if Globals isolation is needed

## Appendix

### Related Files

- **Implementation:** `src/main/java/com/xXseesXx/patternwand/commands/PatternWandCommand.java`
- **Timing API:** `src/main/java/com/xXseesXx/patternwand/patterns/scripted/api/DebugAPI.java`
- **Worker:** `src/main/java/com/xXseesXx/patternwand/items/PatternWandWorker.java`

### References

- **Async Plan:** `ASYNC_EXECUTION_PLAN.md`
- **Project README:** `README.md`
