package com.xXseesXx.patternwand.patterns.scripted;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.xXseesXx.patternwand.PatternWandMod;

/**
 * Loads and caches Lua pattern scripts from the patterns directory and mod assets.
 */
public class PatternScriptLoader {

    private final ScriptEngine engine;
    private final Map<String, CompiledScript> scriptCache;
    private final File patternsDir;

    /**
     * Create a new pattern script loader.
     *
     * @param patternsDir Directory containing .lua pattern files
     */
    public PatternScriptLoader(File patternsDir) {
        this.engine = new ScriptEngine();
        this.scriptCache = new HashMap<>();
        this.patternsDir = patternsDir;
    }

    /**
     * Load all pattern scripts from the patterns directory and mod assets.
     * Recursively searches all subdirectories for .lua files.
     * Filesystem patterns are loaded first and can override built-in patterns.
     * Logs errors but doesn't fail if some scripts can't be loaded.
     */
    public void loadAllPatterns() {
        int loaded = 0;
        int failed = 0;

        // Load from filesystem config directory FIRST (higher priority)
        if (!patternsDir.exists()) {
            PatternWandMod.LOG.info("Patterns directory does not exist: {}", patternsDir.getAbsolutePath());
        } else if (!patternsDir.isDirectory()) {
            PatternWandMod.LOG.error("Patterns path is not a directory: {}", patternsDir.getAbsolutePath());
        } else {
            PatternWandMod.LOG.info("Loading pattern scripts from: {}", patternsDir.getAbsolutePath());

            // Recursively load all .lua files from patterns directory and subdirectories
            int[] counts = loadPatternsRecursive(patternsDir);
            loaded += counts[0];
            failed += counts[1];
        }

        // Load built-in patterns from mod assets SECOND (lower priority, won't override filesystem)
        try {
            int assetLoaded = loadPatternsFromModAssets();
            loaded += assetLoaded;
            if (assetLoaded > 0) {
                PatternWandMod.LOG.info("Loaded {} built-in patterns from mod assets", assetLoaded);
            }
        } catch (Exception e) {
            PatternWandMod.LOG.error("Failed to load patterns from mod assets", e);
            failed++;
        }

        PatternWandMod.LOG.info("Loaded {} pattern scripts total ({} failed)", loaded, failed);
    }

    /**
     * Recursively load all .lua files from a directory and its subdirectories.
     *
     * @param directory Directory to search
     * @return Array with [loaded count, failed count]
     */
    private int[] loadPatternsRecursive(File directory) {
        int loaded = 0;
        int failed = 0;

        File[] entries = directory.listFiles();
        if (entries == null) {
            return new int[] { 0, 0 };
        }

        for (File entry : entries) {
            if (entry.isDirectory()) {
                // Recursively load from subdirectory
                int[] subCounts = loadPatternsRecursive(entry);
                loaded += subCounts[0];
                failed += subCounts[1];
            } else if (entry.isFile() && entry.getName()
                .toLowerCase()
                .endsWith(".lua")) {
                    // Load pattern file
                    try {
                        loadPattern(entry);
                        loaded++;
                    } catch (Exception e) {
                        PatternWandMod.LOG.error("Failed to load pattern script: {}", entry.getName(), e);
                        failed++;
                    }
                }
        }

        return new int[] { loaded, failed };
    }

    /**
     * Load built-in patterns from mod assets only.
     * Does NOT load from resource packs for security reasons.
     * Automatically discovers all .lua files in the patterns/examples directory.
     *
     * @return Number of patterns loaded
     */
    private int loadPatternsFromModAssets() {
        int loaded = 0;

        // Hardcoded list of built-in example patterns
        // Simpler and more reliable than jar enumeration
        String[] builtinPatterns = { "default_context_bounds.lua", "default_context_click.lua",
            "default_context_orientation.lua", "default_context_time.lua", "default_debug.lua",
            "default_geometry_box.lua", "default_geometry_rotate2d.lua", "default_geometry_rotate_face.lua",
            "default_geometry_sphere.lua", "default_metadata_ignore.lua", "default_noise_perlin2d.lua",
            "default_noise_perlin3d.lua", "default_noise_simplex2d.lua", "default_noise_simplex3d.lua",
            "default_palette_exclude.lua", "default_palette_range.lua", "default_palette_uniform.lua",
            "default_palette_weighted.lua", "default_params_types.lua", "default_seed.lua", "default_skip_blocks.lua",
            "default_util_distance.lua", "default_util_hash.lua", "default_util_math.lua",
            "default_util_smoothstep.lua" };

        for (String filename : builtinPatterns) {
            loaded += loadBuiltinPattern(filename);
        }

        return loaded;
    }

    /**
     * Load a single built-in pattern by filename.
     *
     * @param filename Pattern filename (e.g., "checkerboard.lua")
     * @return 1 if loaded, 0 if skipped or failed
     */
    private int loadBuiltinPattern(String filename) {
        try {
            InputStream stream = getClass().getResourceAsStream("/assets/patternwand/patterns/examples/" + filename);
            if (stream != null) {
                try {
                    String source = readStreamAsString(stream);
                    CompiledScript compiled = engine.compile(source, filename);

                    // Only cache if not already loaded from filesystem
                    if (!scriptCache.containsKey(filename)) {
                        scriptCache.put(filename, compiled);
                        PatternWandMod.LOG.debug("Loaded built-in pattern: {}", filename);
                        return 1;
                    } else {
                        PatternWandMod.LOG.debug("Skipping built-in pattern (overridden by filesystem): {}", filename);
                    }
                } finally {
                    stream.close();
                }
            }
        } catch (Exception e) {
            PatternWandMod.LOG.debug("Failed to load built-in pattern: {}", filename, e);
        }
        return 0;
    }

    /**
     * Read an input stream as a string.
     *
     * @param stream The stream to read
     * @return String contents of the stream
     * @throws IOException If reading fails
     */
    private String readStreamAsString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            content.append(line)
                .append('\n');
        }

        return content.toString();
    }

    /**
     * Load a single pattern script from a file.
     *
     * @param file Lua script file
     * @throws IOException            If file cannot be read
     * @throws ScriptCompileException If script compilation fails
     */
    public void loadPattern(File file) throws IOException, ScriptCompileException {
        String name = file.getName();

        // Read file contents
        String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        // Compile script
        CompiledScript compiled = engine.compile(source, name);

        // Cache compiled script
        scriptCache.put(name, compiled);

        PatternWandMod.LOG.debug("Loaded pattern script: {}", name);
    }

    /**
     * Get a compiled script by name.
     *
     * @param name Script name (including .lua extension)
     * @return Compiled script, or null if not found
     */
    public CompiledScript getScript(String name) {
        return scriptCache.get(name);
    }

    /**
     * Get all loaded script names.
     *
     * @return Array of script names
     */
    public String[] getScriptNames() {
        return scriptCache.keySet()
            .toArray(new String[0]);
    }

    /**
     * Get the number of loaded scripts.
     *
     * @return Script count
     */
    public int getScriptCount() {
        return scriptCache.size();
    }

    /**
     * Reload all scripts (clears cache and reloads from disk).
     */
    public void reload() {
        scriptCache.clear();
        loadAllPatterns();
    }

    /**
     * Get the script engine instance.
     *
     * @return Script engine
     */
    public ScriptEngine getEngine() {
        return engine;
    }
}
