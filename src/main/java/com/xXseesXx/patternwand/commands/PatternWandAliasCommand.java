package com.xXseesXx.patternwand.commands;

import java.util.List;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

/**
 * Alias command that delegates to PatternWandCommand.
 * Provides a shorter "/pw" command as an alternative to "/patternwand".
 */
public class PatternWandAliasCommand implements ICommand {

    private final PatternWandCommand delegate;
    private final String aliasName;

    public PatternWandAliasCommand(PatternWandCommand delegate, String aliasName) {
        this.delegate = delegate;
        this.aliasName = aliasName;
    }

    @Override
    public String getCommandName() {
        return aliasName;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return delegate.getCommandUsage(sender)
            .replace("patternwand", aliasName);
    }

    @Override
    public List<String> getCommandAliases() {
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        delegate.processCommand(sender, args);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return delegate.canCommandSenderUseCommand(sender);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return delegate.addTabCompletionOptions(sender, args);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof ICommand) {
            return this.getCommandName()
                .compareTo(((ICommand) o).getCommandName());
        }
        return 0;
    }
}
