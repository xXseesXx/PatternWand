# Execution Flow Diagrams

## Current (Synchronous) Execution

```
Player Right-Clicks Wand
         │
         ▼
┌────────────────────────┐
│   MAIN THREAD BLOCKS   │
│   (Player Frozen)      │
├────────────────────────┤
│                        │
│  Phase 1: Lua Execute  │◄── 60-80% of time
│  ├─ For each block     │    (Most expensive)
│  │   └─ Call pattern() │
│  └─ Build PlacementPlan│
│                        │
│  Phase 2: Aggregate    │◄── <1% of time
│  └─ Count materials    │
│                        │
│  Phase 3: Validate     │◄── 2-3% of time
│  └─ Check inventory    │
│                        │
│  Phase 4: Consume      │◄── 1-2% of time
│  └─ Remove from inv    │
│                        │
│  Phase 5: Place Blocks │◄── 15-30% of time
│  ├─ For each block     │
│  └─ World.setBlock()   │
│                        │
└────────────────────────┘
         │
         ▼
    Player Unfrozen
    
Timeline: ████████████████████ (2-30 seconds blocking)
```

---

## Proposed (Async Plan Generation)

```
Player Right-Clicks Wand
         │
         ▼
    Instant Return ✓
    Player Can Move
         │
         │    ┌──────────────────────────┐
         │    │   BACKGROUND THREAD      │
         │    │   (Player Not Frozen)    │
         │    ├──────────────────────────┤
         ├───►│  Phase 1: Lua Execute    │◄── 60-80% of time
         │    │  ├─ For each block       │    (Now async!)
         │    │  │   └─ Call pattern()   │
         │    │  └─ Build PlacementPlan  │
         │    │                          │
         │    │  Phase 2: Aggregate      │◄── <1% of time
         │    │  └─ Count materials      │
         │    │                          │
         │    │  Phase 3: Validate       │◄── 2-3% of time
         │    │  └─ Check inventory      │
         │    │                          │
         │    └───────────┬──────────────┘
         │                │
Player continues          │
playing normally          │
         │                │
         │                ▼
         │    Schedule Main Thread Callback
         │                │
         ▼                ▼
    ┌────────────────────────┐
    │   MAIN THREAD          │
    │   (Brief Pause)        │
    ├────────────────────────┤
    │                        │
    │  Phase 4: Consume      │◄── 1-2% of time
    │  └─ Remove from inv    │
    │                        │
    │  Phase 5: Place Blocks │◄── 15-30% of time
    │  ├─ For each block     │    (Still noticeable
    │  └─ World.setBlock()   │     on huge patterns)
    │                        │
    └────────────────────────┘
         │
         ▼
      Complete

Timeline: 
  Main Thread:   ▓░░░░░░░░░░░░░░░░░░░▓▓  (only brief pauses)
  Background:    ░████████████████████░░  (runs parallel)
  Player Impact: ▓░░░░░░░░░░░░░░░░░░░▓▓  (minimal perceived lag)
```

---

## Future (Async + Chunked Placement)

```
Player Right-Clicks Wand
         │
         ▼
    Instant Return ✓
    Player Can Move
         │
         │    ┌──────────────────────────┐
         │    │   BACKGROUND THREAD      │
         ├───►│  Phase 1-3: Plan Gen     │◄── 60-85% of time
         │    │  (Same as above)         │    (Async)
         │    └───────────┬──────────────┘
         │                │
Player continues          │
playing                   │
         │                ▼
         │    Schedule Chunked Placement
         │                │
         ▼                ▼
    ┌─────────────────────────────────────┐
    │      MAIN THREAD (Spread Over       │
    │      Multiple Ticks)                │
    ├─────────────────────────────────────┤
    │                                     │
    │  Tick 1: Phase 5a (100 blocks)     │
    │  │                                  │
    │  Tick 2: Phase 5b (100 blocks)     │
    │  │                                  │
    │  Tick 3: Phase 5c (100 blocks)     │
    │  │                                  │
    │  ...                                │
    │  │                                  │
    │  Tick N: Phase 5z (remaining)      │
    │                                     │
    └─────────────────────────────────────┘
         │
         ▼
      Complete

Timeline: 
  Main Thread:   ▓▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁  (imperceptible spikes)
  Background:    ░████████████░░░░░░░░  (runs parallel)
  Player Impact: ▓▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁  (no freeze ever!)
  
Legend:
  █ = Heavy work
  ▓ = Medium work
  ▁ = Light work
  ░ = No work
```

---

## Thread Safety Boundaries

```
┌─────────────────────────────────────────────────────────┐
│                    PLAYER ACTION                        │
│                  (Main Thread - Safe)                   │
└───────────────────────────┬─────────────────────────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │   Thread Boundary     │
                │   (Handoff Context)   │
                └───────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────────┐
│              BACKGROUND THREAD (Read-Only)                │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  ✓ Lua Execution (isolated Globals)                      │
│  ✓ Pattern Logic (pure computation)                      │
│  ✓ Material Counting (read player inventory)             │
│  ✓ Data Structure Building (PlacementPlan)               │
│                                                           │
│  ✗ NO World Access                                       │
│  ✗ NO Inventory Modification                             │
│  ✗ NO Entity Spawning                                    │
│  ✗ NO Chunk Loading                                      │
│                                                           │
└───────────────────────────┬───────────────────────────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │   Thread Boundary     │
                │ (Schedule Callback)   │
                └───────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────────┐
│           MAIN THREAD (Full World Access)                 │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  ✓ World.setBlock() (place blocks)                       │
│  ✓ Inventory Modification (consume materials)            │
│  ✓ Entity Interaction                                    │
│  ✓ Chunk Operations                                      │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

---

## Data Flow

```
┌─────────────┐
│   Player    │
│  Right-Click│
└──────┬──────┘
       │
       ▼
