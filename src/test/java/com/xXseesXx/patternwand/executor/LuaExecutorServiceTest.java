package com.xXseesXx.patternwand.executor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.LuaValue;

import com.xXseesXx.patternwand.patterns.PatternExecutionSnapshot;
import com.xXseesXx.patternwand.patterns.PlacementPlan;
import com.xXseesXx.patternwand.patterns.scripted.CompiledScript;
import com.xXseesXx.patternwand.patterns.scripted.PlacementContext;

/**
 * Tests for LuaExecutorService.
 * 
 * Verifies:
 * - Executor initialization and lifecycle
 * - Job submission and execution
 * - Graceful shutdown
 * - Thread naming and configuration
 */
public class LuaExecutorServiceTest {

    private LuaExecutorService executor;

    @Before
    public void setUp() {
        executor = new LuaExecutorService(2); // Use 2 threads for testing
    }

    @After
    public void tearDown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    @Test
    public void testInitialization() {
        assertEquals("Thread count should match constructor argument", 2, executor.getThreadCount());
        assertFalse("Executor should not be shut down initially", executor.isShutdown());
    }

    @Test
    public void testDefaultThreadCount() {
        LuaExecutorService defaultExecutor = new LuaExecutorService();
        assertEquals("Default thread count should be 2", 2, defaultExecutor.getThreadCount());
        defaultExecutor.shutdown();
    }

    @Test
    public void testInvalidThreadCount() {
        try {
            new LuaExecutorService(0);
            fail("Should throw IllegalArgumentException for thread count 0");
        } catch (IllegalArgumentException e) {
            assertTrue(
                e.getMessage()
                    .contains("at least 1"));
        }

        try {
            new LuaExecutorService(-1);
            fail("Should throw IllegalArgumentException for negative thread count");
        } catch (IllegalArgumentException e) {
            assertTrue(
                e.getMessage()
                    .contains("at least 1"));
        }
    }

    @Test
    public void testSubmitPlanGeneration() throws Exception {
        PatternExecutionSnapshot snapshot = createMockSnapshot("test_pattern", 10);

        Future<PlacementPlan> future = executor.submitPlanGeneration(snapshot);

        assertNotNull("Future should not be null", future);

        // Wait for result (should complete quickly since executePlan is a placeholder)
        PlacementPlan plan = future.get(5, TimeUnit.SECONDS);

        assertNotNull("Plan should not be null", plan);
        assertTrue("Future should be done", future.isDone());
        assertFalse("Future should not be cancelled", future.isCancelled());
    }

    @Test
    public void testSubmitNullSnapshot() {
        try {
            executor.submitPlanGeneration(null);
            fail("Should throw IllegalArgumentException for null snapshot");
        } catch (IllegalArgumentException e) {
            assertTrue(
                e.getMessage()
                    .contains("snapshot cannot be null"));
        }
    }

    @Test
    public void testSubmitMultipleJobs() throws Exception {
        // Submit multiple jobs concurrently
        List<Future<PlacementPlan>> futures = new ArrayList<Future<PlacementPlan>>();

        for (int i = 0; i < 5; i++) {
            PatternExecutionSnapshot snapshot = createMockSnapshot("pattern_" + i, 10 + i);
            futures.add(executor.submitPlanGeneration(snapshot));
        }

        // Wait for all to complete
        for (int i = 0; i < futures.size(); i++) {
            Future<PlacementPlan> future = futures.get(i);
            PlacementPlan plan = future.get(5, TimeUnit.SECONDS);
            assertNotNull("Plan " + i + " should not be null", plan);
            assertTrue("Future " + i + " should be done", future.isDone());
        }
    }

    @Test
    public void testGracefulShutdown() throws Exception {
        // Submit a job
        PatternExecutionSnapshot snapshot = createMockSnapshot("test", 10);
        Future<PlacementPlan> future = executor.submitPlanGeneration(snapshot);

        // Wait for it to complete
        future.get(5, TimeUnit.SECONDS);

        // Now shutdown
        executor.shutdown();

        assertTrue("Executor should be shut down", executor.isShutdown());
    }

    @Test
    public void testShutdownRejectsNewJobs() throws Exception {
        // Shutdown executor
        executor.shutdown();

        // Try to submit a job
        PatternExecutionSnapshot snapshot = createMockSnapshot("test", 10);

        try {
            executor.submitPlanGeneration(snapshot);
            fail("Should throw IllegalStateException after shutdown");
        } catch (IllegalStateException e) {
            assertTrue(
                e.getMessage()
                    .contains("shutting down"));
        }
    }

    @Test
    public void testDoubleShutdown() {
        // First shutdown
        executor.shutdown();
        assertTrue("Executor should be shut down", executor.isShutdown());

        // Second shutdown should not throw, just log warning
        executor.shutdown();
        assertTrue("Executor should still be shut down", executor.isShutdown());
    }

    @Test
    public void testCancelJob() throws Exception {
        PatternExecutionSnapshot snapshot = createMockSnapshot("test", 100);
        Future<PlacementPlan> future = executor.submitPlanGeneration(snapshot);

        // Try to cancel (may or may not succeed depending on timing)
        boolean cancelled = future.cancel(true);

        // Either cancelled or already completed
        assertTrue("Future should be cancelled or done", future.isCancelled() || future.isDone());
    }

    @Test
    public void testFutureIsDone() throws Exception {
        PatternExecutionSnapshot snapshot = createMockSnapshot("test", 10);
        Future<PlacementPlan> future = executor.submitPlanGeneration(snapshot);

        // Initially might not be done
        // (or might be done immediately due to fast execution)

        // Wait for completion
        future.get(5, TimeUnit.SECONDS);

        // Now should definitely be done
        assertTrue("Future should be done after get()", future.isDone());
    }

    // Helper methods

    /**
     * Create a mock PatternExecutionSnapshot for testing.
     */
    private PatternExecutionSnapshot createMockSnapshot(String patternName, int positionCount) throws Exception {
        // Create mock compiled script
        LuaValue mockFunction = LuaValue.valueOf(0);
        CompiledScript script = new CompiledScript(patternName, mockFunction);

        // Create positions
        List<PatternExecutionSnapshot.Position> positions = new ArrayList<PatternExecutionSnapshot.Position>();
        for (int i = 0; i < positionCount; i++) {
            positions.add(new PatternExecutionSnapshot.Position(i, 64, i, i, 0, i));
        }

        // Create palette
        List<PatternExecutionSnapshot.PaletteSlot> palette = new ArrayList<PatternExecutionSnapshot.PaletteSlot>();
        palette.add(new PatternExecutionSnapshot.PaletteSlot("minecraft:stone", 0, 64));
        palette.add(new PatternExecutionSnapshot.PaletteSlot("minecraft:dirt", 0, 32));

        // Create context
        PlacementContext context = new PlacementContext(
            0,
            64,
            0, // clicked pos
            1, // click face
            -10,
            50,
            -10, // min
            10,
            80,
            10, // max
            0.0f,
            0.0f, // yaw/pitch
            1000L,
            6000L // times
        );

        return new PatternExecutionSnapshot(
            script,
            patternName,
            positions,
            palette,
            12345L,
            Collections.<String, Object>emptyMap(),
            context);
    }
}
