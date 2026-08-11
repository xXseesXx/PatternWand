# Threading Quick Start Guide

This guide walks you through implementing async Lua execution in PatternWand.

## TL;DR

**Current Problem:** Large patterns freeze the game for 2-30 seconds  
**Solution:** Move Lua execution to background thread  
**Benefit:** No freezing, 60-80% perceived lag reduction  
**Time:** ~2-4 hours implementation  
**Risk:** Low-Medium (with proper safeguards)

---

## Prerequisites

1. Read `THREADING_SUMMARY.md` for overview
2. Review `AsyncPlacementExecutor.java.example` for implementation
3. Check `THREADING_GOTCHAS.md` for edge cases

---

## Implementation Steps

### Step 1: Add Configuration (5 minutes)

**File:** `src/main/java/com/xXseesXx/patternwand/Config.java`

```java
public class Config {
    // Existing config...
    
    // Add these:
    public static boolean enableAsyncPlanning = true;
    public static int maxAsyncPlanSize = 10000;
    public static int asyncPlanTimeoutSeconds = 30;
    
    public static void synchronizeConfiguration() {
        // Load from forge config
        enableAsyncPlanning = config.getBoolean(
            "enableAsyncPlanning", 
            "async", 
            true, 
            "Enable async pattern planning (recommended)"
        );
        
        maxAsyncPlanSize = config.getInt(
            "maxAsyncPlanSize",
            "async",
            10000, 1000, 100000,
            "Maximum blocks for async planning"
        );
        
        asyncPlanTimeoutSeconds = config.getInt(
            "asyncPlanTimeoutSeconds",
            "async",
            30, 5, 120,
            "Timeout for async plan generation"
        );
    }
}
```

---

### Step 2: Create AsyncPlacementExecutor (30 minutes)

**File:** `src/main/java/com/xXseesXx/patternwand/execution/AsyncPlacementExecutor.java`

Copy from `AsyncPlacementExecutor.java.example` in project root.

**Key points:**
- Uses `Executors.newCachedThreadPool()` for background work
- Tracks active executions in `ConcurrentHashMap`
- Schedules main thread callbacks via `MinecraftServer.addScheduledTask()`

---

### Step 3: Modify PatternWandWorker (60 minutes)

**File:** `src/main/java/com/xXseesXx/patternwand/items/PatternWandWorker.java`

#### 3a. Make generatePlan() accessible

Change from `private` to package-private (or public):

```java
// Before:
private PlacementPlan generatePlan(...) { ... }

// After:
PlacementPlan generatePlan(...) { ... }
// Or:
public PlacementPlan generatePlan(...) { ... }
```

#### 3b. Extract placement execution

Split Phase 5 into separate method:

```java
/**
 * Execute a placement plan on main thread.
 * Extracted from placeBlocksWithPattern Phase 5.
 */
private ArrayList<Point3d> executePlacementPlan(
    PlacementPlan plan,
    ItemStack wandItem,
    Point3d clickedPos,
    IPlayerShim playerShim,
    int side,
    float hitX, float hitY, float hitZ) {
    
    ArrayList<Point3d> placedBlocks = new ArrayList<>();
    
    for (PlacementPlan.PlacementEntry entry : plan.getPlacements()) {
        ItemStack blockStack = new ItemStack(entry.block, 1, entry.metadata);
        
        LinkedList<Point3d> singlePos = new LinkedList<>();
        singlePos.add(entry.position);
        
        ArrayList<Point3d> placed = super.placeBlocks(
            wandItem, singlePos, entry.position, blockStack,
            playerShim, side, hitX, hitY, hitZ
        );
        
        if (!placed.isEmpty()) {
            placedBlocks.add(entry.position);
        }
    }
    
    return placedBlocks;
}
```

#### 3c. Add async wrapper method

