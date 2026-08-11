# Senior GTNH Maintainer Critique of LUA_ASYNC_IMPLEMENTATION.md

**Date:** 2026-08-11  
**Reviewer Perspective:** Senior GTNH maintainer who values simplicity, efficiency, and atomic changes  
**Overall Assessment:** Mixed - Good instincts buried under enterprise bloat

---

## Executive Summary

You're solving a real problem (Lua execution lag), but you've wrapped it in 3 phases, 6 hours of work, and abstractions that smell like someone read too many Java design pattern books.

---

## What's Actually Good ✅

### The core insight is correct:
- Lua execution is the bottleneck ✓
- Block placement is already fast ✓
- Making Lua async is the right fix ✓

### Phase 1.1 (API wrapper reuse) is solid:
- Creating 8000 objects for 1000 blocks is genuinely stupid
- Fixing this is a 30-minute change with real impact
- No architectural churn, just caching what should've been cached

### The timeout budget concept:
- Actually useful for preventing runaway patterns
- Simple addition that prevents real problems

---

## What Makes Me Want to Close This PR Immediately ❌

### 1. You're building enterprise Java for a Minecraft mod

```java
public class PatternExecutionContext {
    public final NoiseAPI noise;
    public final PaletteAPI palette;
    // ... 10 fields
    
    public PatternExecutionContext(IInventory paletteInventory, long seed, 
                                    Map<String, Object> parameterValues,
                                    PlacementContext context) {
        // 30 lines of initialization
    }
}
```

This is a **data class**. Why does it have a constructor with business logic? Why isn't it just:

```java
class PatternContext {
    final LuaTable palette, noise, util, params, context;
    final long seed;
}
```

You don't need a 50-line "execution context builder." Create the damn wrappers once and pass them around.

---

### 2. The async implementation is overcomplicated

You have:
- `AsyncPlanGenerator` (static methods, really?)
- `PlanResult` (with builder pattern)
- `ExecutionBudget` (another class)
- Custom `ThreadPoolExecutor` with named threads
- Recursive callback scheduling via `MinecraftServer.addScheduledTask(this)`

**For what?** Running Lua in a background thread.

Here's the **atomic change** version:

```java
// In PatternWandWorker
private ExecutorService luaExecutor = Executors.newFixedThreadPool(2);

public ArrayList<Point3d> placeBlocks(...) {
    if (shouldUseAsync(blocks.size())) {
        Future<Map<Point3d, Integer>> future = luaExecutor.submit(() -> {
            return generatePlan(blocks, script, context);
        });
        
        // Poll on main thread until ready
        schedulePollAndPlace(future, itemStack, playerShim);
        return new ArrayList<>(); // Placement happens later
    }
    // ... existing sync path
}
```

That's it. The rest is just error handling.

---

### 3. Phase 1.2 (Globals isolation) is solving a non-problem

You say:
> "Shared Globals means concurrent executions interfere"

But then you show:
```java
LuaValue result = script.function.invoke(...)
```

If the **function is already compiled**, it doesn't need `Globals` during execution. The function **is** the closure. This whole section is 200 lines of paranoia.

**Test it first.** If concurrent executions actually interfere, then fix it. Don't preemptively refactor based on theory.

---

### 4. The callback mechanism is a footgun

```java
MinecraftServer.getServer().addScheduledTask(new Runnable() {
    public void run() {
        if (!future.isDone()) {
            MinecraftServer.getServer().addScheduledTask(this); // Recurse
            return;
        }
        // ...
    }
});
```

This polls **every tick** until the future completes. For a 2-second Lua execution, that's 40 wasted scheduler tasks.

Why not:
```java
future.thenAcceptAsync(result -> {
    MinecraftServer.getServer().addScheduledTask(() -> {
        placePlan(result);
    });
}, commonPool());
```

Java has `CompletableFuture` for this.

---

### 5. You're breaking the existing API for no reason

