package com.xXseesXx.patternwand;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import org.junit.Before;
import org.junit.Test;

import com.xXseesXx.patternwand.noise.PerlinNoise;
import com.xXseesXx.patternwand.patterns.scripted.CompiledScript;
import com.xXseesXx.patternwand.patterns.scripted.ScriptEngine;
import com.xXseesXx.patternwand.patterns.scripted.api.NoiseAPI;
import com.xXseesXx.patternwand.patterns.scripted.api.UtilAPI;

public class DebugTest {

    private ScriptEngine engine;
    private IInventory mockPalette;

    @Before
    public void setUp() {
        engine = new ScriptEngine();
        mockPalette = new InventoryBasic("Test Palette", false, 27);
        mockPalette.setInventorySlotContents(0, new ItemStack(Blocks.stone, 64));
        mockPalette.setInventorySlotContents(1, new ItemStack(Blocks.cobblestone, 32));
        mockPalette.setInventorySlotContents(2, new ItemStack(Blocks.dirt, 16));
    }

    @Test
    public void testPerlinDirect() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        System.out.println("Perlin at (0,0): " + perlin.noise(0, 0));
        System.out.println("Perlin at (0,0,0): " + perlin.noise(0, 0, 0));
        System.out.println("Perlin at (100,100): " + perlin.noise(100, 100));
        System.out.println("Perlin at (10.5,20.3): " + perlin.noise(10.5, 20.3));
        System.out.println("Perlin at (0.5,0.5): " + perlin.noise(0.5, 0.5));
    }

    @Test
    public void testNoiseAPI() {
        NoiseAPI noise = new NoiseAPI(12345L);
        System.out.println("NoiseAPI perlin at (0,0): " + noise.perlin(0, 0));
        System.out.println("NoiseAPI perlin at (100,100): " + noise.perlin(100, 100));
        System.out.println("NoiseAPI perlin at (10.5,20.3): " + noise.perlin(10.5, 20.3));
        System.out.println("NoiseAPI perlin at (0.5,0.5): " + noise.perlin(0.5, 0.5));
    }

    @Test
    public void testScriptArgumentPrinting() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    print('x type: ' .. type(x))\n"
            + "    print('palette type: ' .. type(palette))\n"
            + "    print('noise type: ' .. type(noise))\n"
            + "    print('util type: ' .. type(util))\n"
            + "    print('seed type: ' .. type(seed))\n"
            + "    return 0\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "debug_types");
        System.out.println("About to execute pattern...");
        engine.executePattern(compiled, 10, 0, 10, 10, 0, 10, mockPalette, 12345L);
    }

    @Test
    public void testUtilAPIDirectCall() {
        UtilAPI util = new UtilAPI();
        System.out.println("Direct call to util.hash(5.0, 7.0): " + util.hash(5.0, 7.0));
        System.out.println("Direct call to util.hash(5, 7): " + util.hash(5, 7));
        System.out.println("Direct call to util.hash3d(5.0, 0.0, 7.0): " + util.hash3d(5.0, 0.0, 7.0));
        System.out.println("Direct call to util.abs(5.5): " + util.abs(5.5));
    }

    @Test
    public void testSimpleLuaCall() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    print('Calling util.abs(-5.5)')\n"
            + "    local result = util.abs(-5.5)\n"
            + "    print('Result: ' .. result)\n"
            + "    return 0\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "simple_test");
        engine.executePattern(compiled, 0, 0, 0, 0, 0, 0, mockPalette, 12345L);
    }

    @Test
    public void testTwoParamLuaCall() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    print('Calling util.distance(0, 0, 3, 4)')\n"
            + "    local result = util.distance(0, 0, 3, 4)\n"
            + "    print('Result: ' .. result)\n"
            + "    return 0\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "two_param_test");
        engine.executePattern(compiled, 0, 0, 0, 0, 0, 0, mockPalette, 12345L);
    }

    @Test
    public void testDeterministicPattern() throws Exception {
        String script = "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
            + "    print('About to call util.hash')\n"
            + "    print('relX = ' .. relX .. ', type = ' .. type(relX))\n"
            + "    print('relZ = ' .. relZ .. ', type = ' .. type(relZ))\n"
            + "    print('util = ' .. tostring(util) .. ', type = ' .. type(util))\n"
            + "    local hash = util.hash(relX, relZ)\n"
            + "    print('hash result = ' .. hash)\n"
            + "    return hash % 3\n"
            + "end\n"
            + "return pattern";

        CompiledScript compiled = engine.compile(script, "deterministic_debug");
        System.out.println("About to execute pattern...");
        int result = engine.executePattern(compiled, 5, 0, 7, 5, 0, 7, mockPalette, 12345L);
        System.out.println("Result: " + result);
    }
}
