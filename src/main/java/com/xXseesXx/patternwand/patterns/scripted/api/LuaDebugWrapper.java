package com.xXseesXx.patternwand.patterns.scripted.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.VarArgFunction;

/**
 * Lua-friendly wrapper for debug functions.
 * Exposes debug operations as a Lua table.
 */
public class LuaDebugWrapper {

    /**
     * Create a Lua table wrapping debug functions.
     *
     * @param api The underlying debug API
     * @return Lua table with debug functions
     */
    public static LuaTable wrap(final DebugAPI api) {
        LuaTable table = new LuaTable();

        // Print debug message: debug.print(...)
        table.set("print", new VarArgFunction() {

            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                // Convert all arguments to strings and concatenate
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    if (i > 1) sb.append(" ");
                    LuaValue arg = args.arg(i);
                    sb.append(arg.tojstring());
                }
                api.print(sb.toString());
                return LuaValue.NIL;
            }
        });

        return table;
    }
}
