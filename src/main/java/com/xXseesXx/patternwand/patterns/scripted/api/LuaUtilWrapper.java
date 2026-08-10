package com.xXseesXx.patternwand.patterns.scripted.api;

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

        // 3D distance: util.distance3d(x1, y1, z1, x2, y2, z2)
        table.set("distance3d", new VarArgFunction() {

            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                return LuaValue.valueOf(
                    api.distance3d(
                        args.arg(1)
                            .checkdouble(),
                        args.arg(2)
                            .checkdouble(),
                        args.arg(3)
                            .checkdouble(),
                        args.arg(4)
                            .checkdouble(),
                        args.arg(5)
                            .checkdouble(),
                        args.arg(6)
                            .checkdouble()));
            }
        });

        // Check if in sphere: util.inSphere(x, y, z, centerX, centerY, centerZ, radius)
        table.set("inSphere", new VarArgFunction() {

            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                return LuaValue.valueOf(
                    api.inSphere(
                        args.arg(1)
                            .checkdouble(),
                        args.arg(2)
                            .checkdouble(),
                        args.arg(3)
                            .checkdouble(),
                        args.arg(4)
                            .checkdouble(),
                        args.arg(5)
                            .checkdouble(),
                        args.arg(6)
                            .checkdouble(),
                        args.arg(7)
                            .checkdouble()));
            }
        });

        // Check if in box: util.inBox(x, y, z, minX, minY, minZ, maxX, maxY, maxZ)
        table.set("inBox", new VarArgFunction() {

            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                return LuaValue.valueOf(
                    api.inBox(
                        args.arg(1)
                            .checkdouble(),
                        args.arg(2)
                            .checkdouble(),
                        args.arg(3)
                            .checkdouble(),
                        args.arg(4)
                            .checkdouble(),
                        args.arg(5)
                            .checkdouble(),
                        args.arg(6)
                            .checkdouble(),
                        args.arg(7)
                            .checkdouble(),
                        args.arg(8)
                            .checkdouble(),
                        args.arg(9)
                            .checkdouble()));
            }
        });

        // Rotate 2D point: util.rotate2D(x, y, angle)
        // Returns a table with {x, y}
        table.set("rotate2D", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue x, LuaValue y, LuaValue angle) {
                double[] result = api.rotate2D(x.checkdouble(), y.checkdouble(), angle.checkdouble());
                LuaTable resultTable = new LuaTable();
                // Set numeric indices (for array-style access)
                resultTable.set(1, LuaValue.valueOf(result[0]));
                resultTable.set(2, LuaValue.valueOf(result[1]));
                // Also set named keys (for field-style access)
                resultTable.set("x", LuaValue.valueOf(result[0]));
                resultTable.set("y", LuaValue.valueOf(result[1]));
                return resultTable;
            }
        });

        // Modulo: util.mod(a, b)
        table.set("mod", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue a, LuaValue b) {
                return LuaValue.valueOf(api.mod(a.checkdouble(), b.checkdouble()));
            }
        });

        // Sign: util.sign(value)
        table.set("sign", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue value) {
                return LuaValue.valueOf(api.sign(value.checkdouble()));
            }
        });

        // Smoothstep: util.smoothstep(edge0, edge1, x)
        table.set("smoothstep", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue edge0, LuaValue edge1, LuaValue x) {
                return LuaValue.valueOf(api.smoothstep(edge0.checkdouble(), edge1.checkdouble(), x.checkdouble()));
            }
        });

        // Rotate coordinates based on block face: util.rotateFace(relX, relY, relZ, face)
        // Returns a table with {u, v, w} - transformed coordinates
        table.set("rotateFace", new VarArgFunction() {

            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                double[] result = api.rotateFace(
                    args.arg(1)
                        .checkdouble(),
                    args.arg(2)
                        .checkdouble(),
                    args.arg(3)
                        .checkdouble(),
                    args.arg(4)
                        .checkint());
                LuaTable resultTable = new LuaTable();
                // Set numeric indices (for array-style access)
                resultTable.set(1, LuaValue.valueOf(result[0])); // u
                resultTable.set(2, LuaValue.valueOf(result[1])); // v
                resultTable.set(3, LuaValue.valueOf(result[2])); // w
                // Also set named keys (for field-style access)
                resultTable.set("u", LuaValue.valueOf(result[0]));
                resultTable.set("v", LuaValue.valueOf(result[1]));
                resultTable.set("w", LuaValue.valueOf(result[2]));
                return resultTable;
            }
        });

        return table;
    }
}
