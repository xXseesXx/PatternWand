package com.xXseesXx.patternwand.patterns.scripted;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.minecraft.inventory.IInventory;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.xXseesXx.patternwand.patterns.scripted.api.LuaNoiseWrapper;
import com.xXseesXx.patternwand.patterns.scripted.api.LuaPaletteWrapper;
import com.xXseesXx.patternwand.patterns.scripted.api.LuaUtilWrapper;
import com.xXseesXx.patternwand.patterns.scripted.api.NoiseAPI;
import com.xXseesXx.patternwand.patterns.scripted.api.PaletteAPI;
import com.xXseesXx.patternwand.patterns.scripted.api.UtilAPI;

/**
 * Simplified Lua script engine for pattern scripts.
 * Compiles and executes Lua scripts with basic timeout protection.
 */
public class ScriptEngine {

    private static final long TIMEOUT_SECONDS = 10;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private final Globals globals;

    /**
     * Create a new script engine instance.
     */
    public ScriptEngine() {
        // Create standard Lua globals (includes math, string, table libraries)
        this.globals = JsePlatform.standardGlobals();
    }

    /**
     * Compile a Lua script.
     *
     * @param source Lua source code
     * @param name   Script name (for error messages)
     * @return Compiled script
     * @throws ScriptCompileException If compilation fails
     */
    public CompiledScript compile(String source, String name) throws ScriptCompileException {
        try {
            // Compile the Lua source
            LuaValue chunk = globals.load(source, name);

            // Execute to get the pattern function
            LuaValue result = chunk.call();

            if (!result.isfunction()) {
                throw new ScriptCompileException(
                    name,
                    "Script must return a function. Example:\n"
                        + "function pattern(x, y, z, relX, relY, relZ, palette, noise, seed)\n"
                        + "    return 0\n"
                        + "end\n"
                        + "return pattern");
            }

            return new CompiledScript(name, result);

        } catch (LuaError e) {
            throw new ScriptCompileException(name, "Compilation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a compiled pattern script for a single block.
     *
     * @param script           Compiled script
     * @param x                World X coordinate
     * @param y                World Y coordinate
     * @param z                World Z coordinate
     * @param relX             Relative X (from pattern origin)
     * @param relY             Relative Y (from pattern origin)
     * @param relZ             Relative Z (from pattern origin)
     * @param paletteInventory Palette inventory
     * @param seed             Pattern seed
     * @return Palette index (0-26) or -1 for gap
     * @throws ScriptExecutionException If execution fails or times out
     */
    public int executePattern(CompiledScript script, int x, int y, int z, int relX, int relY, int relZ,
        IInventory paletteInventory, long seed) throws ScriptExecutionException {

        // Create API objects
        NoiseAPI noise = new NoiseAPI(seed);
        PaletteAPI palette = new PaletteAPI(paletteInventory, seed);
        UtilAPI util = new UtilAPI();

        // Wrap Java APIs in Lua-friendly tables
        LuaTable luaNoise = LuaNoiseWrapper.wrap(noise);
        LuaTable luaPalette = LuaPaletteWrapper.wrap(palette);
        LuaTable luaUtil = LuaUtilWrapper.wrap(util);

        // Create callable for timeout
        Callable<Integer> task = () -> {
            try {
                // Call pattern function with arguments
                // Use invoke() which accepts varargs
                LuaValue result = script.function
                    .invoke(
                        new LuaValue[] { LuaValue.valueOf(x), LuaValue.valueOf(y), LuaValue.valueOf(z),
                            LuaValue.valueOf(relX), LuaValue.valueOf(relY), LuaValue.valueOf(relZ), luaPalette,
                            luaNoise, luaUtil, LuaValue.valueOf(seed) })
                    .arg1(); // Get first return value

                // Handle return value
                if (result.isnil()) {
                    return -1; // nil means gap (don't place block)
                } else if (result.isnumber()) {
                    int index = result.toint();
                    // Validate palette index
                    if (index < 0 || index >= 27) {
                        throw new ScriptExecutionException(
                            script.name,
                            "Pattern returned invalid palette index: " + index + " (must be 0-26)");
                    }
                    return index;
                } else {
                    throw new ScriptExecutionException(
                        script.name,
                        "Pattern must return a number (palette index) or nil (gap). Got: " + result.typename());
                }

            } catch (LuaError e) {
                throw new ScriptExecutionException(script.name, "Runtime error: " + e.getMessage(), e);
            }
        };

        // Execute with timeout
        Future<Integer> future = executor.submit(task);

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ScriptExecutionException(
                script.name,
                "Script timeout (" + TIMEOUT_SECONDS
                    + " seconds). "
                    + "Your pattern is too complex or has an infinite loop.");

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ScriptExecutionException) {
                throw (ScriptExecutionException) cause;
            }
            throw new ScriptExecutionException(script.name, "Execution failed: " + cause.getMessage(), cause);

        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            throw new ScriptExecutionException(script.name, "Script execution interrupted");
        }
    }

    /**
     * Shutdown the executor service.
     * Should be called when the engine is no longer needed.
     */
    public static void shutdown() {
        executor.shutdown();
    }
}
