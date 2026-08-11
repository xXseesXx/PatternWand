package com.xXseesXx.patternwand.commands;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import com.xXseesXx.patternwand.Config;
import com.xXseesXx.patternwand.PatternWandMod;
import com.xXseesXx.patternwand.items.ItemPatternWandUnbreakable;
import com.xXseesXx.patternwand.patterns.scripted.CompiledScript;
import com.xXseesXx.patternwand.patterns.scripted.PatternScriptLoader;
import com.xXseesXx.patternwand.patterns.scripted.api.DebugAPI;

/**
 * Command for managing pattern scripts.
 * Usage:
 * /patternwand reload - Reload all pattern scripts
 * /patternwand list - List all loaded patterns
 * /patternwand set <pattern> - Set active pattern on held wand
 * /patternwand info - Show info about currently held wand
 */
public class PatternWandCommand extends CommandBase {

    private final PatternScriptLoader scriptLoader;

    public PatternWandCommand(PatternScriptLoader scriptLoader) {
        this.scriptLoader = scriptLoader;
    }

    @Override
    public String getCommandName() {
        return "patternwand";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/patternwand <reload|list|set <pattern>|info|seed <value>|clearseed|debug <on|off>|benchmark <pattern> <size>>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Anyone can use
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;

            case "list":
                handleList(sender);
                break;

            case "set":
                if (args.length < 2) {
                    sender
                        .addChatMessage(new ChatComponentText("§cUsage: /patternwand set <pattern> [param=value ...]"));
                    return;
                }
                // Pass all remaining args as potential parameters
                String[] paramArgs = new String[args.length - 2];
                System.arraycopy(args, 2, paramArgs, 0, args.length - 2);
                handleSet(sender, args[1], paramArgs);
                break;

            case "info":
                handleInfo(sender);
                break;

            case "seed":
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("§cUsage: /patternwand seed <value>"));
                    return;
                }
                handleSeed(sender, args[1]);
                break;

            case "clearseed":
                handleClearSeed(sender);
                break;

            case "debug":
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("§cUsage: /patternwand debug <on|off>"));
                    return;
                }
                handleDebug(sender, args[1]);
                break;

            case "benchmark":
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentText("§cUsage: /patternwand benchmark <pattern> <size>"));
                    return;
                }
                handleBenchmark(sender, args[1], args[2]);
                break;

            default:
                sender.addChatMessage(
                    new ChatComponentText("§cUnknown subcommand: " + subCommand + "\n" + getCommandUsage(sender)));
                break;
        }
    }

    private void handleReload(ICommandSender sender) {
        try {
            // Reload patterns from filesystem and mod assets
            scriptLoader.reload();
            int count = scriptLoader.getScriptCount();

            sender.addChatMessage(new ChatComponentText("§aReloaded " + count + " pattern scripts"));
        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentText("§cFailed to reload patterns: " + e.getMessage()));
            PatternWandMod.LOG.error("Failed to reload patterns", e);
        }
    }

    private void handleList(ICommandSender sender) {
        // Use server config to determine visibility
        String[] scripts = scriptLoader.getScriptNames(Config.showDefaultPatterns);

        if (scripts.length == 0) {
            sender.addChatMessage(new ChatComponentText("§eNo pattern scripts loaded"));
            return;
        }

        sender.addChatMessage(new ChatComponentText("§aLoaded patterns (" + scripts.length + "):"));

        for (String script : scripts) {
            // Remove .lua extension for display
            String displayName = script.replace(".lua", "");
            sender.addChatMessage(new ChatComponentText("  §7- §f" + displayName));
        }
    }

    private void handleSet(ICommandSender sender, String patternName, String[] paramArgs) {
        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText("§cThis command can only be used by players"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack heldItem = player.getCurrentEquippedItem();

        if (heldItem == null || !(heldItem.getItem() instanceof ItemPatternWandUnbreakable)) {
            sender.addChatMessage(new ChatComponentText("§cYou must be holding a Pattern Wand"));
            return;
        }

        // Add .lua extension if not present
        String scriptName = patternName.endsWith(".lua") ? patternName : patternName + ".lua";

        // Check if script exists
        CompiledScript script = scriptLoader.getScript(scriptName);
        if (script == null) {
            sender.addChatMessage(new ChatComponentText("§cPattern not found: " + patternName));
            sender.addChatMessage(new ChatComponentText("§eUse /patternwand list to see available patterns"));
            return;
        }

        // Store pattern name in wand NBT
        NBTTagCompound tag = heldItem.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            heldItem.setTagCompound(tag);
        }

        tag.setString("activePattern", scriptName);

        // Parse and store parameters
        NBTTagCompound paramsTag = new NBTTagCompound();
        int paramCount = 0;

        for (String paramArg : paramArgs) {
            // Support both = and : as separators
            String[] parts = paramArg.split("[=:]", 2);
            if (parts.length == 2) {
                String paramName = parts[0].trim();
                String paramValue = parts[1].trim();

                // Validate parameter exists in metadata
                com.xXseesXx.patternwand.patterns.scripted.PatternParameter param = script.metadata
                    .getParameter(paramName);
                if (param != null) {
                    // Store parameter value based on type
                    try {
                        Object validatedValue = param.validate(paramValue);
                        if (validatedValue instanceof Integer) {
                            paramsTag.setInteger(paramName, (Integer) validatedValue);
                        } else if (validatedValue instanceof Number) {
                            paramsTag.setDouble(paramName, ((Number) validatedValue).doubleValue());
                        } else if (validatedValue instanceof Boolean) {
                            paramsTag.setBoolean(paramName, (Boolean) validatedValue);
                        } else if (validatedValue instanceof String) {
                            paramsTag.setString(paramName, (String) validatedValue);
                        }
                        paramCount++;
                    } catch (IllegalArgumentException e) {
                        sender.addChatMessage(
                            new ChatComponentText(
                                "§cInvalid value for parameter '" + paramName + "': " + e.getMessage()));
                    }
                } else {
                    sender.addChatMessage(
                        new ChatComponentText("§eWarning: Unknown parameter '" + paramName + "' (ignored)"));
                }
            }
        }

        // Store parameters in NBT
        if (paramCount > 0) {
            tag.setTag("patternParams", paramsTag);
            sender.addChatMessage(
                new ChatComponentText(
                    "§aSet active pattern to: §f" + patternName + " §7with " + paramCount + " parameter(s)"));
        } else {
            // Remove old parameters if none provided
            tag.removeTag("patternParams");
            sender.addChatMessage(new ChatComponentText("§aSet active pattern to: §f" + patternName));
        }

        // Show author and description if available
        String author = script.metadata.getAuthor();
        String description = script.metadata.getDescription();
        if (author != null && !author.equals("Unknown")) {
            sender.addChatMessage(new ChatComponentText("§7Author: §f" + author));
        }
        if (description != null && !description.isEmpty()) {
            sender.addChatMessage(new ChatComponentText("§7Description: §f" + description));
        }

        // Show parameter info if pattern has parameters
        if (script.metadata.hasParameters() && paramCount == 0) {
            sender.addChatMessage(new ChatComponentText("§7Available parameters:"));
            for (com.xXseesXx.patternwand.patterns.scripted.PatternParameter param : script.metadata.getParameters()) {
                sender.addChatMessage(
                    new ChatComponentText("  §e" + param.getName() + " §7(default: " + param.getDefaultValue() + ")"));
            }
        }
    }

    private void handleInfo(ICommandSender sender) {
        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText("§cThis command can only be used by players"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack heldItem = player.getCurrentEquippedItem();

        if (heldItem == null || !(heldItem.getItem() instanceof ItemPatternWandUnbreakable)) {
            sender.addChatMessage(new ChatComponentText("§cYou must be holding a Pattern Wand"));
            return;
        }

        NBTTagCompound tag = heldItem.getTagCompound();

        // Pattern info
        if (tag == null || !tag.hasKey("activePattern")) {
            sender.addChatMessage(new ChatComponentText("§eNo active pattern set"));
            sender.addChatMessage(new ChatComponentText("§7Use /patternwand set <pattern> to set one"));
        } else {
            String activePattern = tag.getString("activePattern");
            String displayName = activePattern.replace(".lua", "");
            sender.addChatMessage(new ChatComponentText("§aActive pattern: §f" + displayName));

            // Show metadata if available
            CompiledScript script = scriptLoader.getScript(activePattern);
            if (script != null && script.metadata != null) {
                String author = script.metadata.getAuthor();
                String description = script.metadata.getDescription();
                if (author != null && !author.equals("Unknown")) {
                    sender.addChatMessage(new ChatComponentText("§7Author: §f" + author));
                }
                if (description != null && !description.isEmpty()) {
                    sender.addChatMessage(new ChatComponentText("§7Description: §f" + description));
                }
            }
        }

        // Seed info
        if (tag != null && tag.hasKey("patternSeed")) {
            long seed = tag.getLong("patternSeed");
            sender.addChatMessage(new ChatComponentText("§aCustom seed: §f" + seed));
        } else {
            sender.addChatMessage(new ChatComponentText("§7Using default seed: §f" + Config.defaultPatternSeed));
        }
    }

    private void handleSeed(ICommandSender sender, String seedStr) {
        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText("§cThis command can only be used by players"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack heldItem = player.getCurrentEquippedItem();

        if (heldItem == null || !(heldItem.getItem() instanceof ItemPatternWandUnbreakable)) {
            sender.addChatMessage(new ChatComponentText("§cYou must be holding a Pattern Wand"));
            return;
        }

        // Parse seed value
        long seed;
        try {
            seed = Long.parseLong(seedStr);
        } catch (NumberFormatException e) {
            sender.addChatMessage(new ChatComponentText("§cInvalid seed value. Must be a number."));
            return;
        }

        // Store seed in wand NBT
        NBTTagCompound tag = heldItem.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            heldItem.setTagCompound(tag);
        }

        tag.setLong("patternSeed", seed);

        sender.addChatMessage(new ChatComponentText("§aSet pattern seed to: §f" + seed));
        sender.addChatMessage(new ChatComponentText("§7This seed will be used for all pattern noise generation"));
    }

    private void handleClearSeed(ICommandSender sender) {
        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText("§cThis command can only be used by players"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack heldItem = player.getCurrentEquippedItem();

        if (heldItem == null || !(heldItem.getItem() instanceof ItemPatternWandUnbreakable)) {
            sender.addChatMessage(new ChatComponentText("§cYou must be holding a Pattern Wand"));
            return;
        }

        NBTTagCompound tag = heldItem.getTagCompound();
        if (tag != null && tag.hasKey("patternSeed")) {
            tag.removeTag("patternSeed");
            sender.addChatMessage(new ChatComponentText("§aCleared custom seed"));
            sender.addChatMessage(new ChatComponentText("§7Now using default seed: §f" + Config.defaultPatternSeed));
        } else {
            sender.addChatMessage(new ChatComponentText("§eNo custom seed was set"));
        }
    }

    private void handleDebug(ICommandSender sender, String mode) {
        String lowerMode = mode.toLowerCase();

        if (lowerMode.equals("on") || lowerMode.equals("true") || lowerMode.equals("enable")) {
            DebugAPI.setDebugEnabled(true);
            sender.addChatMessage(new ChatComponentText("§aDebug mode enabled"));
            sender.addChatMessage(
                new ChatComponentText("§7Pattern scripts can now output debug messages using debug.print()"));
            sender.addChatMessage(new ChatComponentText("§7Pattern execution timing will be tracked and displayed"));
        } else if (lowerMode.equals("off") || lowerMode.equals("false") || lowerMode.equals("disable")) {
            DebugAPI.setDebugEnabled(false);
            sender.addChatMessage(new ChatComponentText("§eDebug mode disabled"));
        } else {
            sender.addChatMessage(new ChatComponentText("§cUsage: /patternwand debug <on|off>"));
            sender.addChatMessage(
                new ChatComponentText("§7Current status: " + (DebugAPI.isDebugEnabled() ? "§aenabled" : "§cdisabled")));
        }
    }

    private void handleBenchmark(ICommandSender sender, String patternName, String sizeStr) {
        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText("§cThis command can only be used by players"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;

        // Parse size
        int size;
        try {
            size = Integer.parseInt(sizeStr);
            if (size <= 0 || size > 10000) {
                sender.addChatMessage(new ChatComponentText("§cSize must be between 1 and 10000"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.addChatMessage(new ChatComponentText("§cInvalid size: must be a number"));
            return;
        }

        // Add .lua extension if not present
        String scriptName = patternName.endsWith(".lua") ? patternName : patternName + ".lua";

        // Check if script exists
        CompiledScript script = scriptLoader.getScript(scriptName);
        if (script == null) {
            sender.addChatMessage(new ChatComponentText("§cPattern not found: " + patternName));
            sender.addChatMessage(new ChatComponentText("§eUse /patternwand list to see available patterns"));
            return;
        }

        sender.addChatMessage(
            new ChatComponentText("§eRunning benchmark: " + patternName + " with " + size + " blocks..."));
        sender.addChatMessage(new ChatComponentText("§7This will temporarily enable debug mode"));

        // Store original debug state
        boolean originalDebugState = DebugAPI.isDebugEnabled();

        try {
            // Enable debug mode for timing
            DebugAPI.setDebugEnabled(true);

            // Create a synthetic pattern execution for benchmarking
            // We'll measure just the Lua execution phase without actual block placement
            long startTimeNs = System.nanoTime();

            // Create test palette (54 slots filled with stone)
            net.minecraft.inventory.IInventory testPalette = new net.minecraft.inventory.InventoryBasic(
                "Benchmark",
                false,
                54);
            for (int i = 0; i < 54; i++) {
                testPalette.setInventorySlotContents(i, new ItemStack(net.minecraft.init.Blocks.stone, 64, 0));
            }

            // Use default seed and empty parameters
            long seed = Config.defaultPatternSeed;
            java.util.Map<String, Object> params = script.metadata.createDefaultValues();

            // Create synthetic placement context (centered at player position)
            int centerX = (int) player.posX;
            int centerY = (int) player.posY;
            int centerZ = (int) player.posZ;

            com.xXseesXx.patternwand.patterns.scripted.PlacementContext context = new com.xXseesXx.patternwand.patterns.scripted.PlacementContext(
                centerX,
                centerY,
                centerZ,
                1, // UP face
                centerX - 10,
                centerY - 10,
                centerZ - 10,
                centerX + 10,
                centerY + 10,
                centerZ + 10,
                player.rotationYaw,
                player.rotationPitch,
                player.worldObj.getTotalWorldTime(),
                player.worldObj.getWorldTime());

            // Start timing
            DebugAPI.startPatternTiming(player);
            DebugAPI.startPhase1();

            // Prepare positions for batch execution
            java.util.List<com.xXseesXx.patternwand.patterns.scripted.ScriptEngine.BlockPosition> positions = new java.util.ArrayList<com.xXseesXx.patternwand.patterns.scripted.ScriptEngine.BlockPosition>();

            // Create a grid pattern for testing
            int gridSize = (int) Math.ceil(Math.sqrt(size));

            for (int i = 0; i < size; i++) {
                int x = centerX + (i % gridSize);
                int y = centerY;
                int z = centerZ + (i / gridSize);

                int relX = x - centerX;
                int relY = 0;
                int relZ = z - centerZ;

                positions.add(
                    new com.xXseesXx.patternwand.patterns.scripted.ScriptEngine.BlockPosition(
                        x,
                        y,
                        z,
                        relX,
                        relY,
                        relZ));
            }

            // Execute pattern in batch (API wrappers created once)
            int[] results;
            try {
                results = PatternWandMod.proxy.getScriptLoader()
                    .getEngine()
                    .executePatternBatch(script, positions, testPalette, seed, params, context);
            } catch (Exception e) {
                sender.addChatMessage(new ChatComponentText("§cPattern execution failed: " + e.getMessage()));
                PatternWandMod.LOG.error("Benchmark pattern execution failed", e);
                return;
            }

            // Count blocks planned
            int blocksEvaluated = results.length;
            int blocksPlanned = 0;
            for (int idx : results) {
                if (idx >= 0) {
                    blocksPlanned++;
                }
            }

            // End timing
            DebugAPI.endPhase1(blocksPlanned);
            long totalTimeNs = System.nanoTime() - startTimeNs;

            // Calculate statistics
            double totalMs = totalTimeNs / 1_000_000.0;
            double avgMsPerBlock = totalMs / blocksEvaluated;
            double blocksPerSecond = blocksEvaluated / (totalMs / 1000.0);

            // Send benchmark results
            sender.addChatMessage(new ChatComponentText("§a=== Benchmark Results ==="));
            sender.addChatMessage(new ChatComponentText("§7Pattern: §f" + patternName));
            sender.addChatMessage(new ChatComponentText("§7Blocks evaluated: §f" + blocksEvaluated));
            sender.addChatMessage(
                new ChatComponentText(
                    "§7Blocks planned: §f" + blocksPlanned
                        + " §7("
                        + String.format("%.1f%%", (blocksPlanned * 100.0 / blocksEvaluated))
                        + ")"));
            sender.addChatMessage(new ChatComponentText("§7Total time: §e" + String.format("%.2f ms", totalMs)));
            sender
                .addChatMessage(new ChatComponentText("§7Avg per block: §e" + String.format("%.4f ms", avgMsPerBlock)));
            sender.addChatMessage(
                new ChatComponentText("§7Throughput: §e" + String.format("%.0f blocks/sec", blocksPerSecond)));

            // Show detailed phase timing
            DebugAPI.finishPatternTiming();

        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentText("§cBenchmark failed: " + e.getMessage()));
            PatternWandMod.LOG.error("Benchmark failed", e);
        } finally {
            // Restore original debug state
            DebugAPI.setDebugEnabled(originalDebugState);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            // Tab complete subcommands
            return getListOfStringsMatchingLastWord(
                args,
                "reload",
                "list",
                "set",
                "info",
                "seed",
                "clearseed",
                "debug",
                "benchmark");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            // Tab complete pattern names
            String[] scripts = scriptLoader.getScriptNames();
            List<String> patternNames = new ArrayList<>();

            for (String script : scripts) {
                // Remove .lua extension for tab completion
                patternNames.add(script.replace(".lua", ""));
            }

            return getListOfStringsMatchingLastWord(args, patternNames.toArray(new String[0]));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("benchmark")) {
            // Tab complete pattern names for benchmark
            String[] scripts = scriptLoader.getScriptNames();
            List<String> patternNames = new ArrayList<>();

            for (String script : scripts) {
                // Remove .lua extension for tab completion
                patternNames.add(script.replace(".lua", ""));
            }

            return getListOfStringsMatchingLastWord(args, patternNames.toArray(new String[0]));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("benchmark")) {
            // Suggest common benchmark sizes
            return getListOfStringsMatchingLastWord(args, "10", "100", "500", "1000", "5000");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            // Tab complete debug options
            return getListOfStringsMatchingLastWord(args, "on", "off");
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("set")) {
            // Tab complete parameter names for the selected pattern
            String patternName = args[1];
            String scriptName = patternName.endsWith(".lua") ? patternName : patternName + ".lua";

            CompiledScript script = scriptLoader.getScript(scriptName);
            if (script != null && script.metadata.hasParameters()) {
                List<String> paramSuggestions = new ArrayList<>();

                // Collect already used parameter names
                java.util.Set<String> usedParams = new java.util.HashSet<String>();
                for (int i = 2; i < args.length - 1; i++) {
                    String[] parts = args[i].split("[=:]", 2);
                    if (parts.length > 0) {
                        usedParams.add(parts[0].trim());
                    }
                }

                // Suggest parameters that haven't been used yet
                for (com.xXseesXx.patternwand.patterns.scripted.PatternParameter param : script.metadata
                    .getParameters()) {
                    if (!usedParams.contains(param.getName())) {
                        // Suggest parameter with = syntax
                        String suggestion = param.getName() + "=";
                        // Add default value hint based on type
                        if (param.getType()
                            == com.xXseesXx.patternwand.patterns.scripted.PatternParameter.Type.BOOLEAN) {
                            suggestion += "true";
                        } else {
                            suggestion += param.getDefaultValue();
                        }
                        paramSuggestions.add(suggestion);
                    }
                }

                // If current argument contains =, suggest values for boolean parameters
                String currentArg = args[args.length - 1];
                if (currentArg.contains("=") || currentArg.contains(":")) {
                    String[] parts = currentArg.split("[=:]", 2);
                    if (parts.length >= 1) {
                        String paramName = parts[0].trim();
                        com.xXseesXx.patternwand.patterns.scripted.PatternParameter param = script.metadata
                            .getParameter(paramName);
                        if (param != null && param.getType()
                            == com.xXseesXx.patternwand.patterns.scripted.PatternParameter.Type.BOOLEAN) {
                            // Suggest boolean values
                            return getListOfStringsMatchingLastWord(args, paramName + "=true", paramName + "=false");
                        }
                    }
                }

                return getListOfStringsMatchingLastWord(
                    args,
                    paramSuggestions.toArray(new String[paramSuggestions.size()]));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            // Tab complete debug options
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }

        return null;
    }
}
