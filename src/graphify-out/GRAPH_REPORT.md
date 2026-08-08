# Graph Report - src  (2026-08-08)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 633 nodes · 1588 edges · 42 communities (35 shown, 7 thin omitted)
- Extraction: 82% EXTRACTED · 18% INFERRED · 0% AMBIGUOUS · INFERRED: 285 edges (avg confidence: 0.8)
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
- Community 14
- Community 15
- Community 16

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
- `UtilAPITest` --references--> `UtilAPI`  [EXTRACTED]
  test/java/com/xXseesXx/patternwand/patterns/scripted/api/UtilAPITest.java → main/java/com/xXseesXx/patternwand/patterns/scripted/api/UtilAPI.java
- `PatternScriptLoaderTest` --references--> `PatternScriptLoader`  [EXTRACTED]
  test/java/com/xXseesXx/patternwand/patterns/scripted/PatternScriptLoaderTest.java → main/java/com/xXseesXx/patternwand/patterns/scripted/PatternScriptLoader.java
- `DebugTest` --references--> `ScriptEngine`  [EXTRACTED]
  test/java/com/xXseesXx/patternwand/DebugTest.java → main/java/com/xXseesXx/patternwand/patterns/scripted/ScriptEngine.java
- `ScriptEngineTest` --references--> `ScriptEngine`  [EXTRACTED]
  test/java/com/xXseesXx/patternwand/patterns/scripted/ScriptEngineTest.java → main/java/com/xXseesXx/patternwand/patterns/scripted/ScriptEngine.java
- `DebugAPITest` --references--> `DebugAPI`  [EXTRACTED]
  test/java/com/xXseesXx/patternwand/patterns/scripted/api/DebugAPITest.java → main/java/com/xXseesXx/patternwand/patterns/scripted/api/DebugAPI.java

## Import Cycles
- None detected.

## Communities (42 total, 7 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.05
Nodes (43): cpw.mods.fml.common.eventhandler.SubscribeEvent, IPatternWandItem, ItemPatternWandUnbreakable, ItemStack, NBTTagCompound, Override, Point3d, Override (+35 more)

### Community 1 - "Community 1"
Cohesion: 0.07
Nodes (17): LuaContextWrapper, LuaTable, LuaDebugWrapper, LuaTable, LuaNoiseWrapper, LuaTable, LuaPaletteWrapper, LuaTable (+9 more)

### Community 2 - "Community 2"
Cohesion: 0.06
Nodes (5): PerlinNoise, SimplexNoise, ValueNoise, NoiseAPI, NoiseAPITest

### Community 3 - "Community 3"
Cohesion: 0.07
Nodes (26): cpw.mods.fml.common.network.simpleimpl.IMessage, cpw.mods.fml.common.network.simpleimpl.IMessageHandler, cpw.mods.fml.common.network.simpleimpl.MessageContext, io.netty.buffer.ByteBuf, ContainerPatternWand, ItemStack, Override, PaletteSlot (+18 more)

### Community 4 - "Community 4"
Cohesion: 0.10
Nodes (18): cpw.mods.fml.common.event.FMLInitializationEvent, cpw.mods.fml.common.event.FMLPostInitializationEvent, cpw.mods.fml.common.event.FMLPreInitializationEvent, cpw.mods.fml.common.event.FMLServerStartingEvent, cpw.mods.fml.common.Mod, cpw.mods.fml.common.network.IGuiHandler, cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper, cpw.mods.fml.relauncher.SideOnly (+10 more)

### Community 7 - "Community 7"
Cohesion: 0.15
Nodes (3): DebugAPI, org.junit.After, DebugAPITest

### Community 8 - "Community 8"
Cohesion: 0.15
Nodes (6): Override, PatternWandAliasCommand, Override, PatternWandCommand, net.minecraft.command.ICommand, net.minecraft.command.ICommandSender

### Community 9 - "Community 9"
Cohesion: 0.23
Nodes (3): LuaTable, PlacementContext, PlacementContextTest

### Community 10 - "Community 10"
Cohesion: 0.10
Nodes (8): Deprecated, PatternMetadata, PatternParameter, Type, BOOLEAN, FLOAT, INTEGER, STRING

## Knowledge Gaps
- **4 isolated node(s):** `INTEGER`, `FLOAT`, `BOOLEAN`, `STRING`
  These have ≤1 connection - possible missing edges or undocumented components.
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CompiledScript` connect `Community 1` to `Community 0`, `Community 10`, `Community 11`, `Community 4`?**
  _High betweenness centrality (0.102) - this node is a cross-community bridge._
- **Why does `PatternScriptLoader` connect `Community 11` to `Community 8`, `Community 1`, `Community 4`?**
  _High betweenness centrality (0.078) - this node is a cross-community bridge._
- **Why does `ContainerPatternWand` connect `Community 3` to `Community 0`, `Community 1`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **What connects `INTEGER`, `FLOAT`, `BOOLEAN` to the rest of the system?**
  _4 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05433848797250859 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.06821787414066631 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.06393442622950819 - nodes in this community are weakly interconnected._