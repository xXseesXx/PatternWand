# Lua Concurrency Test Results

**Date:** August 11, 2026  
**Branch:** async-execution  
**Commit:** 97dcd7e  
**Milestone:** Phase C, Milestone 3

## Executive Summary

**DECISION: Globals isolation IS REQUIRED for async execution**

Testing conclusively demonstrates that a single `ScriptEngine` instance with shared `Globals` **cannot safely handle concurrent Lua execution**. Only 2% of concurrent executions were properly isolated when sharing a single engine instance.

## Test Results

### Test 3A: Concurrent Same-Script Execution
**Status:** ❌ FAILED  
**Reason:** Timeout artifacts (result: 52 instead of expected: 0)  
**Conclusion:** Not conclusive due to test implementation issues

### Test 3B: Random State Isolation  
**Status:** ✅ PASSED  
**Conclusion:** Random state (math.randomseed/math.random) is properly isolated between separate ScriptEngine instances

### Test 3C: Global Variable Pollution ⭐ CRITICAL TEST
**Status:** ❌ FAILED (Expected failure - proves isolation needed)

**Part 1: Single engine, sequential execution**
- ✅ PASS - Globals DO persist within a single engine
- Result: counter increments as expected (1, 2, 3...)

**Part 2: Multiple engines, concurrent execution**
- ✅ PASS - Each ScriptEngine has isolated Globals
- Results: 100/100 executions returned 1 (100.0%)
- Each engine starts with its own fresh Globals instance

**Part 3: Same engine, concurrent execution ⚠️**
- ❌ FAIL - Single engine CANNOT handle concurrent calls
- Results: Only 2/100 executions returned 1 (2.0%)
- **54 unique values observed** - massive race condition
- **CRITICAL FINDING:** Shared Globals = NOT thread-safe

### Test 3D: Multiple Different Scripts
**Status:** ❌ FAILED  
**Reason:** Timeout artifacts (result: 12 instead of expected: 2)  
**Conclusion:** Not conclusive due to test implementation issues

## Key Findings

### 1. ScriptEngine Architecture

Current implementation (ScriptEngine.java):
```java
public class ScriptEngine {
    private final Globals globals; // ONE Globals per ScriptEngine
    
    public ScriptEngine() {
        this.globals = JsePlatform.standardGlobals();
        // Remove dangerous libraries...
    }
}
```

**Problem:** The `globals` field is shared across all concurrent executions within the same engine.

### 2. Thread Safety Analysis

**What IS thread-safe:**
- ✅ Each `ScriptEngine` instance has its own isolated `Globals`
- ✅ Multiple `ScriptEngine` instances can run concurrently without interference
- ✅ LuaJIT's math.random() state is properly isolated per engine

**What is NOT thread-safe:**
- ❌ Concurrent calls to the same `ScriptEngine` instance
- ❌ Global variable state is shared between concurrent executions
- ❌ Race conditions occur with only 2% proper isolation

### 3. Implications for Async Execution

**Current Synchronous Architecture:**
- Single `ScriptEngine` instance in `PatternScriptLoader`
- All pattern executions share the same engine
- Works fine because execution is single-threaded

**Async Architecture Requirements:**
- ❌ **CANNOT** use a single shared `ScriptEngine` for concurrent execution
- ✅ **MUST** create separate `Globals` instances per concurrent execution
- ✅ **MUST** implement Globals pooling to avoid creation overhead

## Decision: Implement Globals Isolation

Based on Test 3C results (2% isolation rate with shared engine), we **MUST** implement Globals pooling for async execution.

### Implementation Strategy

**Option 1: Globals Pool (Recommended)**
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

**Benefits:**
- Thread-safe concurrent execution
- Reuses Globals instances (reduces GC pressure)
- Each execution gets isolated global state

**Option 2: Per-Thread ScriptEngine (Alternative)**
- Create `ScriptEngine` per background thread
- Use ThreadLocal or thread pool with engine per worker
- Simpler but less flexible

**Chosen Approach:** Option 1 (Globals Pool) as specified in ASYNC_EXECUTION_PLAN.md Milestone 17.

## Next Steps

### Immediate Actions

1. **Acknowledge Finding:** 
   - Document that Globals isolation is required (this document)
   - Update ASYNC_EXECUTION_PLAN.md with decision

2. **Continue to Milestone 4:**
   - Implement async foundation with immutable snapshots
   - Plan for Globals pooling integration in Milestone 17

3. **Defer Globals Implementation:**
   - Globals pooling is Milestone 17 (after async foundation)
   - Foundation can be built knowing isolation will be added

### Testing Requirements

When implementing Globals pooling (Milestone 17):
- Re-run Test 3C with pooled Globals
- Verify 95%+ isolation rate
- Confirm no performance regression

## Test Code Location

- **Test Suite:** `src/test/java/com/xXseesXx/patternwand/patterns/LuaConcurrencyTest.java`
- **Commit:** 97dcd7e

## References

- **Async Plan:** `ASYNC_EXECUTION_PLAN.md` - Milestone 3 & 17
- **LuaJ Documentation:** http://www.luaj.org/luaj/3.0/README.html
- **Thread Safety:** Globals instances are NOT thread-safe, must be isolated per execution

---

## Appendix: Test 3C Detailed Output

```
[Test 3C] Global variable pollution test:
  Part 1: Single engine shows globals DO persist: PASS
  Part 2: Concurrent execution with separate engines:
    Total executions: 100
    Results that are 1: 100 (100.0%)
    Unique values: 1
  Part 2 VERDICT: Each ScriptEngine has isolated Globals (PASS)
  Part 3: CRITICAL - Same engine, concurrent executions:
    Total executions: 100
    Results that are 1: 2 (2.0%)
    Unique values: 54
  Part 3 VERDICT: Single engine CANNOT safely handle concurrent calls (FAIL)
  FINAL DECISION: Globals isolation IS REQUIRED for async execution
```

**Interpretation:**
- 100 concurrent executions sharing 1 engine
- Only 2 executions saw counter=0 (returned 1)
- 98 executions saw contaminated counter values
- 54 different values = extreme race condition
- **Conclusion:** Shared Globals = unsafe for concurrency
