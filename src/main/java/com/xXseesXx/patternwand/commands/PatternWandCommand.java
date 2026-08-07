package com.xXseesXx.patternwand.commands;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import com.xXseesXx.patternwand.PatternWandMod;
import com.xXseesXx.patternwand.items.ItemPatternWand;
import com.xXseesXx.patternwand.patterns.scripted.CompiledScript;
import com.xXseesXx.patternwand.patterns.scripted.PatternScriptLoader;

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
        return "/patternwand <reload|list|set <pattern>|info|seed <value>|clearseed>";
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
                    sender.addChatMessage(new ChatComponentText("§cUsage: /patternwand set <pattern>"));
                    return;
                }
                handleSet(sender, args[1]);
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

            default:
                sender.addChatMessage(
                    new ChatComponentText("§cUnknown subcommand: " + subCommand + "\n" + getCommandUsage(sender)));
                break;
        }
    }

    private void handleReload(ICommandSender sender) {
        try {
            scriptLoader.reload();
            sender.addChatMessage(
                new ChatComponentText("§aReloaded " + scriptLoader.getScriptCount() + " pattern scripts"));
        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentText("§cFailed to reload patterns: " + e.getMessage()));
            PatternWandMod.LOG.error("Failed to reload patterns", e);
        }
    }

    private void handleList(ICommandSender sender) {
        String[] scripts = scriptLoader.getScriptNames();

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

    private void handleSet(ICommandSender sender, String patternName) {
        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText("§cThis command can only be used by players"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack heldItem = player.getCurrentEquippedItem();

        if (heldItem == null || !(heldItem.getItem() instanceof ItemPatternWand)) {
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

        sender.addChatMessage(new ChatComponentText("§aSet active pattern to: §f" + patternName));
    }

    private void handleInfo(ICommandSender sender) {
        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText("§cThis command can only be used by players"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack heldItem = player.getCurrentEquippedItem();

        if (heldItem == null || !(heldItem.getItem() instanceof ItemPatternWand)) {
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
        }

        // Seed info
        if (tag != null && tag.hasKey("patternSeed")) {
            long seed = tag.getLong("patternSeed");
            sender.addChatMessage(new ChatComponentText("§aCustom seed: §f" + seed));
        } else {
            long worldSeed = player.worldObj.getSeed();
            sender.addChatMessage(new ChatComponentText("§7Using world seed: §f" + worldSeed));
        }
    }

    private void handleSeed(ICommandSender sender, String seedStr) {
        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText("§cThis command can only be used by players"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack heldItem = player.getCurrentEquippedItem();

        if (heldItem == null || !(heldItem.getItem() instanceof ItemPatternWand)) {
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

        if (heldItem == null || !(heldItem.getItem() instanceof ItemPatternWand)) {
            sender.addChatMessage(new ChatComponentText("§cYou must be holding a Pattern Wand"));
            return;
        }

        NBTTagCompound tag = heldItem.getTagCompound();
        if (tag != null && tag.hasKey("patternSeed")) {
            tag.removeTag("patternSeed");
            sender.addChatMessage(new ChatComponentText("§aCleared custom seed"));
            sender.addChatMessage(new ChatComponentText("§7Now using world seed: §f" + player.worldObj.getSeed()));
        } else {
            sender.addChatMessage(new ChatComponentText("§eNo custom seed was set"));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            // Tab complete subcommands
            return getListOfStringsMatchingLastWord(args, "reload", "list", "set", "info", "seed", "clearseed");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            // Tab complete pattern names
            String[] scripts = scriptLoader.getScriptNames();
            List<String> patternNames = new ArrayList<>();

            for (String script : scripts) {
                // Remove .lua extension for tab completion
                patternNames.add(script.replace(".lua", ""));
            }

            return getListOfStringsMatchingLastWord(args, patternNames.toArray(new String[0]));
        }

        return null;
    }
}
