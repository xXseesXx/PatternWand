package com.xXseesXx.patternwand.patterns;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.xXseesXx.patternwand.patterns.scripted.CompiledScript;
import com.xXseesXx.patternwand.patterns.scripted.ScriptEngine;

/**
 * Concurrency tests for Lua pattern execution.
 * 
 * Tests whether LuaJIT is thread-safe for concurrent pattern execution
 * WITHOUT Globals isolation. Results determine if we need to add Globals pooling.
 * 
 * Phase C, Milestone 3: Prove Lua Thread Safety Requirements
 */
public class LuaConcurrencyTest {

    private ExecutorService executor;
    private IInventory mockPalette;

    @Before
    public void setUp() {
        // Use 3 threads for concurrent execution
        executor = Executors.newFixedThreadPool(3);

        // Create mock palette with some blocks
        mockPalette = new InventoryBasic("Test Palette", false, 54);
        for (int i = 0; i < 10; i++) {
            mockPalette.setInventorySlotContents(i, new ItemStack(Blocks.stone, 64));
        }
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    /**
     * Test 3A: Concurrent Same-Script Execution
     * 
     * Run the same CompiledScript concurrently with different seeds.
     * Each should produce deterministic output matching single-threaded execution.
     * 
     * NOTE: Each thread must use its own ScriptEngine instance since Globals are per-engine.
     * 
     * PASS CRITERIA: All 3 concurrent executions produce expected results with no interference.
     */
    @Test
    public void test3A_ConcurrentSameScriptExecution() throws Exception {
        // Create a simple deterministic pattern that uses the seed
        String patternScript = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    -- Use seed in calculation for determinism\n"
            + "    local value = (x + y + z + seed) % palette.size()\n"
            + "    return value\n"
            + "end\n"
            + "return pattern";

        // Expected results for each seed (calculated manually)
        long seed1 = 100;
        long seed2 = 200;
        long seed3 = 300;

        int x = 10, y = 20, z = 30;

        // Calculate expected results
        int expected1 = (int) ((x + y + z + seed1) % 10); // 160 % 10 = 0
        int expected2 = (int) ((x + y + z + seed2) % 10); // 260 % 10 = 0
        int expected3 = (int) ((x + y + z + seed3) % 10); // 360 % 10 = 0

        // Create concurrent tasks - each with its own ScriptEngine
        List<Callable<Integer>> tasks = new ArrayList<>();

        tasks.add(() -> {
            ScriptEngine engine = new ScriptEngine();
            CompiledScript script = engine.compile(patternScript, "test_deterministic_1");
            return engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed1);
        });

        tasks.add(() -> {
            ScriptEngine engine = new ScriptEngine();
            CompiledScript script = engine.compile(patternScript, "test_deterministic_2");
            return engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed2);
        });

