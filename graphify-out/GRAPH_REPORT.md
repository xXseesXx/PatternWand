# Graph Report - .  (2026-08-08)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 703 nodes · 1685 edges · 54 communities (45 shown, 9 thin omitted)
- Extraction: 83% EXTRACTED · 17% INFERRED · 0% AMBIGUOUS · INFERRED: 282 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `29200c87`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8
- Community 9
- Community 10
- Community 11
- Community 12
- Community 13
- Community 14
- Community 15
- Community 16
- Community 17
- Community 18
- Community 19
- Community 20
- Community 21
- Community 47
- Community 48
- Community 49

## God Nodes (most connected - your core abstractions)
1. `PaletteAPI` - 43 edges
2. `PaletteAPITest` - 38 edges
3. `UtilAPITest` - 34 edges
4. `NoiseAPITest` - 31 edges
5. `UtilAPI` - 26 edges
6. `PatternScriptLoader` - 25 edges
7. `DebugAPITest` - 24 edges
8. `PatternPalette` - 23 edges
9. `PlacementContext` - 21 edges
10. `PatternWandCommand` - 20 edges

## Surprising Connections (you probably didn't know these)
- `CommonProxy` --references--> `PatternScriptLoader`  [EXTRACTED]
  src/main/java/com/xXseesXx/patternwand/CommonProxy.java → src/main/java/com/xXseesXx/patternwand/patterns/scripted/PatternScriptLoader.java
- `ModItems` --references--> `ItemPatternWandUnbreakable`  [EXTRACTED]
  src/main/java/com/xXseesXx/patternwand/ModItems.java → src/main/java/com/xXseesXx/patternwand/items/ItemPatternWandUnbreakable.java
- `PatternWandCommand` --references--> `PatternScriptLoader`  [EXTRACTED]
  src/main/java/com/xXseesXx/patternwand/commands/PatternWandCommand.java → src/main/java/com/xXseesXx/patternwand/patterns/scripted/PatternScriptLoader.java
- `ContainerPatternWand` --references--> `IPatternWandItem`  [EXTRACTED]
  src/main/java/com/xXseesXx/patternwand/gui/ContainerPatternWand.java → src/main/java/com/xXseesXx/patternwand/items/IPatternWandItem.java
- `GuiPatternWand` --references--> `IPatternWandItem`  [EXTRACTED]
  src/main/java/com/xXseesXx/patternwand/gui/GuiPatternWand.java → src/main/java/com/xXseesXx/patternwand/items/IPatternWandItem.java

## Import Cycles
- None detected.

## Communities (54 total, 9 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.05
Nodes (5): org.junit.Test, PaletteAPI, UtilAPI, PaletteAPITest, UtilAPITest

### Community 1 - "Community 1"
Cohesion: 0.06
Nodes (18): net.minecraft.inventory.IInventory, org.junit.Before, org.luaj.vm2.Globals, org.luaj.vm2.LuaTable, org.luaj.vm2.LuaValue, LuaContextWrapper, LuaTable, LuaDebugWrapper (+10 more)

### Community 2 - "Community 2"
Cohesion: 0.06
Nodes (6): PerlinNoise, PerlinNoise, SimplexNoise, ValueNoise, NoiseAPI, NoiseAPITest

### Community 3 - "Community 3"
Cohesion: 0.07
Nodes (14): net.minecraft.command.ICommand, net.minecraft.command.ICommandSender, Override, PatternWandAliasCommand, Override, PatternWandCommand, Deprecated, PatternMetadata (+6 more)

### Community 4 - "Community 4"
Cohesion: 0.10
Nodes (18): cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPostInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.event.FMLServerStartingEvent, cpw.mods.fml.common.Mod, cpw.mods.fml.common.network.IGuiHandler, cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper, cpw.mods.fml.relauncher.SideOnly (+10 more)

### Community 5 - "Community 5"
Cohesion: 0.10
Nodes (15): NBTTagList, net.minecraft.client.gui.inventory.GuiContainer, net.minecraft.entity.player.EntityPlayer, net.minecraft.entity.player.InventoryPlayer, net.minecraft.inventory.Container, net.minecraft.inventory.Slot, net.minecraft.util.ResourceLocation, Slot (+7 more)

### Community 7 - "Community 7"
Cohesion: 0.23
Nodes (3): LuaTable, PlacementContext, PlacementContextTest

### Community 8 - "Community 8"
Cohesion: 0.17
Nodes (3): org.junit.After, PatternScriptLoader, PatternScriptLoaderTest

### Community 9 - "Community 9"
Cohesion: 0.16
Nodes (11): net.minecraft.creativetab.CreativeTabs, net.minecraft.item.Item, net.minecraft.world.World, portablejim.bbw.basics.EnumLock, portablejim.bbw.core.items.ItemBasicWand, ItemPatternWandUnbreakable, ItemStack, Override (+3 more)

### Community 10 - "Community 10"
Cohesion: 0.16
Nodes (7): net.minecraft.block.Block, BlockKey, BlockMatcher, Deprecated, Override, Override, PaletteEntry

### Community 11 - "Community 11"
Cohesion: 0.20
Nodes (11): cpw.mods.fml.common.network.simpleimpl.IMessage, cpw.mods.fml.common.network.simpleimpl.IMessageHandler, cpw.mods.fml.common.network.simpleimpl.MessageContext, io.netty.buffer.ByteBuf, net.minecraft.nbt.NBTTagCompound, GenericHandler, Override, Handler (+3 more)

### Community 12 - "Community 12"
Cohesion: 0.28
Nodes (9): net.minecraft.item.ItemStack, portablejim.bbw.basics.Point3d, portablejim.bbw.shims.IPlayerShim, portablejim.bbw.shims.IWorldShim, Deprecated, EntityPlayer, ItemStack, Override (+1 more)

### Community 14 - "Community 14"
Cohesion: 0.18
Nodes (8): cpw.mods.fml.common.eventhandler.SubscribeEvent, net.minecraft.util.AxisAlignedBB, net.minecraftforge.client.event.DrawBlockHighlightEvent, net.minecraftforge.common.util.ForgeDirection, portablejim.bbw.basics.EnumFluidLock, portablejim.bbw.core.WandWorker, Override, ScriptExecutionException

### Community 15 - "Community 15"
Cohesion: 0.18
Nodes (6): net.minecraft.nbt.NBTTagList, IPatternWandItem, NBTTagCompound, IInventory, Override, PatternPalette

### Community 19 - "Community 19"
Cohesion: 0.36
Nodes (4): net.minecraft.entity.EntityLivingBase, portablejim.bbw.core.wands.IWand, Override, PatternWandUnbreakable

### Community 21 - "Community 21"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **7 isolated node(s):** `INTEGER`, `FLOAT`, `BOOLEAN`, `STRING`, `test-client.sh script` (+2 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CompiledScript` connect `Community 1` to `Community 3`, `Community 4`, `Community 8`, `Community 12`, `Community 14`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Why does `PatternScriptLoader` connect `Community 8` to `Community 1`, `Community 3`, `Community 4`?**
  _High betweenness centrality (0.063) - this node is a cross-community bridge._
- **Why does `ContainerPatternWand` connect `Community 5` to `Community 1`, `Community 12`, `Community 15`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **What connects `INTEGER`, `FLOAT`, `BOOLEAN` to the rest of the system?**
  _7 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05277262420119563 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.061018437225636525 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.060285563194077206 - nodes in this community are weakly interconnected._