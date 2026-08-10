# Debug Timing System

## Overview

The PatternWand now includes a comprehensive phase-based timing system that provides detailed performance metrics for pattern execution. This replaces the old timing that only tracked overall execution.

## Features

### Phase-Based Timing
Tracks each phase of the batched execution pipeline separately:
1. **Phase 1 - Plan Generation**: All Lua script execution
2. **Phase 2 - Material Aggregation**: Counting requirements
3. **Phase 3 - Material Validation**: Inventory checks
4. **Phase 4 - Material Consumption**: Item removal
5. **Phase 5 - Block Placement**: World modification

### Lua Execution Detail
Within Phase 1, tracks:
- Total Lua execution time
- Number of Lua calls
- Average time per call (nanoseconds)

### Player Feedback
- Messages sent directly to player's chat
- Color-coded for readability
- Shows percentage breakdown by phase
- Includes console logging for server admins

## How to Use

### Enable Debug Mode
```
/patternwand debug on
```
or
```
/pw debug on
```

### Use Your Pattern
Right-click with the wand to place blocks. After placement completes, you'll see detailed timing in chat:

```
=== Pattern Execution Timing ===
Total: 45.23 ms (823 blocks placed, 823 planned)
Phase 1 (Plan Generation): 28.45 ms (62.9%)
  - Lua execution: 26.12 ms (823 calls, 31745.3 ns/call)
Phase 2 (Aggregation): 0.15 ms (0.3%)
Phase 3 (Validation): 1.23 ms (2.7%)
Phase 4 (Consumption): 0.02 ms (0.0%)
Phase 5 (Placement): 15.38 ms (34.0%)
```

### Disable Debug Mode
```
/pw debug off
```

## Understanding the Output

### Total Time
Complete operation from start to finish, including all phases.

### Phase 1 - Plan Generation
**What it measures:** Time to execute the Lua pattern for all positions
- High time here indicates complex pattern logic
- **Lua execution detail** shows average time per pattern call
- **Optimization target:** Simplify pattern math, reduce conditional logic

**Example:**
```
Phase 1 (Plan Generation): 28.45 ms (62.9%)
  - Lua execution: 26.12 ms (823 calls, 31745.3 ns/call)
```
This means:
- 28.45ms total for Phase 1
- 26.12ms was pure Lua execution
- 823 pattern evaluations
- ~32 microseconds per pattern call

### Phase 2 - Aggregation
**What it measures:** Time to count and aggregate material requirements
- Should be very fast (< 1ms for most patterns)
- Scales with number of unique block types, not total blocks
- High time here indicates a problem (very unlikely)

### Phase 3 - Validation
**What it measures:** Time to check player inventory for all required materials
- Scales with number of unique material types
- **This is where batching wins:** Old system scanned inventory per block
- Expected: ~0.1-2ms for typical patterns

**Performance win:**
- **Before batching:** 1000 blocks = ~1000 inventory scans
- **After batching:** 1000 blocks using 5 types = 5 inventory scans (**200x faster**)

### Phase 4 - Consumption
**What it measures:** Time to consume materials from inventory
- Currently minimal (parent handles consumption in Phase 5)
- Future: Will track batch consumption when implemented

### Phase 5 - Placement
**What it measures:** Time to actually place blocks in the world
- Includes world checks, physics updates, neighbor notifications
- Scales linearly with blocks placed
- Expected: 10-50ms for 1000 blocks (depends on world state)

## Performance Baselines

### Small Pattern (10-50 blocks)
```
Total: 2-5 ms
Phase 1: 60-70% (Lua dominates)
Phase 5: 20-30% (World placement)
Other phases: < 10%
```

### Medium Pattern (100-500 blocks)
```
Total: 10-30 ms
Phase 1: 50-60%
Phase 5: 35-45%
Other phases: < 5%
```

### Large Pattern (1000-5000 blocks)
```
Total: 50-250 ms
Phase 1: 40-50%
Phase 5: 45-55%
Phase 3: 1-3% (batching keeps this low!)
```

## Optimization Tips

### If Phase 1 is Slow (> 60% of total)
Your Lua pattern is expensive. Consider:
- Reduce math operations per block
- Pre-calculate values outside the pattern function
- Avoid expensive operations like `math.sqrt()` in tight loops
- Use lookup tables instead of calculations

