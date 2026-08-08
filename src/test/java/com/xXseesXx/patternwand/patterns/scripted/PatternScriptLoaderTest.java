package com.xXseesXx.patternwand.patterns.scripted;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for PatternScriptLoader.
 * Tests loading patterns from files and directories.
 */
public class PatternScriptLoaderTest {

    private PatternScriptLoader loader;
    private File tempDir;
    private File tempPattern;

    @Before
    public void setUp() throws IOException {
        // Create temporary directory for test patterns
        tempDir = new File(System.getProperty("java.io.tmpdir"), "pattern_test_" + System.currentTimeMillis());
        tempDir.mkdirs();

        // Create loader with temporary directory
        loader = new PatternScriptLoader(tempDir);
    }

    @After
    public void tearDown() {
        // Clean up temporary files
        if (tempPattern != null && tempPattern.exists()) {
            tempPattern.delete();
        }
        if (tempDir != null && tempDir.exists()) {
            deleteRecursive(tempDir);
        }
    }

    /**
     * Recursively delete a directory and all its contents.
     */
    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    @Test
    public void testLoadSinglePattern() throws IOException, ScriptCompileException {
        // Create a simple pattern file
        tempPattern = new File(tempDir, "test_pattern.lua");
        writePatternFile(
            tempPattern,
            "-- META: name = \"Test Pattern\"\n" + "-- META: description = \"A test pattern\"\n"
                + "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
                + "    return 0\n"
                + "end\n"
                + "return pattern");

        loader.loadPattern(tempPattern);
        CompiledScript script = loader.getScript("test_pattern.lua");

        assertNotNull(script);
        assertEquals("test_pattern.lua", script.name);
    }

    @Test
    public void testLoadAllPatternsFromDirectory() throws IOException {
        // Create multiple pattern files
        writePatternFile(
            new File(tempDir, "pattern1.lua"),
            "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 0\n"
                + "end\n"
                + "return pattern");

        writePatternFile(
            new File(tempDir, "pattern2.lua"),
            "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 1\n"
                + "end\n"
                + "return pattern");

        writePatternFile(
            new File(tempDir, "pattern3.lua"),
            "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 2\n"
                + "end\n"
                + "return pattern");

        loader.loadAllPatterns();

        // Should load test patterns (3) plus built-in patterns (13)
        assertTrue("Should have at least 3 patterns", loader.getScriptCount() >= 3);
    }

    @Test
    public void testLoadPatternWithMetadata() throws IOException, ScriptCompileException {
        tempPattern = new File(tempDir, "meta_pattern.lua");
        writePatternFile(
            tempPattern,
            "-- META: name = \"Metadata Pattern\"\n" + "-- META: description = \"Pattern with metadata\"\n"
                + "-- META: author = \"Test Author\"\n"
                + "-- META: version = \"1.0\"\n"
                + "-- META: category = \"Test\"\n"
                + "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
                + "    return 0\n"
                + "end\n"
                + "return pattern");

        loader.loadPattern(tempPattern);
        CompiledScript script = loader.getScript("meta_pattern.lua");

        assertNotNull(script);
        // Note: Current implementation doesn't parse metadata, just compiles
        // This test verifies metadata doesn't break compilation
    }

    @Test
    public void testIgnoreNonLuaFiles() throws IOException {
        // Create Lua files and non-Lua files
        writePatternFile(
            new File(tempDir, "valid.lua"),
            "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 0\n"
                + "end\n"
                + "return pattern");

        // Create non-Lua files
        File txtFile = new File(tempDir, "readme.txt");
        FileWriter writer = new FileWriter(txtFile);
        writer.write("This is not a pattern");
        writer.close();

        loader.loadAllPatterns();

        // Should only load .lua files (1 test file + 13 built-ins)
        assertTrue("Should have at least 1 pattern", loader.getScriptCount() >= 1);
        assertNotNull(loader.getScript("valid.lua"));
    }

    @Test
    public void testLoadPatternWithComplexLogic() throws IOException, ScriptCompileException {
        tempPattern = new File(tempDir, "complex_pattern.lua");
        writePatternFile(
            tempPattern,
            "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
                + "    local dist = util.distance(relX, relZ, 0, 0)\n"
                + "    local noiseVal = noise.perlin(x * 0.1, z * 0.1)\n"
                + "    if dist < 10 then\n"
                + "        return 0\n"
                + "    elseif noiseVal > 0 then\n"
                + "        return 1\n"
                + "    else\n"
                + "        return 2\n"
                + "    end\n"
                + "end\n"
                + "return pattern");

        loader.loadPattern(tempPattern);
        CompiledScript script = loader.getScript("complex_pattern.lua");

        assertNotNull(script);
    }

