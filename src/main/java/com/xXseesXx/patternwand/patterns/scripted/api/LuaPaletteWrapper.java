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

        // Pick uniform random slot: palette.pickUniform()
        // Returns 0-based index (0-26)
        table.set("pickUniform", new ZeroArgFunction() {

            @Override
            public LuaValue call() {
                return LuaValue.valueOf(api.pickUniform());
            }
        });

        // Pick weighted random excluding indices: palette.pickWeightedExcept(indices)
        // indices can be a single number or a table of numbers
        table.set("pickWeightedExcept", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue indices) {
                int[] excludeArray;

                if (indices.isnumber()) {
                    // Single index
                    excludeArray = new int[] { indices.toint() };
                } else if (indices.istable()) {
                    // Table of indices
                    LuaTable table = indices.checktable();
                    java.util.List<Integer> list = new java.util.ArrayList<Integer>();
                    int i = 1;
                    while (true) {
                        LuaValue val = table.get(i);
                        if (val.isnil()) break;
                        if (val.isnumber()) {
                            list.add(val.toint());
                        }
                        i++;
                    }
                    excludeArray = new int[list.size()];
                    for (int j = 0; j < list.size(); j++) {
                        excludeArray[j] = list.get(j);
                    }
                } else {
                    excludeArray = new int[0];
                }

                return LuaValue.valueOf(api.pickWeightedExcept(excludeArray));
            }
        });

        // Pick weighted random from range: palette.pickWeightedRange(min, max)
        table.set("pickWeightedRange", new org.luaj.vm2.lib.TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue minVal, LuaValue maxVal) {
                int min = minVal.toint();
                int max = maxVal.toint();
                return LuaValue.valueOf(api.pickWeightedRange(min, max));
            }
        });

        return table;
    }
}