```java
/**
 * Place blocks using async pattern execution.
 */
private void placeBlocksWithPatternAsync(
    ItemStack itemStack,
    LinkedList<Point3d> blocks,
    Point3d clickedPos,
    ItemStack sourceItems,
    IPlayerShim playerShim,
    int side,
    float hitX, float hitY, float hitZ,
    String patternName) {
    
    // Check size limit
    if (blocks.size() > Config.maxAsyncPlanSize) {
        playerShim.getPlayer().addChatMessage(
            new ChatComponentText(
                "§cPattern too large for async execution (max " + 
                Config.maxAsyncPlanSize + " blocks)"
            )
        );
        return;
    }
    
    // Create context
    AsyncPlacementExecutor.PlanContext context = 
        new AsyncPlacementExecutor.PlanContext(
            this, blocks, patternName, itemStack,
            clickedPos, playerShim, side
        );
    
    // Submit async
    AsyncPlacementExecutor.executeAsync(context, 
        new AsyncPlacementExecutor.PlacementCallback() {
            @Override
            public void onPlanComplete(
                AsyncPlacementExecutor.PlanContext ctx,
                AsyncPlacementExecutor.PlanResult result,
                EntityPlayer player) {
                
                // Handle result
                if (result.hasError()) {
                    player.addChatMessage(
                        new ChatComponentText("§c" + result.errorMessage)
                    );
                    return;
                }
                
                if (result.hasMissingMaterials()) {
                    reportMissingMaterials(result.missingMaterials, playerShim);
                    return;
                }
                
                if (result.isSuccess()) {
                    // Execute on main thread
                    ArrayList<Point3d> placed = executePlacementPlan(
                        result.plan, itemStack, clickedPos,
                        playerShim, side, hitX, hitY, hitZ
                    );
                    
                    if (!placed.isEmpty()) {
                        player.addChatMessage(
                            new ChatComponentText(
                                String.format("§aPlaced %d blocks", placed.size())
                            )
                        );
                    }
                }
            }
        }
    );
}
```

#### 3d. Modify placeBlocks() to choose sync/async

```java
@Override
public ArrayList<Point3d> placeBlocks(
    ItemStack itemStack,
    LinkedList<Point3d> blocks,
    Point3d clickedPos,
    ItemStack sourceItems,
    IPlayerShim playerShim,
    int side,
    float hitX, float hitY, float hitZ) {
    
    String activePattern = getActivePattern(itemStack);
    
    if (activePattern != null && !activePattern.isEmpty()) {
        // Check if async is enabled and pattern is large enough
        if (Config.enableAsyncPlanning && blocks.size() > 50) {
            // Use async execution
            placeBlocksWithPatternAsync(
                itemStack, blocks, clickedPos, sourceItems,
                playerShim, side, hitX, hitY, hitZ, activePattern
            );
            
            // Return empty - actual placement happens async
            return new ArrayList<>();
            
        } else {
            // Use original synchronous execution
            return placeBlocksWithPattern(
                itemStack, blocks, clickedPos, sourceItems,
                playerShim, side, hitX, hitY, hitZ, activePattern
            );
        }
    }
    
    // No pattern - use default
    return super.placeBlocks(
        itemStack, blocks, clickedPos, sourceItems,
        playerShim, side, hitX, hitY, hitZ
    );
}
```

---

### Step 4: Add Shutdown Hook (10 minutes)

**File:** `src/main/java/com/xXseesXx/patternwand/PatternWandMod.java`

```java
@Mod.EventHandler
public void serverStopping(FMLServerStoppingEvent event) {
    LOG.info("Server stopping, cleaning up async executors...");
    AsyncPlacementExecutor.shutdown();
}
```

---

### Step 5: Test Basic Functionality (30 minutes)

#### Test 1: Small Pattern (Sync)
```
1. Create 50 block pattern
2. Should execute synchronously (too small for async)
3. Verify: Immediate placement, no chat message delay
```

#### Test 2: Large Pattern (Async)
```
1. Create 1,000 block pattern
2. Should execute asynchronously
3. Verify: 
   - "Generating plan..." message appears
   - Player can move during planning
   - "Placed X blocks" appears after planning
```

#### Test 3: Error Handling
```
1. Create pattern that throws Lua error
2. Verify: Error message appears, no crash
```

#### Test 4: Missing Materials
```
1. Remove materials from inventory mid-planning
2. Verify: "Insufficient materials" message
```

---

