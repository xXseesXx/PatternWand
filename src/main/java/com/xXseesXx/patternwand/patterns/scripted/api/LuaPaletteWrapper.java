package com.xXseesXx.patternwand.patterns.scripted.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

/**
 * Lua-friendly wrapper for palette functions.
 * Exposes palette operations as a Lua table with function fields.
 * Maintains 0-based indexing for Java compatibility but provides clear documentation.
 */
public class LuaPaletteWrapper {

    /**
     * Create a Lua table wrapping palette functions.
     *
     * @param api The underlying palette API
     * @return Lua table with palette functions
     */
    public static LuaTable wrap(final PaletteAPI api) {
        LuaTable table = new LuaTable();

        // Get palette size: palette.size()
        table.set("size", new ZeroArgFunction() {

            @Override
            public LuaValue call() {
                return LuaValue.valueOf(api.size());
            }
        });

        // Get slot weight: palette.getWeight(index)
        // Note: Uses 0-based indexing (0-26)
        table.set("getWeight", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue index) {
                return LuaValue.valueOf(api.getWeight(index.checkdouble()));
            }
        });

        // Check if slot is empty: palette.isEmpty(index)
        // Note: Uses 0-based indexing (0-26)
        table.set("isEmpty", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue index) {
                return LuaValue.valueOf(api.isEmpty(index.checkdouble()));
            }
        });

        // Pick weighted random slot: palette.pickWeighted()
        // Returns 0-based index (0-26)
        table.set("pickWeighted", new ZeroArgFunction() {

            @Override
            public LuaValue call() {
                return LuaValue.valueOf(api.pickWeighted());
            }
        });

        // Count non-empty slots: palette.countNonEmpty()
        table.set("countNonEmpty", new ZeroArgFunction() {

            @Override
            public LuaValue call() {
                return LuaValue.valueOf(api.countNonEmpty());
            }
        });

        return table;
    }
}
