package com.xXseesXx.patternwand;

import java.io.File;

import com.xXseesXx.patternwand.commands.PatternWandCommand;
import com.xXseesXx.patternwand.gui.PatternWandGuiHandler;
import com.xXseesXx.patternwand.patterns.scripted.PatternScriptLoader;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

public class CommonProxy {

    private PatternScriptLoader scriptLoader;

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        // Register items
        GameRegistry.registerItem(ModItems.patternWandUnbreakable, "patternWandUnbreakable");

        // Register GUI handler
        NetworkRegistry.INSTANCE.registerGuiHandler(PatternWandMod.instance, new PatternWandGuiHandler());

        // Initialize pattern script loader
        File configDir = event.getModConfigurationDirectory();
        File patternsDir = new File(configDir, "patternwand/patterns");

        // Create directory if it doesn't exist
        if (!patternsDir.exists()) {
            patternsDir.mkdirs();
            PatternWandMod.LOG.info("Created patterns directory: {}", patternsDir.getAbsolutePath());
        }

        scriptLoader = new PatternScriptLoader(patternsDir);
        scriptLoader.loadAllPatterns();

        PatternWandMod.LOG.info(Config.greeting);
        PatternWandMod.LOG.info("Pattern Wand item registered!");
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {
        // Register commands
        event.registerServerCommand(new PatternWandCommand(scriptLoader));
    }

    public PatternScriptLoader getScriptLoader() {
        return scriptLoader;
    }
}
