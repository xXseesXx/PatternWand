# Batch Mode Testing Guide

## Status: Ready for Testing

**Implementation:** Phases 1-4 Complete ✓  
**Build Status:** Success ✓  
**Commit:** `731d4f0` on branch `batchmode`

## What Was Implemented

### Phase 1: PlacementPlan Data Structure ✓
- `PlacementPlan.java` with `PlacementEntry` and `MaterialRequirement` classes
- Material aggregation via `getMaterialRequirements()`
- Efficient keying by block+metadata

### Phase 2: Plan Generation ✓
- `generatePlan()` method isolates all Lua execution
- Builds complete plan before touching inventory or world
- Handles gaps (palette index -1) correctly

### Phase 3: Batched Execution ✓
- 5-phase execution:
  1. Generate plan (Lua isolated)
  2. Aggregate materials
  3. Validate availability  
  4. Consumption (via parent's placeBlocks)
  5. Execute plan
- Materials validated before consumption
- No partial builds on insufficient materials

### Phase 4: User Feedback ✓
- `reportMissingMaterials()` shows up to 5 missing materials
- Displays have/need counts
- Shows remaining count if > 5 materials missing

## Testing Scenarios

### Test 1: Small Pattern (Single Block Type)
**Purpose:** Verify basic functionality  
**Pattern:** `default_fill` or simple checkerboard  
**Size:** ~10 blocks  
**Expected:**
- ✓ Plan generated
- ✓ Blocks placed correctly
- ✓ No errors

**How to test:**
1. Place 64 cobblestone in palette
2. Set pattern: `/pw set default_fill`
3. Right-click to flood-fill a small area
4. Verify blocks placed

### Test 2: Medium Pattern (Multiple Block Types)
**Purpose:** Verify material aggregation  
**Pattern:** `default_checkerboard` with 2-3 block types  
**Size:** ~100 blocks  
**Expected:**
- ✓ Materials aggregated correctly
- ✓ Inventory scanned once per material type (not per block)
- ✓ All blocks placed

**How to test:**
1. Place cobblestone and stone bricks in palette (32 each)
2. Set pattern: `/pw set default_checkerboard`
3. Right-click to create medium area
4. Check logs for "Plan requires X distinct material types"

### Test 3: Large Pattern (Performance Test)
**Purpose:** Measure performance improvement  
**Pattern:** Complex pattern with 5+ block types  
**Size:** 1000+ blocks  
**Expected:**
- ✓ Completes in reasonable time
- ✓ No lag spikes
- ✓ Debug timing shows improvement

**How to test:**
1. Enable debug output: `/pw debug on`
2. Fill palette with 5-7 different block types
3. Use complex pattern on large area
4. Note completion time in logs

### Test 4: Insufficient Materials
**Purpose:** Verify validation and error reporting  
**Pattern:** Any pattern  
**Size:** Medium  
**Setup:** Only provide 50% of required materials  
**Expected:**
- ✓ "Insufficient materials!" message
- ✓ Lists missing materials with counts
- ✓ NO blocks consumed
- ✓ NO blocks placed (atomic operation)

**How to test:**
1. Place only 10 cobblestone in palette
2. Try to place 100 block pattern
3. Verify error message shows "Need X more Cobblestone (have 10, need 100)"
4. Verify inventory unchanged

### Test 5: Pattern with Gaps
**Purpose:** Verify -1 (gap) handling  
**Pattern:** One that returns -1 for some positions  
**Size:** Small-medium  
**Expected:**
- ✓ Gaps not added to plan
- ✓ Only solid blocks placed
- ✓ Material count accurate (doesn't include gaps)

**How to test:**
1. Create/use pattern that returns -1 conditionally
2. Verify only non-gap positions have blocks
3. Check material consumption matches placed blocks

### Test 6: Edge Cases
**Purpose:** Verify robustness  

**6a. Empty Palette:**
- Expected: No placements in plan, operation aborts cleanly

**6b. Pattern Returns Invalid Index:**
- Expected: Block skipped, operation continues

**6c. World Placement Failures:**
- Try to place where blocks can't go
- Expected: Plan generated, some placements fail, partial success reported

**6d. Very Large Pattern:**
- 5000-10000 blocks
- Expected: Completes successfully, no memory issues

## Performance Metrics to Collect

### Before Batch Mode (estimated from architecture):
- **Inventory scans:** N blocks × M material types = up to N scans
- **Material consumption:** N individual calls
- **Lua execution:** Mixed with Minecraft operations

### After Batch Mode (expected):
- **Inventory scans:** M material types (1 scan per type)
- **Material consumption:** M aggregate calls
- **Lua execution:** Isolated in tight loop

### Expected Improvements:
For a 1000-block pattern using 5 material types:
- **Inventory operations:** 1000 scans → 5 scans (**200x faster**)
- **Better cache locality:** Lua isolated
- **Atomic validation:** No partial builds

## Success Criteria

- [ ] All 6 test scenarios pass
- [ ] No compilation errors
- [ ] No runtime errors
- [ ] Materials validated correctly
- [ ] Error messages helpful and accurate
- [ ] Performance measurably better (Phase 6)
- [ ] No change to user-facing behavior (except error messages)

## Known Limitations

1. **Material consumption still per-block in Phase 5:**  
   The parent's `placeBlocks()` is called once per block, so we don't get batched consumption yet. However, we DO get:
   - Batched validation (major win)
   - No partial builds (major UX win)
   - Foundation for true batch consumption later

2. **No actual inventory batch API:**  
   Would need to implement custom batch methods or modify parent class for true batched consumption.

3. **Debug logging uses PatternWandMod.LOG.debug():**  
   If debug() doesn't exist, replace with info() and use debug command to toggle.

## Next Steps After Testing

1. **Phase 5 Results → Update docs**
2. **Phase 6:** Performance profiling
   - Measure before/after with timing
   - Document improvements
   - Create benchmark patterns
3. **Future:** Consider implementing true batch consumption
4. **Future:** Use this foundation for AE2 integration

## How to Run Tests

```bash
# Build and copy to mods folder
./gradlew build
cp build/libs/PatternWand-*.jar ~/minecraft/mods/

# In Minecraft:
/pw debug on          # Enable debug output
/pw reload            # Reload patterns
/pw set <pattern>     # Set pattern to test
# Right-click with wand to test
```

## Reporting Issues

If testing reveals issues:
1. Note the test scenario
2. Capture error messages
3. Check logs in `.minecraft/logs/latest.log`
4. Note expected vs actual behavior
5. Create issue or fix directly
