package com.xXseesXx.patternwand.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import com.xXseesXx.patternwand.PatternWandMod;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

/**
 * Config GUI factory for PatternWand mod configuration.
 */
public class PatternWandGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return PatternWandConfigGui.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    /**
     * Config GUI screen for PatternWand.
     */
    public static class PatternWandConfigGui extends GuiConfig {

        public PatternWandConfigGui(GuiScreen parentScreen) {
            super(
                parentScreen,
                getConfigElements(),
                PatternWandMod.MODID,
                false,
                false,
                GuiConfig.getAbridgedConfigPath(PatternWandMod.configFile.toString()));
        }

        private static List<IConfigElement> getConfigElements() {
            List<IConfigElement> list = new ArrayList<>();

            // Load the configuration file
            Configuration config = new Configuration(PatternWandMod.configFile);
            config.load();

            // Add all config elements from the GENERAL category
            list.addAll(new ConfigElement(config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements());

            return list;
        }
    }
}
