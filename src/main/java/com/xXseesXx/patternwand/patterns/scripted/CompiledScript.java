package com.xXseesXx.patternwand.patterns.scripted;

import org.luaj.vm2.LuaValue;

/**
 * Represents a compiled Lua script with metadata.
 */
public class CompiledScript {

    public final String name;
    public final LuaValue function;
    public final PatternMetadata metadata;

    public CompiledScript(String name, LuaValue function, PatternMetadata metadata) {
        this.name = name;
        this.function = function;
        this.metadata = metadata != null ? metadata : new PatternMetadata();
    }

    /**
     * Create compiled script without metadata (backward compatibility).
     */
    public CompiledScript(String name, LuaValue function) {
        this(name, function, new PatternMetadata());
    }
}