    @Test(expected = ScriptCompileException.class)
    public void testLoadInvalidPattern() throws IOException, ScriptCompileException {
        tempPattern = new File(tempDir, "invalid.lua");
        writePatternFile(
            tempPattern,
            "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 0\n" +
            // Missing "end" - syntax error
                "return pattern");

        loader.loadPattern(tempPattern);
    }

    @Test
    public void testLoadEmptyDirectory() {
        // Load from empty directory - will still load built-ins
        loader.loadAllPatterns();

        // Should load built-in patterns even with empty directory
        assertTrue("Should have loaded built-in patterns", loader.getScriptCount() > 0);
    }

    @Test
    public void testLoadNonexistentDirectory() {
        File nonexistent = new File(tempDir, "nonexistent");
        PatternScriptLoader loader2 = new PatternScriptLoader(nonexistent);

        // Should not throw exception
        loader2.loadAllPatterns();

        // Should still load built-in patterns even with nonexistent directory
        assertTrue("Should have loaded built-in patterns", loader2.getScriptCount() > 0);
    }

    @Test
    public void testLoadPatternWithComments() throws IOException, ScriptCompileException {
        tempPattern = new File(tempDir, "commented.lua");
        writePatternFile(
            tempPattern,
            "-- This is a comment\n" + "-- Another comment\n"
                + "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
                + "    -- Inline comment\n"
                + "    return 0 -- End of line comment\n"
                + "end\n"
                + "-- Final comment\n"
                + "return pattern");

        loader.loadPattern(tempPattern);
        CompiledScript script = loader.getScript("commented.lua");

        assertNotNull(script);
    }

    @Test
    public void testLoadPatternWithLocalVariables() throws IOException, ScriptCompileException {
        tempPattern = new File(tempDir, "local_vars.lua");
        writePatternFile(
            tempPattern,
            "local SCALE = 0.1\n" + "local THRESHOLD = 0.5\n"
                + "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n"
                + "    local value = noise.perlin(x * SCALE, z * SCALE)\n"
                + "    if value > THRESHOLD then\n"
                + "        return 0\n"
                + "    else\n"
                + "        return 1\n"
                + "    end\n"
                + "end\n"
                + "return pattern");

        loader.loadPattern(tempPattern);
        CompiledScript script = loader.getScript("local_vars.lua");

        assertNotNull(script);
    }

    @Test
    public void testLoadMultiplePatternsSameDirectory() throws IOException {
        // Create multiple patterns with different complexities
        for (int i = 0; i < 10; i++) {
            writePatternFile(
                new File(tempDir, "pattern_" + i + ".lua"),
                "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return "
                    + (i % 3)
                    + "\n"
                    + "end\n"
                    + "return pattern");
        }

        loader.loadAllPatterns();

        // Should load 10 test patterns plus 13 built-ins
        assertTrue("Should have at least 10 patterns", loader.getScriptCount() >= 10);
    }

    @Test
    public void testLoadPatternsRecursively() throws IOException {
        // Create patterns in nested subdirectories
        File subDir1 = new File(tempDir, "category1");
        File subDir2 = new File(tempDir, "category2");
        File subSubDir = new File(subDir1, "advanced");

        subDir1.mkdirs();
        subDir2.mkdirs();
        subSubDir.mkdirs();

        // Root level
        writePatternFile(
            new File(tempDir, "root_pattern.lua"),
            "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 0\n"
                + "end\n"
                + "return pattern");

        // First subdirectory
        writePatternFile(
            new File(subDir1, "sub1_pattern.lua"),
            "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 1\n"
                + "end\n"
                + "return pattern");

        // Second subdirectory
        writePatternFile(
            new File(subDir2, "sub2_pattern.lua"),
            "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 2\n"
                + "end\n"
                + "return pattern");

        // Nested subdirectory (2 levels deep)
        writePatternFile(
            new File(subSubDir, "nested_pattern.lua"),
            "function pattern(x, y, z, relX, relY, relZ, palette, noise, util, seed)\n" + "    return 3\n"
                + "end\n"
                + "return pattern");

        loader.loadAllPatterns();

        // Should find test patterns plus built-in patterns (13)
        // At minimum we need our 4 test patterns
        assertTrue("Should have at least 4 patterns", loader.getScriptCount() >= 4);
        assertNotNull(loader.getScript("root_pattern.lua"));
        assertNotNull(loader.getScript("sub1_pattern.lua"));
        assertNotNull(loader.getScript("sub2_pattern.lua"));
        assertNotNull(loader.getScript("nested_pattern.lua"));
    }

    /**
     * Helper method to write pattern file content.
     */
    private void writePatternFile(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }
}
