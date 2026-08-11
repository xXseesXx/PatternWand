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

        ScriptEngine engine = new ScriptEngine();
        CompiledScript script = engine.compile(patternScript, "test_deterministic");

        // Expected results for each seed (calculated manually)
        long seed1 = 100;
        long seed2 = 200;
        long seed3 = 300;

        int x = 10, y = 20, z = 30;

        // Calculate expected results
        int expected1 = (int) ((x + y + z + seed1) % 10); // 160 % 10 = 0
        int expected2 = (int) ((x + y + z + seed2) % 10); // 260 % 10 = 0
        int expected3 = (int) ((x + y + z + seed3) % 10); // 360 % 10 = 0

        // Create concurrent tasks
        List<Callable<Integer>> tasks = new ArrayList<>();

        tasks.add(() -> engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed1));
        tasks.add(() -> engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed2));
        tasks.add(() -> engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed3));

        // Execute concurrently
        List<Future<Integer>> futures = executor.invokeAll(tasks);

        // Verify results
        assertEquals("Thread 1 result incorrect", expected1, futures.get(0).get().intValue());
        assertEquals("Thread 2 result incorrect", expected2, futures.get(1).get().intValue());
        assertEquals("Thread 3 result incorrect", expected3, futures.get(2).get().intValue());

        // Run single-threaded for comparison
        int single1 = engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed1);
        int single2 = engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed2);
        int single3 = engine.executePattern(script, x, y, z, 0, 0, 0, mockPalette, seed3);

        // Concurrent results should match single-threaded
        assertEquals("Concurrent vs single-threaded mismatch (seed1)", single1, futures.get(0).get().intValue());
        assertEquals("Concurrent vs single-threaded mismatch (seed2)", single2, futures.get(1).get().intValue());
        assertEquals("Concurrent vs single-threaded mismatch (seed3)", single3, futures.get(2).get().intValue());
    }

    /**
     * Test 3B: Random State Isolation
     * 
     * Test pattern using math.randomseed() and math.random().
     * Run concurrently with different seeds.
     * Verify no cross-contamination of random state between threads.
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

        ScriptEngine engine = new ScriptEngine();
        CompiledScript script = engine.compile(randomPattern, "test_random");

        long seed1 = 12345;
        long seed2 = 67890;
        long seed3 = 11111;

        // First, establish expected results in single-threaded execution
        int expected1 = engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, seed1);
        int expected2 = engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, seed2);
        int expected3 = engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, seed3);

        // Now run concurrently multiple times to check for contamination
        for (int iteration = 0; iteration < 10; iteration++) {
            List<Callable<Integer>> tasks = new ArrayList<>();

            tasks.add(() -> engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, seed1));
            tasks.add(() -> engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, seed2));
            tasks.add(() -> engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, seed3));

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

        ScriptEngine engine = new ScriptEngine();
        CompiledScript script = engine.compile(globalPattern, "test_global");

        // In single-threaded execution, counter should increment predictably
        int result1 = engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, 0);
        assertEquals("First call should return 1 % 10 = 1", 1, result1);

        int result2 = engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, 0);
        assertEquals("Second call should return 2 % 10 = 2", 2, result2);

        // Now test concurrent execution
        // If globals are SHARED, we'll get unpredictable race conditions
        // If globals are ISOLATED, each thread should start with counter=0

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> {
                // Each execution should see counter=0 initially if isolated
                // Or counter=unpredictable if shared
                return engine.executePattern(script, 0, 0, 0, 0, 0, 0, mockPalette, 0);
            });
        }

        List<Future<Integer>> futures = executor.invokeAll(tasks);

        // Collect results
        List<Integer> results = new ArrayList<>();
        for (Future<Integer> future : futures) {
            results.add(future.get());
        }

        // Analysis: If globals are isolated, we should see mostly 1s (counter starting at 0)
        // If globals are shared, we'll see a wider distribution

        int countOnes = 0;
        for (int result : results) {
            if (result == 1) countOnes++;
        }

        // If more than 80% of results are 1, globals are likely isolated
        // If less than 50% are 1, globals are definitely shared
        double percentageOnes = (countOnes * 100.0) / results.size();

        System.out.println("[Test 3C] Global variable pollution test:");
        System.out.println("  Total executions: " + results.size());
        System.out.println("  Results that are 1: " + countOnes + " (" + String.format("%.1f%%", percentageOnes) + ")");
        System.out.println("  Unique values: " + results.stream()
            .distinct()
            .count());

        if (percentageOnes >= 80) {
            System.out.println("  VERDICT: Globals appear to be ISOLATED (good!)");
        } else if (percentageOnes >= 50) {
            System.out.println("  VERDICT: Globals state is AMBIGUOUS (needs investigation)");
            fail("Global state test inconclusive. Percentage of 1s: " + percentageOnes + "%");
        } else {
            System.out.println("  VERDICT: Globals are SHARED (need isolation!)");
            fail(
                "Global variables are shared between concurrent executions! "
                    + "Globals isolation is REQUIRED. Only "
                    + percentageOnes
                    + "% were isolated.");
        }
    }

    /**
     * Test 3D: Multiple Different Scripts
     * 
     * Run 3 different patterns concurrently.
     * Verify no interference between different scripts.
     * 
     * PASS CRITERIA: Each pattern executes independently with correct results.
     */
    @Test
    public void test3D_MultipleDifferentScripts() throws Exception {
        // Pattern 1: Simple constant
        String pattern1 = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    return 1\n"
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

        ScriptEngine engine = new ScriptEngine();
        CompiledScript script1 = engine.compile(pattern1, "test_constant");
        CompiledScript script2 = engine.compile(pattern2, "test_coordinates");
        CompiledScript script3 = engine.compile(pattern3, "test_seed");

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

            tasks.add(() -> engine.executePattern(script1, x, y, z, 0, 0, 0, mockPalette, seed));
            tasks.add(() -> engine.executePattern(script2, x, y, z, 0, 0, 0, mockPalette, seed));
            tasks.add(() -> engine.executePattern(script3, x, y, z, 0, 0, 0, mockPalette, seed));

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