┌────────────────┐         ┌──────────────────┐
│ Wand Item NBT  │────────►│  PlanContext     │
│ - Pattern Name │         │  - Pattern       │
│ - Parameters   │         │  - Blocks List   │
│ - Seed         │         │  - Player Ref    │
│ - Palette      │         │  - World Context │
└────────────────┘         └────────┬─────────┘
                                    │
                        ┌───────────▼────────────┐
                        │  THREAD BOUNDARY       │
                        │  (Immutable Context)   │
                        └───────────┬────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────┐
│              Background: Generate Plan                 │
├────────────────────────────────────────────────────────┤
│                                                        │
│  For each block position:                             │
│    ├─ Execute Lua pattern()                           │
│    └─ Return palette index                            │
│                                                        │
│  Build PlacementPlan:                                  │
│    ├─ List<PlacementEntry> (pos, block, meta)         │
│    └─ Map<String, MaterialRequirement> (aggregated)   │
│                                                        │
└────────────────────────┬───────────────────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │  PlacementPlan  │
                │  (Immutable)    │
                └────────┬────────┘
                         │
                ┌────────▼─────────┐
                │ THREAD BOUNDARY  │
                │ (Pass Plan Back) │
                └────────┬─────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────┐
│              Main Thread: Execute Plan                 │
├────────────────────────────────────────────────────────┤
│                                                        │
│  For each PlacementEntry:                             │
│    ├─ Consume materials from inventory                │
│    └─ World.setBlock(pos, block, meta)                │
│                                                        │
└────────────────────────┬───────────────────────────────┘
                         │
                         ▼
                  ┌─────────────┐
                  │   Complete  │
                  │  (Success)  │
                  └─────────────┘
```

---

## Error Handling Flow

```
┌─────────────────────────────────────────────────────────┐
│                  Background Thread                      │
└─────────┬───────────────────────────────────────────────┘
          │
          ├─► Pattern Error (Lua)
          │   └─► PlanResult.error("message")
          │       └─► Main Thread: Show error chat
          │
          ├─► Missing Materials
          │   └─► PlanResult.missingMaterials(list)
          │       └─► Main Thread: Show missing items
          │
          ├─► Timeout (>30s)
          │   └─► Future.cancel()
          │       └─► Main Thread: Show timeout message
          │
          └─► Success
              └─► PlanResult.success(plan)
                  └─► Main Thread: Execute placement
                      │
                      ├─► World state changed?
                      │   └─► Validate before placement
                      │
                      ├─► Player disconnected?
                      │   └─► Abort silently
                      │
                      └─► Placement error?
                          └─► Show partial success message
```

---

## Performance Comparison

### 1,000 Block Pattern

**Synchronous (Current):**
```
Timeline: |████████████████████████████| 5.0s total
Phase 1:  |████████████████████        | 4.0s (80%)
Phase 5:  |                    ████    | 1.0s (20%)
TPS:      |▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼| 2-5 TPS
Player:   |😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱| FROZEN
```

**Asynchronous (Proposed):**
```
Timeline: |░░░░░░░░░░░░░░░░░░░░████    | 5.0s total
Phase 1:  |████████████████████░░░░    | 4.0s (background)
Phase 5:  |                    ████    | 1.0s (main thread)
TPS:      |████████████████████▼▼▼▼    | 18-20 TPS
Player:   |😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😱😱😱😱    | Brief pause
```

### 5,000 Block Pattern

**Synchronous (Current):**
```
Timeline: |████████████████████████████████████████████████| 30s
TPS:      |▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼| 1-2 TPS
Player:   |😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱| FROZEN
Server:   |⚠️ Server lag warning ⚠️                             |
```

**Asynchronous (Proposed):**
```
Timeline: |░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░████████  | 30s
Phase 1:  |████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░  | 24s (bg)
Phase 5:  |                        ████████████████████████  | 6s (main)
TPS:      |████████████████████████▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼  | 18-20 → 10-15
Player:   |😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱😱  | Better!
```

**Asynchronous + Chunked (Future):**
```
Timeline: |░░░░░░░░░░░░░░░░░░░░░░░░▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁  | 30s
Phase 1:  |████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░  | 24s (bg)
Phase 5:  |                        ▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁  | 6s (spread)
TPS:      |███████████████████████████████████████████████  | 18-20
Player:   |😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊😊| SMOOTH!
Server:   |✓ No server impact                               |
```

Legend:
- █ = Heavy load
- ▼ = TPS drop
- ░ = Background work (no player impact)
- ▁ = Light load (spread over ticks)
- 😱 = Player frozen
- 😊 = Player can play
