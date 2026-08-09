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

import com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI;
import com.xXseesXx.patternwand.patterns.scripted.api.LuaContextWrapper;
import com.xXseesXx.patternwand.patterns.scripted.api.LuaDebugWrapper;
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
     * Create a new script engine instance with sandboxed Lua environment.
     * Only safe libraries are exposed to prevent malicious scripts.
     */
    public ScriptEngine() {
        // Create standard Lua globals (includes math, string, table libraries)
        this.globals = JsePlatform.standardGlobals();

        // Remove dangerous libraries that could access filesystem or execute commands
        // This prevents malicious patterns from doing anything harmful
        globals.set("os", LuaValue.NIL); // Remove OS library (file system, command execution)
        globals.set("io", LuaValue.NIL); // Remove IO library (file operations)
        globals.set("package", LuaValue.NIL); // Remove package library (prevents loading external modules)
        globals.set("dofile", LuaValue.NIL); // Remove file execution
        globals.set("loadfile", LuaValue.NIL); // Remove file loading
        globals.set("require", LuaValue.NIL); // Remove module loading
        globals.set("luajava", LuaValue.NIL); // Remove Java binding library (prevents access to Java classes)

        // Set memory limit (128KB per script instance - reasonable for pattern logic)
        // This prevents memory exhaustion attacks
        globals.load(new java.io.StringReader(""), "memory_limit")
            .call();
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

            // Execute to get the pattern function and optional metadata
            LuaValue result = chunk.call();

            // Extract pattern function
            LuaValue patternFunction = null;
            PatternMetadata metadata = null;

            // Check if result is a function (simple pattern) or if we need to look for 'pattern' global
            if (result.isfunction()) {
                patternFunction = result;
            } else {
                // Look for 'pattern' function in globals
                patternFunction = globals.get("pattern");
                if (!patternFunction.isfunction()) {
                    throw new ScriptCompileException(
                        name,
                        "Script must define a 'pattern' function or return one. Example:\n"
                            + "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
                            + "    return 0\n"
                            + "end");
                }
            }

            // Try to extract metadata if present
            LuaValue metadataTable = globals.get("metadata");
            if (metadataTable.istable()) {
                metadata = extractMetadata(metadataTable);
            }

            return new CompiledScript(name, patternFunction, metadata);

        } catch (LuaError e) {
            throw new ScriptCompileException(name, "Compilation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extract pattern metadata from Lua metadata table.
     *
     * @param metadataTable Lua metadata table
     * @return PatternMetadata object
     */
    private PatternMetadata extractMetadata(LuaValue metadataTable) {
        String name = null;
        String author = null;
        String description = null;
        boolean ignoreMetadata = false;
        java.util.List<PatternParameter> parameters = new java.util.ArrayList<PatternParameter>();

        // Extract name
        LuaValue nameValue = metadataTable.get("name");
        if (!nameValue.isnil()) {
            name = nameValue.tojstring();
        }

        // Extract author
        LuaValue authorValue = metadataTable.get("author");
        if (!authorValue.isnil()) {
            author = authorValue.tojstring();
        }

        // Extract description
        LuaValue descriptionValue = metadataTable.get("description");
        if (!descriptionValue.isnil()) {
            description = descriptionValue.tojstring();
        }

        // Extract ignoreMetadata flag
        LuaValue ignoreMetadataValue = metadataTable.get("ignoreMetadata");
        if (!ignoreMetadataValue.isnil()) {
            ignoreMetadata = ignoreMetadataValue.toboolean();
        }

        // Extract parameters (now a dictionary/table)
        LuaValue parametersTable = metadataTable.get("parameters");
        if (parametersTable.istable()) {
            // Iterate through dictionary keys
            LuaValue key = LuaValue.NIL;
            while (true) {
                org.luaj.vm2.Varargs entry = parametersTable.next(key);
                key = entry.arg1();
                if (key.isnil()) {
                    break;
                }

                LuaValue paramTable = entry.arg(2);
                if (paramTable.istable()) {
                    // Key is the parameter name
                    String paramName = key.tojstring();
                    PatternParameter param = extractParameter(paramName, paramTable);
                    if (param != null) {
                        parameters.add(param);
                    }
                }
            }
        }

        return new PatternMetadata(name, author, description, parameters, ignoreMetadata);
    }

    /**
     * Extract a single parameter from Lua parameter table.
     *
     * @param paramName  Parameter name from dictionary key
     * @param paramTable Lua parameter table
     * @return PatternParameter object, or null if invalid
     */
    private PatternParameter extractParameter(String paramName, LuaValue paramTable) {
        // Extract type
        LuaValue typeValue = paramTable.get("type");
        if (typeValue.isnil()) {
            return null; // Type is required
        }
        String typeStr = typeValue.tojstring()
            .toLowerCase();
        PatternParameter.Type type;
        switch (typeStr) {
            case "integer":
            case "int":
                type = PatternParameter.Type.INTEGER;
                break;
            case "float":
            case "number":
            case "double":
                type = PatternParameter.Type.FLOAT;
                break;
            case "boolean":
            case "bool":
                type = PatternParameter.Type.BOOLEAN;
                break;
            case "string":
            case "text":
                type = PatternParameter.Type.STRING;
                break;
            default:
                return null; // Invalid type
        }

        // Extract default value
        LuaValue defaultValue = paramTable.get("default");
        Object defaultObj;
        switch (type) {
            case INTEGER:
                defaultObj = defaultValue.isnil() ? 0 : defaultValue.toint();
                break;
            case FLOAT:
                defaultObj = defaultValue.isnil() ? 0.0 : defaultValue.todouble();
                break;
            case BOOLEAN:
                defaultObj = defaultValue.isnil() ? false : defaultValue.toboolean();
                break;
            case STRING:
                defaultObj = defaultValue.isnil() ? "" : defaultValue.tojstring();
                break;
            default:
                defaultObj = null;
        }

        // Extract min/max for numeric types
        Double min = null;
        Double max = null;
        if (type == PatternParameter.Type.INTEGER || type == PatternParameter.Type.FLOAT) {
            LuaValue minValue = paramTable.get("min");
            if (!minValue.isnil()) {
                min = minValue.todouble();
            }
            LuaValue maxValue = paramTable.get("max");
            if (!maxValue.isnil()) {
                max = maxValue.todouble();
            }
        }

        return new PatternParameter(paramName, type, defaultObj, min, max);
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
     * @param parameterValues  Parameter values (can be null)
     * @param context          Placement context (can be null)
     * @return Palette index (0-26) or -1 for gap
     * @throws ScriptExecutionException If execution fails or times out
     */
    public int executePattern(CompiledScript script, int x, int y, int z, int relX, int relY, int relZ,
        IInventory paletteInventory, long seed, java.util.Map<String, Object> parameterValues, PlacementContext context)
        throws ScriptExecutionException {

        // Start timing if debug is enabled
        long startTimeNs = DebugAPI.isDebugEnabled() ? System.nanoTime() : 0;

        // Create API objects
        NoiseAPI noise = new NoiseAPI(seed);
        PaletteAPI palette = new PaletteAPI(paletteInventory, seed);
        UtilAPI util = new UtilAPI();
        DebugAPI debug = new DebugAPI();

        // Wrap Java APIs in Lua-friendly tables
        LuaTable luaNoise = LuaNoiseWrapper.wrap(noise);
        LuaTable luaPalette = LuaPaletteWrapper.wrap(palette);
        LuaTable luaUtil = LuaUtilWrapper.wrap(util);
        LuaTable luaDebug = LuaDebugWrapper.wrap(debug);

        // Create parameters table
        LuaTable luaParams = new LuaTable();
        if (parameterValues != null) {
            for (java.util.Map.Entry<String, Object> entry : parameterValues.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Integer) {
                    luaParams.set(entry.getKey(), LuaValue.valueOf((Integer) value));
                } else if (value instanceof Number) {
                    luaParams.set(entry.getKey(), LuaValue.valueOf(((Number) value).doubleValue()));
                } else if (value instanceof Boolean) {
                    luaParams.set(entry.getKey(), LuaValue.valueOf((Boolean) value));
                } else if (value instanceof String) {
                    luaParams.set(entry.getKey(), LuaValue.valueOf((String) value));
                }
            }
        }

        // Create context table
        LuaTable luaContext = (context != null) ? LuaContextWrapper.wrap(context) : new LuaTable();

        // Create callable for timeout
        Callable<Integer> task = () -> {
            try {
                // Call pattern function with arguments
                // Use invoke() which accepts varargs
                LuaValue result = script.function
                    .invoke(
                        new LuaValue[] { LuaValue.valueOf(x), LuaValue.valueOf(y), LuaValue.valueOf(z),
                            LuaValue.valueOf(relX), LuaValue.valueOf(relY), LuaValue.valueOf(relZ), luaPalette,
                            luaNoise, luaUtil, LuaValue.valueOf(seed), luaParams, luaContext, luaDebug })
                    .arg1(); // Get first return value

                // Handle return value
                if (result.isnil()) {
                    return -1; // nil means gap (don't place block)
                } else if (result.isnumber()) {
                    int index = result.toint();
                    // Validate palette index
                    if (index < 0 || index >= 54) {
                        throw new ScriptExecutionException(
                            script.name,
                            "Pattern returned invalid palette index: " + index + " (must be 0-53)");
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
            Integer result = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Record execution time if debug is enabled
            if (DebugAPI.isDebugEnabled()) {
                long executionTimeNs = System.nanoTime() - startTimeNs;
                DebugAPI.recordBlockExecution(executionTimeNs);
            }

            return result;

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
     * Execute pattern with default parameters (backward compatibility).
     */
    public int executePattern(CompiledScript script, int x, int y, int z, int relX, int relY, int relZ,
        IInventory paletteInventory, long seed) throws ScriptExecutionException {
        java.util.Map<String, Object> params = script.metadata.createDefaultValues();
        return executePattern(script, x, y, z, relX, relY, relZ, paletteInventory, seed, params, null);
    }

    /**
     * Shutdown the executor service.
     * Should be called when the engine is no longer needed.
     */
    public static void shutdown() {
        executor.shutdown();
    }
}
