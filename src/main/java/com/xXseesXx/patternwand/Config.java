package com.xXseesXx.patternwand;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Hello from PatternWand!";
    public static int patternWandMaxBlocks = 8192 * 16;
    public static int patternWandDurability = 1561; // Same as diamond tools
    public static long defaultPatternSeed = 0L; // Default seed for pattern generation (0 = stable default)

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration
            .getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet thee?");
        patternWandMaxBlocks = configuration.getInt(
            "patternWandMaxBlocks",
            Configuration.CATEGORY_GENERAL,
            patternWandMaxBlocks,
            1,
            10000,
            "Maximum number of blocks the Pattern Wand can place at once");
        patternWandDurability = configuration.getInt(
            "patternWandDurability",
            Configuration.CATEGORY_GENERAL,
            patternWandDurability,
            1,
            10000,
            "Durability of the Pattern Wand");

        // Get long value using Property (1.7.10 Configuration doesn't have getLong)
        String seedValue = configuration.get(
            Configuration.CATEGORY_GENERAL,
            "defaultPatternSeed",
            String.valueOf(defaultPatternSeed),
            "Default seed for pattern generation. Used when no custom seed is set on the wand. Use /patternwand seed <value> to override per-wand.")
            .getString();
        try {
            defaultPatternSeed = Long.parseLong(seedValue);
        } catch (NumberFormatException e) {
            defaultPatternSeed = 0L;
        }

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