Current:
```java
int executePattern(CompiledScript script, int x, int y, int z, ...)
```

Your version:
```java
int executePattern(CompiledScript script, int x, int y, int z, 
                   PatternExecutionContext context)
```

Every caller now needs to create a `PatternExecutionContext`. That's not an optimization, that's a **breaking change** disguised as one.

Keep the old signature. Create the context **inside** if needed.

---

## What You Should Actually Do

### Atomic Change #1: Cache API Wrappers (30 minutes)

```java
// Add to PatternWandWorker
private LuaTable cachedPalette, cachedNoise, cachedUtil;
private IInventory lastPaletteInventory;
private long lastSeed;

private void refreshLuaWrappers(ItemStack wand) {
    IInventory inv = getWandInventory(wand);
    long seed = getPatternSeed(wand);
    
    if (inv != lastPaletteInventory || seed != lastSeed) {
        cachedPalette = LuaPaletteWrapper.wrap(new PaletteAPI(inv, seed));
        cachedNoise = LuaNoiseWrapper.wrap(new NoiseAPI(seed));
        cachedUtil = LuaUtilWrapper.wrap(new UtilAPI());
        
        lastPaletteInventory = inv;
        lastSeed = seed;
    }
}

// In placeBlocksWithPattern()
refreshLuaWrappers(itemStack);
// Now use cachedPalette, cachedNoise, cachedUtil
```

**Ship it.** Profile it. If it helps, great. If not, you wasted 30 minutes, not 6 hours.

---

### Atomic Change #2: Add Async Toggle (2 hours)

```java
// In Config
public static boolean asyncLuaExecution = false; // Default OFF
public static int asyncThreshold = 200;

// In PatternWandWorker
if (Config.asyncLuaExecution && blocks.size() > Config.asyncThreshold) {
    executeAsync(blocks, script, itemStack, playerShim);
} else {
    executeSynchronous(blocks, script, itemStack, playerShim);
}
```

Use `CompletableFuture.supplyAsync()` and `thenAcceptAsync()`. No custom executors, no "execution contexts."

**Ship it disabled by default.** Let users opt in. Gather feedback.

---

### Atomic Change #3: Add Total Timeout (30 minutes)

```java
long start = System.currentTimeMillis();
for (Point3d pos : blocks) {
    if (System.currentTimeMillis() - start > 60000) {
        throw new TimeoutException("Pattern took > 60s");
    }
    executeLua(pos);
}
```

Done. Test it with a slow pattern. If it works, commit.

---

## The GTNH Way

### Prefer:
- Small diffs
- One feature per PR
- Disabled-by-default for risky changes
- Profiling before and after
- Keeping existing APIs stable

### Avoid:
- "Execution context" objects
- Builder patterns in hot paths
- Preemptive optimization based on theory
- Breaking changes for internal refactors
- 2000-line implementation plans

---

## My Recommendation

1. **Ship Phase 1.1 (wrapper caching) as-is** - it's good, small, safe
2. **Delete Phase 1.2 (Globals isolation)** - prove the problem exists first
3. **Rewrite Phase 2 (async) to use `CompletableFuture`** - 1/3 the code
4. **Default async to OFF** - make it opt-in until proven stable
5. **Split into 3 PRs:**
   - **PR #1:** Cache API wrappers
   - **PR #2:** Add total timeout budget
   - **PR #3:** Add async execution (disabled by default)

Each PR is reviewable in 10 minutes. Each can be reverted independently. That's how you maintain a 400-mod pack without going insane.

---

## TL;DR

You found a real problem and proposed a real solution, but you've wrapped it in so much Java ceremony that I'd reject the PR on principle. 

Strip out the abstraction layers, ship small changes, prove they work, then iterate. 

**This isn't Spring Boot.**

---

## Verdict

**Status:** Reject as-is, but accept the atomic changes individually  
**Confidence:** High - this approach has kept GTNH stable for years  
**Next Steps:** Implement the three atomic changes as separate PRs
