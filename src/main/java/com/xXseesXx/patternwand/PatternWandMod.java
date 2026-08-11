package com.xXseesXx.patternwand;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.xXseesXx.patternwand.network.PacketSyncPalette;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

@Mod(
    modid = PatternWandMod.MODID,
    version = Tags.VERSION,
    name = "PatternWand",
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:betterbuilderswands",
    guiFactory = "com.xXseesXx.patternwand.gui.PatternWandGuiFactory")
public class PatternWandMod {

    public static final String MODID = "patternwand";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static java.io.File configFile;

    @SidedProxy(
        clientSide = "com.xXseesXx.patternwand.ClientProxy",
        serverSide = "com.xXseesXx.patternwand.CommonProxy")
    public static CommonProxy proxy;

    @Mod.Instance(MODID)
    public static PatternWandMod instance;

    public static SimpleNetworkWrapper networkWrapper;

    public static com.xXseesXx.patternwand.executor.AsyncPlacementHandler asyncPlacementHandler;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Register network handler
        networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        networkWrapper.registerMessage(PacketSyncPalette.Handler.class, PacketSyncPalette.class, 0, Side.SERVER);

        // Initialize and register async placement handler
        asyncPlacementHandler = new com.xXseesXx.patternwand.executor.AsyncPlacementHandler();
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(asyncPlacementHandler);

        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        // Cancel all pending async jobs
        if (asyncPlacementHandler != null) {
            LOG.info("Server stopping - cancelling pending async jobs");
            asyncPlacementHandler.cancelAllJobs();
        }

        // Shutdown Lua executor service gracefully
        if (proxy.getLuaExecutor() != null) {
            LOG.info("Server stopping - shutting down Lua executor");
            proxy.getLuaExecutor()
                .shutdown();
        }
    }
}
