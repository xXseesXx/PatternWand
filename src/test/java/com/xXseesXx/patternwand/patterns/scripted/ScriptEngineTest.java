package com.xXseesXx.patternwand.patterns.scripted;

import static org.junit.Assert.*;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for ScriptEngine.
 * Tests script compilation, execution, and error handling.
 */
public class ScriptEngineTest {

    private ScriptEngine engine;
    private IInventory mockPalette;

    @Before
    public void setUp() {
        engine = new ScriptEngine();

        // Create mock palette with some blocks
        mockPalette = new InventoryBasic("Test Palette", false, 54);
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 64));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 32));
        mockPalette.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 16));
    }

    @Test
    public void testSimplePatternCompilation() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 0\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "test_simple");
        assertNotNull(compiled);
        assertEquals("test_simple", compiled.name);
    }

    @Test
    public void testCheckerboardPattern() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    if (relX + relZ) % 2 == 0 then\n"
            + "        return 0\n"
            + "    else\n"
            + "        return 1\n"
            + "    end\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "checkerboard");

        // Test execution at various points
        int result1 = engine.executePattern(compiled, 0, 0, 0, 0, 0, 0, mockPalette, 12345L);
        int result2 = engine.executePattern(compiled, 1, 0, 1, 1, 0, 1, mockPalette, 12345L);
        int result3 = engine.executePattern(compiled, 1, 0, 0, 1, 0, 0, mockPalette, 12345L);

        assertEquals(0, result1); // (0,0) should be 0
        assertEquals(0, result2); // (1,1) should be 0
        assertEquals(1, result3); // (1,0) should be 1
    }

    @Test
    public void testPatternWithNoise() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    local value = noise.perlin(x * 0.1, z * 0.1)\n"
            + "    if value > 0 then\n"
            + "        return 0\n"
            + "    else\n"
            + "        return 1\n"
            + "    end\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "noise_pattern");
        int result = engine.executePattern(compiled, 10, 0, 10, 10, 0, 10, mockPalette, 12345L);

        assertTrue(result >= 0 && result <= 2);
    }

    @Test
    public void testPatternWithUtil() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    local dist = util.distance(relX, relZ, 0, 0)\n"
            + "    if dist < 5 then\n"
            + "        return 0\n"
            + "    else\n"
            + "        return 1\n"
            + "    end\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "util_pattern");

        int result1 = engine.executePattern(compiled, 0, 0, 0, 0, 0, 0, mockPalette, 12345L);
        int result2 = engine.executePattern(compiled, 10, 0, 10, 10, 0, 10, mockPalette, 12345L);

        assertEquals(0, result1); // Close to center
        assertEquals(1, result2); // Far from center
    }

    @Test
    public void testPatternWithPalette() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    local size = palette.countNonEmpty()\n"
            + "    return (relX % size)\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "palette_pattern");

        int result = engine.executePattern(compiled, 5, 0, 0, 5, 0, 0, mockPalette, 12345L);
        assertEquals(2, result); // 5 % 3 = 2
    }

    @Test
    public void testReturnNilForGap() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    if relX % 2 == 0 then\n"
            + "        return 0\n"
            + "    else\n"
            + "        return nil\n"
            + "    end\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "gap_pattern");

        int result1 = engine.executePattern(compiled, 0, 0, 0, 0, 0, 0, mockPalette, 12345L);
        int result2 = engine.executePattern(compiled, 1, 0, 0, 1, 0, 0, mockPalette, 12345L);

        assertEquals(0, result1);
        assertEquals(-1, result2); // nil returns -1
    }

    @Test(expected = ScriptCompileException.class)
    public void testInvalidSyntax() throws Exception {
        String script = "function pattern(x, y, z)\n" + "    return 0\n" + // Missing 'end'
            "return pattern";

        engine.compile(script, "invalid_syntax");
    }

    @Test(expected = ScriptCompileException.class)
    public void testNotReturningFunction() throws Exception {
        String script = "local x = 5\n" + "return x"; // Returns number, not function

        engine.compile(script, "not_function");
    }

    @Test(expected = ScriptExecutionException.class)
    public void testRuntimeError() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    local result = 10 / 0\n"
            + // Division by zero
            "    return result\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "runtime_error");
        engine.executePattern(compiled, 0, 0, 0, 0, 0, 0, mockPalette, 12345L);
    }

    @Test(expected = ScriptExecutionException.class)
    public void testInvalidPaletteIndex() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 100\n"
            + // Invalid index (must be 0-26)
            "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "invalid_index");
        engine.executePattern(compiled, 0, 0, 0, 0, 0, 0, mockPalette, 12345L);
    }

    @Test(expected = ScriptExecutionException.class)
    public void testInvalidReturnType() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    return 'string'\n"
            + // Returns string instead of number
            "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "invalid_return");
        engine.executePattern(compiled, 0, 0, 0, 0, 0, 0, mockPalette, 12345L);
    }

    @Test
    public void testDeterministicExecution() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    local hash = util.hash(relX, relZ)\n"
            + "    return hash % 3\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "deterministic");

        // Execute multiple times with same inputs
        int result1 = engine.executePattern(compiled, 5, 0, 7, 5, 0, 7, mockPalette, 12345L);
        int result2 = engine.executePattern(compiled, 5, 0, 7, 5, 0, 7, mockPalette, 12345L);

        assertEquals(result1, result2); // Should be deterministic
    }

    @Test
    public void testMathFunctions() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    local angle = math.atan2(relZ, relX)\n"
            + "    local dist = math.sqrt(relX * relX + relZ * relZ)\n"
            + "    local value = math.sin(angle) * math.cos(dist)\n"
            + "    if value > 0 then\n"
            + "        return 0\n"
            + "    else\n"
            + "        return 1\n"
            + "    end\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "math_functions");
        int result = engine.executePattern(compiled, 10, 0, 10, 10, 0, 10, mockPalette, 12345L);

        assertTrue(result >= 0 && result <= 2);
    }
}