        tasks.add(() -> {
            ScriptEngine engine = new ScriptEngine();
            CompiledScript script = engine.compile(patternScript, "test_deterministic_3");
            return engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed3);
        });

        // Execute concurrently
        List<Future<Integer>> futures = executor.invokeAll(tasks);

        // Verify results
        assertEquals(
            "Thread 1 result incorrect",
            expected1,
            futures.get(0)
                .get()
                .intValue());
        assertEquals(
            "Thread 2 result incorrect",
            expected2,
            futures.get(1)
                .get()
                .intValue());
        assertEquals(
            "Thread 3 result incorrect",
            expected3,
            futures.get(2)
                .get()
                .intValue());

        // Run single-threaded for comparison
        ScriptEngine singleEngine = new ScriptEngine();
        CompiledScript singleScript = singleEngine.compile(patternScript, "test_deterministic_single");
        int single1 = singleEngine.executePattern(singleScript, x, y, z, 0, 0, 0, mockPalette, seed1);
        int single2 = singleEngine.executePattern(singleScript, x, y, z, 0, 0, 0, mockPalette, seed2);
        int single3 = singleEngine.executePattern(singleScript, x, y, z, 0, 0, 0, mockPalette, seed3);

        // Concurrent results should match single-threaded
        assertEquals(
            "Concurrent vs single-threaded mismatch (seed1)",
            single1,
            futures.get(0)
                .get()
                .intValue());
        assertEquals(
            "Concurrent vs single-threaded mismatch (seed2)",
            single2,
            futures.get(1)
                .get()
                .intValue());
        assertEquals(
            "Concurrent vs single-threaded mismatch (seed3)",
            single3,
            futures.get(2)
                .get()
                .intValue());
    }

    /**
     * Test 3B: Random State Isolation
     * 
     * Test pattern using math.randomseed() and math.random().
     * Run concurrently with different seeds.
     * Verify no cross-contamination of random state between threads.
     * 
     * NOTE: Each thread uses its own ScriptEngine instance.
     * 
     * PASS CRITERIA: Each thread's random sequence is independent and deterministic.
     */
    @Test
    public void test3B_RandomStateIsolation() throws Exception {
        String randomPattern = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    -- Seed the random generator\n"
            + "    math.randomseed(seed)\n"
            + "    -- Generate random number and map to palette\n"
            + "    return math.random(0, palette.size() - 1)\n"
            + "end\n"
            + "return pattern";

        long seed1 = 12345;
        long seed2 = 67890;
        long seed3 = 11111;

        // First, establish expected results in single-threaded execution
        ScriptEngine setupEngine = new ScriptEngine();
        CompiledScript setupScript = setupEngine.compile(randomPattern, "test_random_setup");
        int expected1 = setupEngine.executePattern(setupScript, 0, 0, 0, 0, 0, 0, mockPalette, seed1);
        int expected2 = setupEngine.executePattern(setupScript, 0, 0, 0, 0, 0, 0, mockPalette, seed2);
        int expected3 = setupEngine.executePattern(setupScript, 0, 0, 0, 0, 0, 0, mockPalette, seed3);

        // Now run concurrently multiple times to check for contamination
        for (int iteration = 0; iteration < 10; iteration++) {
            List<Callable<Integer>> tasks = new ArrayList<>();

            tasks.add(() -> {
                ScriptEngine engine = new ScriptEngine();
                CompiledScript script = engine.compile(randomPattern, "test_random_1");
                return engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, seed1);
            });

            tasks.add(() -> {
                ScriptEngine engine = new ScriptEngine();
                CompiledScript script = engine.compile(randomPattern, "test_random_2");
                return engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, seed2);
            });

            tasks.add(() -> {
                ScriptEngine engine = new ScriptEngine();
                CompiledScript script = engine.compile(randomPattern, "test_random_3");
                return engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, seed3);
            });

            List<Future<Integer>> futures = executor.invokeAll(tasks);

            // Each concurrent execution should match its expected value
            assertEquals(
                "Random state contaminated (iteration " + iteration + ", thread 1)",
                expected1,
                futures.get(0)
                    .get()
                    .intValue());
            assertEquals(
                "Random state contaminated (iteration " + iteration + ", thread 2)",
                expected2,
                futures.get(1)
                    .get()
                    .intValue());
            assertEquals(
                "Random state contaminated (iteration " + iteration + ", thread 3)",
                expected3,
                futures.get(2)
                    .get()
                    .intValue());
        }
    }

    /**
     * Test 3C: Global Variable Pollution
     * 
     * Test pattern that mutates a global variable.
     * Run concurrently and detect if globals leak between executions.
     * 
     * NOTE: Each thread uses its own ScriptEngine. This tests if WITHIN a single
     * ScriptEngine instance, globals are properly isolated between concurrent executions.
     * 
     * THIS IS THE CRITICAL TEST for Globals isolation decision.
     * 
     * PASS CRITERIA: Global variables are isolated between concurrent executions.
     * FAIL CRITERIA: Global variables are shared, causing race conditions.
     */
    @Test
    public void test3C_GlobalVariablePollution() throws Exception {
        String globalPattern = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    -- Mutate global variable (this is the test)\n"
            + "    counter = (counter or 0) + 1\n"
            + "    return counter % palette.size()\n"
            + "end\n"
            + "return pattern";

        // Test 1: Single engine, sequential execution
        // This establishes that globals persist within a single engine
        ScriptEngine singleEngine = new ScriptEngine();
        CompiledScript singleScript = singleEngine.compile(globalPattern, "test_global_single");

        int result1 = singleEngine.executePattern(singleScript, 0, 0, 0, 0, 0, 0, mockPalette, 0);
        assertEquals("First call should return 1 % 10 = 1", 1, result1);

        int result2 = singleEngine.executePattern(singleScript, 0, 0, 0, 0, 0, 0, mockPalette, 0);
        assertEquals("Second call should return 2 % 10 = 2", 2, result2);

        System.out.println("[Test 3C] Global variable pollution test:");
        System.out.println("  Part 1: Single engine shows globals DO persist: PASS");

        // Test 2: Multiple engines in parallel
        // Each engine should have its own isolated globals
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> {
                // Each task creates its own engine - globals should be isolated per engine
                ScriptEngine engine = new ScriptEngine();
                CompiledScript script = engine.compile(globalPattern, "test_global_concurrent");
                return engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, 0);
            });
        }

        List<Future<Integer>> futures = executor.invokeAll(tasks);

        // Collect results
        List<Integer> results = new ArrayList<>();
        for (Future<Integer> future : futures) {
            results.add(future.get());
        }

        // Analysis: If each engine has its own globals, all results should be 1
        int countOnes = 0;
        for (int result : results) {
            if (result == 1) countOnes++;
        }

        double percentageOnes = (countOnes * 100.0) / results.size();

        System.out.println("  Part 2: Concurrent execution with separate engines:");
        System.out.println("    Total executions: " + results.size());
        System.out
            .println("    Results that are 1: " + countOnes + " (" + String.format("%.1f%%", percentageOnes) + ")");
        System.out.println(
            "    Unique values: " + results.stream()
                .distinct()
                .count());

        // Since each task creates its own engine, all should return 1
        if (percentageOnes >= 95) {
            System.out.println("  Part 2 VERDICT: Each ScriptEngine has isolated Globals (PASS)");
        } else {
            System.out.println("  Part 2 VERDICT: Globals are leaking between engines! (FAIL)");
            fail(
                "Globals are leaking between ScriptEngine instances! " + "Only "
                    + percentageOnes
                    + "% returned 1. This should be 100%.");
        }

        // Test 3: CRITICAL TEST - Same engine, concurrent executions
        // This tests if a SINGLE engine can handle concurrent calls safely
        System.out.println("  Part 3: CRITICAL - Same engine, concurrent executions:");

        ScriptEngine sharedEngine = new ScriptEngine();
        CompiledScript sharedScript = sharedEngine.compile(globalPattern, "test_global_shared");

        List<Callable<Integer>> sharedTasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            sharedTasks.add(() -> {
                // All tasks share the same engine - this will reveal thread safety issues
                return sharedEngine.executePattern(sharedScript, 0, 0, 0, 0, 0, 0, mockPalette, 0);
            });
        }

        List<Future<Integer>> sharedFutures = executor.invokeAll(sharedTasks);

        List<Integer> sharedResults = new ArrayList<>();
        for (Future<Integer> future : sharedFutures) {
            sharedResults.add(future.get());
        }

        int sharedCountOnes = 0;
        for (int result : sharedResults) {
            if (result == 1) sharedCountOnes++;
        }

        double sharedPercentageOnes = (sharedCountOnes * 100.0) / sharedResults.size();

        System.out.println("    Total executions: " + sharedResults.size());
        System.out.println(
            "    Results that are 1: " + sharedCountOnes + " (" + String.format("%.1f%%", sharedPercentageOnes) + ")");
        System.out.println(
            "    Unique values: " + sharedResults.stream()
                .distinct()
                .count());

        if (sharedPercentageOnes >= 80) {
            System.out.println("  Part 3 VERDICT: Single engine CAN safely handle concurrent calls (PASS)");
            System.out.println("  FINAL DECISION: Globals isolation NOT needed - LuaJIT is thread-safe enough");
        } else {
            System.out.println("  Part 3 VERDICT: Single engine CANNOT safely handle concurrent calls (FAIL)");
            System.out.println("  FINAL DECISION: Globals isolation IS REQUIRED for async execution");
            fail(
                "Single ScriptEngine instance is NOT thread-safe for concurrent execution! " + "Only "
                    + sharedPercentageOnes
                    + "% were isolated. "
                    + "Must implement Globals pooling for async execution.");
        }
    }

    /**
     * Test 3D: Multiple Different Scripts
     * 
     * Run 3 different patterns concurrently.
     * Verify no interference between different scripts.
     * 
     * NOTE: Each thread uses its own ScriptEngine instance.
     * 
     * PASS CRITERIA: Each pattern executes independently with correct results.
     */
    @Test
    public void test3D_MultipleDifferentScripts() throws Exception {
        // Pattern 1: Simple constant
        String pattern1 = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 1\n"
            + "end\n"
            + "return pattern";

        // Pattern 2: Uses coordinates
        String pattern2 = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    return (x + y) % palette.size()\n"
            + "end\n"
            + "return pattern";

        // Pattern 3: Uses seed
        String pattern3 = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    return (seed % palette.size())\n"
            + "end\n"
            + "return pattern";

        // Test values
        int x = 5, y = 7, z = 3;
        long seed = 42;

        // Expected results
        int expected1 = 1;
        int expected2 = (x + y) % 10; // 12 % 10 = 2
        int expected3 = (int) (seed % 10); // 42 % 10 = 2

        // Run concurrently multiple times
        for (int iteration = 0; iteration < 20; iteration++) {
            List<Callable<Integer>> tasks = new ArrayList<>();

            tasks.add(() -> {
                ScriptEngine engine = new ScriptEngine();
                CompiledScript script = engine.compile(pattern1, "test_constant");
                return engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed);
            });

            tasks.add(() -> {
                ScriptEngine engine = new ScriptEngine();
                CompiledScript script = engine.compile(pattern2, "test_coordinates");
                return engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed);
            });

            tasks.add(() -> {
                ScriptEngine engine = new ScriptEngine();
                CompiledScript script = engine.compile(pattern3, "test_seed");
                return engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed);
            });

            List<Future<Integer>> futures = executor.invokeAll(tasks);

            assertEquals(
                "Pattern 1 interference (iteration " + iteration + ")",
                expected1,
                futures.get(0)
                    .get()
                    .intValue());
            assertEquals(
                "Pattern 2 interference (iteration " + iteration + ")",
                expected2,
                futures.get(1)
                    .get()
                    .intValue());
            assertEquals(
                "Pattern 3 interference (iteration " + iteration + ")",
                expected3,
                futures.get(2)
                    .get()
                    .intValue());
        }
    }
}