**Example optimization:**
```lua
-- Slow: calculate distance every time
function pattern(x, y, z, ...)
    local dist = math.sqrt(x*x + y*y + z*z)
    if dist < 10 then return 0 end
    return 1
end

-- Fast: use squared distance (no sqrt)
function pattern(x, y, z, ...)
    local distSq = x*x + y*y + z*z
    if distSq < 100 then return 0 end  -- 10^2 = 100
    return 1
end
```

### If Phase 3 is Slow (> 5% of total)
Inventory checks are taking too long. This shouldn't happen with batching.
- Check if you have hundreds of different block types
- Consider using fewer material types in palette

### If Phase 5 is Slow (> 60% of total)
World placement is the bottleneck (normal for large patterns).
- This is Minecraft overhead, not much can be optimized
- Consider async placement (future feature)

## Debug Logging

All timing is also logged to console with `[PatternWand Debug]` prefix:
```
[PatternWand Debug] === Pattern Execution Timing ===
[PatternWand Debug] Total: 45.23 ms (823 blocks placed, 823 planned)
...
```

This is useful for:
- Server admins monitoring performance
- Debugging issues when player can't see chat
- Collecting timing data for benchmarks

## API for Pattern Scripts

The `debug` object passed to patterns includes a `print()` method:

```lua
function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed, params, context, debug)
    debug.print("This is a debug message!")
    debug.print("Position:", x, y, z)
    
    -- Your pattern logic
    return 0
end
```

When debug mode is enabled:
- Messages appear in player chat
- Messages appear in console
- Useful for tracing pattern behavior

## Technical Details

### Timing Mechanism
- Uses `System.nanoTime()` for high-precision timing
- Tracks start/end of each phase
- Calculates percentages relative to total time

### Message Formatting
- `§e` = Yellow (headers)
- `§a` = Green (success/totals)
- `§7` = Gray (details)

### Thread Safety
The timing system uses static fields and is not thread-safe. This is acceptable because:
- Pattern execution is single-threaded per player
- Minecraft's game loop is single-threaded
- Different players timing simultaneously won't corrupt data (just mix results)

If multiple players use patterns simultaneously, timing will be inaccurate. This is a debug feature, not production monitoring.

## Future Enhancements

### Planned Features
1. **Async execution timing**: Track tick-by-tick placement
2. **AE2 integration timing**: Track network query time
3. **Histogram mode**: Collect timing data over multiple operations
4. **Per-block-type breakdown**: Show which materials are slow to place

### Export to File
Future: `/pw debug export` to save timing data to CSV for analysis

## Troubleshooting

### Timing not showing in chat
- Verify debug mode is on: `/pw debug on`
- Check console logs for `[PatternWand Debug]` messages
- Ensure pattern is actually executing (not failing early)

### Percentages don't add to 100%
- Rounding errors (normal)
- Overhead between phases (not tracked separately)

### Phase times are negative or invalid
- Bug! Report with:
  - Pattern used
  - Block count
  - Console logs

## Example Session

```
Player: /pw debug on
Server: Debug mode enabled

Player: /pw set default_fill
Server: Active pattern set to: default_fill

[Player right-clicks with wand, places 250 blocks]

Chat Output:
=== Pattern Execution Timing ===
Total: 18.45 ms (250 blocks placed, 250 planned)
Phase 1 (Plan Generation): 10.23 ms (55.4%)
  - Lua execution: 9.87 ms (250 calls, 39480.0 ns/call)
Phase 2 (Aggregation): 0.08 ms (0.4%)
Phase 3 (Validation): 0.45 ms (2.4%)
Phase 4 (Consumption): 0.01 ms (0.1%)
Phase 5 (Placement): 7.68 ms (41.6%)
```

## Summary

The new debug timing system provides actionable insights into pattern performance:
- **Identify bottlenecks** by phase
- **Verify batching benefits** (Phase 3 should be < 5%)
- **Optimize patterns** based on Phase 1 breakdown
- **Track placement overhead** in Phase 5

Enable it, use your patterns, and see exactly where time is spent! 🚀
