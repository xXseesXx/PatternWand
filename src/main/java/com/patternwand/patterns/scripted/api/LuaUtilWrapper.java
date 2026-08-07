package com.patternwand.patterns.scripted.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

/**
 * Lua-friendly wrapper for utility functions.
 * Exposes utility operations as a Lua table with function fields.
 */
public class LuaUtilWrapper {

    /**
     * Create a Lua table wrapping utility functions.
     *
     * @param api The underlying util API
     * @return Lua table with utility functions
     */
    public static LuaTable wrap(final UtilAPI api) {
        LuaTable table = new LuaTable();

        // 2D hash: util.hash(x, z)
        table.set("hash", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue x, LuaValue z) {
                return LuaValue.valueOf(api.hash(x.checkdouble(), z.checkdouble()));
            }
        });

        // 3D hash: util.hash3d(x, y, z)
        table.set("hash3d", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                return LuaValue.valueOf(api.hash3d(x.checkdouble(), y.checkdouble(), z.checkdouble()));
            }
        });

        // Euclidean distance: util.distance(x1, y1, x2, y2)
        table.set("distance", new VarArgFunction() {

            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                return LuaValue.valueOf(
                    api.distance(
                        args.arg(1)
                            .checkdouble(),
                        args.arg(2)
                            .checkdouble(),
                        args.arg(3)
                            .checkdouble(),
                        args.arg(4)
                            .checkdouble()));
            }
        });

        // Manhattan distance: util.manhattan(x1, y1, x2, y2)
        table.set("manhattan", new VarArgFunction() {

            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                return LuaValue.valueOf(
                    api.manhattan(
                        args.arg(1)
                            .checkdouble(),
                        args.arg(2)
                            .checkdouble(),
                        args.arg(3)
                            .checkdouble(),
                        args.arg(4)
                            .checkdouble()));
            }
        });

        // Map value to range: util.map(value, inMin, inMax, outMin, outMax)
        table.set("map", new VarArgFunction() {

            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                return LuaValue.valueOf(
                    api.map(
                        args.arg(1)
                            .checkdouble(),
                        args.arg(2)
                            .checkdouble(),
                        args.arg(3)
                            .checkdouble(),
                        args.arg(4)
                            .checkdouble(),
                        args.arg(5)
                            .checkdouble()));
            }
        });

        // Clamp value: util.clamp(value, min, max)
        table.set("clamp", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue value, LuaValue min, LuaValue max) {
                return LuaValue.valueOf(api.clamp(value.checkdouble(), min.checkdouble(), max.checkdouble()));
            }
        });

        // Linear interpolation: util.lerp(a, b, t)
        table.set("lerp", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue a, LuaValue b, LuaValue t) {
                return LuaValue.valueOf(api.lerp(a.checkdouble(), b.checkdouble(), t.checkdouble()));
            }
        });

        // Floor: util.floor(value)
        table.set("floor", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue value) {
                return LuaValue.valueOf(api.floor(value.checkdouble()));
            }
        });

        // Ceil: util.ceil(value)
        table.set("ceil", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue value) {
                return LuaValue.valueOf(api.ceil(value.checkdouble()));
            }
        });

        // Absolute value: util.abs(value)
        table.set("abs", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue value) {
                return LuaValue.valueOf(api.abs(value.checkdouble()));
            }
        });

        return table;
    }
}
