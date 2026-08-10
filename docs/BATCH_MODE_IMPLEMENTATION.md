# Batch Mode Implementation Plan

## Overview

Refactor PatternWand's block placement system to use batched execution. This separates pattern generation (Lua execution) from material consumption and world modification, resulting in massive performance improvements and better error handling.

## Motivation

### Current Architecture Problems
```java
for each position:
  execute Lua pattern → get block type
  check player inventory (scan all slots)
  consume item
  place block in world
```

For a 10,000 block pattern:
- 10,000 Lua VM invocations with context switching
- 10,000 inventory scans (expensive)
- 10,000 material consumption calls
- No validation before consumption (partial builds on failure)

### Batched Architecture Benefits
```
Phase 1: Generate plan (all Lua execution in tight loop)
Phase 2: Aggregate materials (count requirements)
Phase 3: Validate availability (check once per material type)
Phase 4: Consume materials (batch consumption)
Phase 5: Execute plan (place blocks)
```

For a 10,000 block pattern using 5 block types:
- 10,000 Lua calls (unavoidable, but isolated)
- 5 inventory scans instead of 10,000 (**2000x improvement**)
- 5 consumption calls instead of 10,000 (**2000x improvement**)
- Validation before consumption (no partial builds)

**Estimated Performance Gain:** 50-200% for typical patterns

## Implementation Phases

### Phase 1: Create PlacementPlan Data Structure
**Time:** ~30 minutes  
**Risk:** Low

Create `PlacementPlan.java` to represent the complete placement operation:
- List of `PlacementEntry` (position + block + metadata)
- Method to aggregate material requirements
- Simple, immutable data structure

**Files:**
- `src/main/java/com/xXseesXx/patternwand/patterns/PlacementPlan.java` (new)

### Phase 2: Add Plan Generation Method
**Time:** ~45 minutes  
**Risk:** Low

Extract Lua execution logic into `generatePlan()` method:
- Execute pattern for all positions
- Collect results into PlacementPlan
- No material consumption or world modification
- Pure pattern → plan transformation

**Files:**
- `src/main/java/com/xXseesXx/patternwand/items/PatternWandWorker.java` (modify)

### Phase 3: Rewrite placeBlocksWithPattern() with Batching
**Time:** ~60 minutes  
**Risk:** Medium

Replace the existing per-block loop with batched execution:
1. Generate plan (Phase 1: all Lua)
2. Aggregate requirements (Phase 2: count materials)
3. Validate availability (Phase 3: check inventory)
4. Report missing materials and abort if insufficient
5. Consume materials in batch (Phase 4: batch consumption)
6. Execute plan (Phase 5: place blocks)

**Files:**
- `src/main/java/com/xXseesXx/patternwand/items/PatternWandWorker.java` (modify)

### Phase 4: Add User Feedback
**Time:** ~15 minutes  
**Risk:** Low

Add helpful error messages:
- List missing materials with quantities
- Show available vs required
- Success message with placement stats

**Files:**
- `src/main/java/com/xXseesXx/patternwand/items/PatternWandWorker.java` (modify)

### Phase 5: Testing & Validation
**Time:** ~60 minutes  
**Risk:** Low

Test scenarios:
- Small pattern (10 blocks, single type)
- Medium pattern (100 blocks, 3 types)
- Large pattern (1000+ blocks, 5+ types)
- Insufficient materials (validate error reporting)
- Pattern with gaps (validate -1 handling)
- Edge cases (empty palette, invalid pattern)

### Phase 6: Performance Profiling
**Time:** ~30 minutes  
**Risk:** Low

Compare performance before/after:
- Enable debug timing
- Test with 5000-10000 block patterns
- Measure inventory operation reduction
- Document improvements

## Architecture Diagram

```
┌─────────────────────────────────────────┐
│   placeBlocksWithPattern()              │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────┐
│ PHASE 1: Generate Plan                     │
│ - Execute Lua for all positions            │
│ - Record (position, block, metadata)       │
│ - No inventory interaction                 │
│ - No world modification                    │
└────────────────┬───────────────────────────┘
                 │
                 ▼
         ┌─────────────┐
         │PlacementPlan│
         └──────┬──────┘
                │
                ▼
┌────────────────────────────────────────────┐
│ PHASE 2: Aggregate Materials               │
│ - Count unique block types                 │
│ - Sum quantities needed                    │
│ - Map<BlockKey, Quantity>                  │
└────────────────┬───────────────────────────┘
                 │
                 ▼
      ┌──────────────────┐
      │MaterialRequirement│
      └─────────┬─────────┘
                │
                ▼
┌────────────────────────────────────────────┐
│ PHASE 3: Validate Availability             │
│ - Check player inventory once per type     │
│ - Collect missing materials                │
└────────────────┬───────────────────────────┘
                 │
         ┌───────┴────────┐
         │                │
         ▼                ▼
    Sufficient      Insufficient
         │                │
         │                └──> Report & Abort
         ▼
┌────────────────────────────────────────────┐
│ PHASE 4: Consume Materials                 │
│ - Batch consume per material type          │
│ - No world interaction yet                 │
└────────────────┬───────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────┐
│ PHASE 5: Execute Plan                      │
│ - Place blocks from plan                   │
│ - Use parent's placement logic             │
│ - Track successful placements              │
└────────────────┬───────────────────────────┘
                 │
                 ▼
         ┌──────────────┐
         │ArrayList<Pos>│ (placed blocks)
         └──────────────┘
```

## Future Enhancements Enabled

This architecture naturally supports:

### AE2 Integration (Future)
```java
// Phase 3: Check multiple sources
int inPlayer = playerShim.countItems(...);
int inAE2 = ae2Provider.countItems(...);
int total = inPlayer + inAE2;

// Phase 4: Consume from multiple sources
int fromPlayer = Math.min(needed, inPlayer);
int fromAE2 = needed - fromPlayer;
```

### Undo/Redo (Future)
```java
// Store plan for reversal
UndoStack.push(plan);

// Undo: reverse the plan
plan.reverse().execute();
```

### Preview Mode (Future)
```java
PlacementPlan plan = generatePlan(...);
renderPreview(plan);  // Show bounding box
// Wait for player confirmation
plan.execute();       // Execute when confirmed
```

### Async Execution (Future)
```java
// Generate plan on tick 1
PlacementPlan plan = generatePlan(...);

// Execute gradually over ticks 2-N
for (int i = 0; i < 100 && hasMore; i++) {
    placeNextBlock();
}
```

## Success Criteria

- [ ] All tests pass
- [ ] No change to user-facing behavior (except error messages)
- [ ] Performance improvement measurable with debug timing
- [ ] No partial builds on insufficient materials
- [ ] Clean separation between phases
- [ ] Code is more maintainable than before

## Estimated Total Time

**Implementation:** 3-4 hours  
**Testing:** 1 hour  
**Total:** 4-5 hours

## Risk Mitigation

- Keep old implementation as reference during development
- Test incrementally after each phase
- Use existing parent placement logic (minimize changes)
- Maintain backward compatibility with pattern scripts
