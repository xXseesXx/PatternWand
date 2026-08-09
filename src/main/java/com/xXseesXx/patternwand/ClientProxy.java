package com.xXseesXx.patternwand;

import net.minecraftforge.common.MinecraftForge;

import com.xXseesXx.patternwand.client.PatternWandBlockEvents;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // Register client-side event handlers
        MinecraftForge.EVENT_BUS.register(new PatternWandBlockEvents());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);

        // Register NEI input handler if NEI is present
        registerNEIHandler();
    }

    /**
     * Register NEI input handler for bookmark group drag-and-drop support.
     * This is done in postInit to ensure NEI has loaded, and wrapped in a try-catch
     * to gracefully handle the case where NEI is not installed.
     */
    private void registerNEIHandler() {
        try {
            Class.forName("codechicken.nei.guihook.GuiContainerManager");

            // NEI is present, register our handler
            codechicken.nei.guihook.GuiContainerManager
                .addInputHandler(new com.xXseesXx.patternwand.integration.nei.PatternWandNEIInputHandler());

            PatternWandMod.LOG.info("Registered NEI input handler for bookmark group support");
        } catch (ClassNotFoundException e) {
            // NEI is not present, skip registration
            PatternWandMod.LOG.debug("NEI not found, bookmark group support disabled");
        } catch (Exception e) {
            // Something went wrong during registration
            PatternWandMod.LOG.warn("Failed to register NEI input handler", e);
        }
    }
}
