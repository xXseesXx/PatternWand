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

        // Initialize pattern script loader (but don't load patterns yet)
        File configDir = event.getModConfigurationDirectory();
        File patternsDir = new File(configDir, "patternwand/patterns");

        // Create directory if it doesn't exist
        if (!patternsDir.exists()) {
            patternsDir.mkdirs();
            PatternWandMod.LOG.info("Created patterns directory: {}", patternsDir.getAbsolutePath());
        }

        scriptLoader = new PatternScriptLoader(patternsDir);

        PatternWandMod.LOG.info(Config.greeting);
        PatternWandMod.LOG.info("Pattern Wand item registered!");
    }

    public void init(FMLInitializationEvent event) {
        // Load patterns during init phase when resource managers are fully available
        if (scriptLoader != null) {
            scriptLoader.loadAllPatterns();
        }

        // Register crafting recipe for Pattern Wand
        registerPatternWandRecipe();
    }

    private void registerPatternWandRecipe() {
        // Get the unbreakable wand from BetterBuildersWands
        net.minecraft.item.Item bbwUnbreakableWand = GameRegistry.findItem("betterbuilderswands", "wandUnbreakable");

        if (bbwUnbreakableWand == null) {
            PatternWandMod.LOG.error("Could not find BetterBuildersWands unbreakable wand for recipe!");
            return;
        }

        // Tier 11 (4096): 1 redstone, 1 lapis
        // Pattern:
        // - R -
        // R W R
        // - L -
        GameRegistry.addRecipe(
            new net.minecraft.item.ItemStack(ModItems.patternWandUnbreakable, 1, 11),
            " R ",
            "RWR",
            " L ",
            'W',
            new net.minecraft.item.ItemStack(bbwUnbreakableWand, 1, 11),
            'R',
            net.minecraft.init.Blocks.redstone_block,
            'L',
            net.minecraft.init.Blocks.lapis_block);

        // Tier 12 (8192): 2 redstone, 2 lapis
        // Pattern:
        // L R L
        // R W R
        // - - -
        GameRegistry.addRecipe(
            new net.minecraft.item.ItemStack(ModItems.patternWandUnbreakable, 1, 12),
            "LRL",
            "RWR",
            "   ",
            'W',
            new net.minecraft.item.ItemStack(bbwUnbreakableWand, 1, 12),
            'R',
            net.minecraft.init.Blocks.redstone_block,
            'L',
            net.minecraft.init.Blocks.lapis_block);

        // Tier 13 (16384): 4 redstone, 4 lapis
        // Pattern:
        // L R L
        // R W R
        // L R L
        GameRegistry.addRecipe(
            new net.minecraft.item.ItemStack(ModItems.patternWandUnbreakable, 1, 13),
            "LRL",
            "RWR",
            "LRL",
            'W',
            new net.minecraft.item.ItemStack(bbwUnbreakableWand, 1, 13),
            'R',
            net.minecraft.init.Blocks.redstone_block,
            'L',
            net.minecraft.init.Blocks.lapis_block);

        PatternWandMod.LOG.info("Registered Pattern Wand crafting recipes for all tiers");
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {
        // Register commands
        PatternWandCommand mainCommand = new PatternWandCommand(scriptLoader);
        event.registerServerCommand(mainCommand);
        event.registerServerCommand(new com.xXseesXx.patternwand.commands.PatternWandAliasCommand(mainCommand, "pw"));
    }

    public PatternScriptLoader getScriptLoader() {
        return scriptLoader;
    }
}