### Step 6: Test Edge Cases (60 minutes)

Use test scenarios from `THREADING_GOTCHAS.md`:

- [ ] Player disconnect during planning
- [ ] Multiple rapid clicks
- [ ] Server shutdown during planning
- [ ] Inventory changes during planning
- [ ] Very large pattern (5,000+ blocks)

---

### Step 7: Performance Testing (30 minutes)

Before/after comparison:

```
/patternwand set default_noise_perlin2d size=30
(Generates ~30×30×30 = 27,000 blocks)

Before (sync):
- Measure: Time until player can move again
- Check: Server TPS during execution

After (async):
- Measure: Time until placement completes
- Check: Can player move during planning?
- Check: Server TPS during planning
```

Expected results:
- Player can move immediately with async
- TPS remains stable during planning
- Placement still causes brief lag (Phase 5)

---

## Quick Verification

After implementation, verify:

```bash
# 1. Check files exist
ls src/main/java/com/xXseesXx/patternwand/execution/AsyncPlacementExecutor.java

# 2. Build project
./gradlew build

# 3. Check no compilation errors
# Should succeed

# 4. In-game test
/patternwand set default_noise_perlin2d
# Place large area with wand
# Verify: "Generating plan..." message appears
# Verify: Can move during generation
```

---

## Troubleshooting

### Issue: Still freezing during execution

**Cause:** Pattern size below threshold  
**Fix:** Lower threshold in `placeBlocks()` from 50 to 10

### Issue: "Player disconnected" in logs

**Cause:** Player logged out during planning  
**Fix:** This is expected, check that no crash occurs

### Issue: Blocks not placing

**Cause:** Callback not executing  
**Fix:** Check `MinecraftServer.getServer()` is not null

### Issue: Compilation error on `generatePlan()`

**Cause:** Method is private  
**Fix:** Change visibility to package-private or public

---

## Next Steps (Optional Enhancements)

After basic async works:

1. **Add progress messages** for very large patterns
   ```java
   if (plan.size() > 1000) {
       player.addChatMessage("§7Planning 10%...");
   }
   ```

2. **Add chunked placement** for Phase 5 lag
   - See `ASYNC_USAGE_EXAMPLE.java` for implementation

3. **Add metrics/monitoring**
   ```
   /patternwand stats async
   ```

4. **Add pattern preview** system using async generation

5. **Per-player concurrent limits**
   ```java
   if (activeExecutionsForPlayer > 1) {
       return; // Limit to 1 concurrent per player
   }
   ```

---

## Rollback Plan

If async causes issues:

1. Set `enableAsyncPlanning = false` in config
2. Restart server
3. Patterns execute synchronously (original behavior)

Code supports both paths, so no risk of breaking existing functionality.

---

## Success Metrics

You've successfully implemented async if:

- ✅ Large patterns don't freeze the game
- ✅ Players receive "Generating plan..." message
- ✅ Players can move during planning
- ✅ Blocks still place correctly
- ✅ No crashes or errors in logs
- ✅ Server TPS remains stable
- ✅ Config toggle works (can disable if needed)

---

## Getting Help

If stuck, check:

1. `THREADING_INVESTIGATION.md` - Detailed explanation
2. `THREADING_GOTCHAS.md` - Edge cases and solutions
3. `EXECUTION_FLOW_DIAGRAMS.md` - Visual reference
4. Logs: Look for `AsyncPlacementExecutor` and `PatternWandWorker` entries

Common log messages:
```
[INFO] Generating plan for 1000 blocks async
[DEBUG] Player disconnected, aborting placement
[INFO] Async plan completed in 2.3s
[ERROR] Async plan generation failed: <error>
```

---

## Estimated Timeline

- Step 1 (Config): 5 min
- Step 2 (Executor): 30 min
- Step 3 (Worker): 60 min
- Step 4 (Shutdown): 10 min
- Step 5 (Basic tests): 30 min
- Step 6 (Edge cases): 60 min
- Step 7 (Performance): 30 min

**Total: ~3.5 hours** (with testing)  
**Minimum viable: ~1.5 hours** (without comprehensive testing)

Good luck! 🚀
