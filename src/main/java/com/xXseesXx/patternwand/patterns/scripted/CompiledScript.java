package com.xXseesXx.patternwand.patterns.scripted;

import org.luaj.vm2.LuaValue;

/**
 * Represents a compiled Lua script.
 */
public class CompiledScript {

    public final String name;
    public final LuaValue function;

    public CompiledScript(String name, LuaValue function) {
        this.name = name;
        this.function = function;
    }
}
