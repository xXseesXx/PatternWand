package com.patternwand.patterns.scripted;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.patternwand.PatternWandMod;

/**
 * Loads and caches Lua pattern scripts from the patterns directory.
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
     * Load all pattern scripts from the patterns directory.
     * Logs errors but doesn't fail if some scripts can't be loaded.
     */
    public void loadAllPatterns() {
        if (!patternsDir.exists()) {
            PatternWandMod.LOG.warn("Patterns directory does not exist: {}", patternsDir.getAbsolutePath());
            return;
        }

        if (!patternsDir.isDirectory()) {
            PatternWandMod.LOG.error("Patterns path is not a directory: {}", patternsDir.getAbsolutePath());
            return;
        }

        PatternWandMod.LOG.info("Loading pattern scripts from: {}", patternsDir.getAbsolutePath());

        int loaded = 0;
        int failed = 0;

        // Load from main patterns directory
        File[] files = patternsDir.listFiles(
            (dir, name) -> name.toLowerCase()
                .endsWith(".lua"));
        if (files != null) {
            for (File file : files) {
                try {
                    loadPattern(file);
                    loaded++;
                } catch (Exception e) {
                    PatternWandMod.LOG.error("Failed to load pattern script: {}", file.getName(), e);
                    failed++;
                }
            }
        }

        // Also load from examples subdirectory
        File examplesDir = new File(patternsDir, "examples");
        if (examplesDir.exists() && examplesDir.isDirectory()) {
            File[] exampleFiles = examplesDir.listFiles(
                (dir, name) -> name.toLowerCase()
                    .endsWith(".lua"));
            if (exampleFiles != null) {
                for (File file : exampleFiles) {
                    try {
                        loadPattern(file);
                        loaded++;
                    } catch (Exception e) {
                        PatternWandMod.LOG.error("Failed to load example pattern: {}", file.getName(), e);
                        failed++;
                    }
                }
            }
        }

        PatternWandMod.LOG.info("Loaded {} pattern scripts ({} failed)", loaded, failed);
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
